package org.clulab.reading

import ai.lum.common.ConfigUtils._
import ai.lum.common.ConfigFactory
import ai.lum.odinson._
import org.clulab.processors.fastnlp.FastNLPProcessor


case class Match(
  docId: Int,
  foundBy: String,
  namedCaptures: Array[NamedCapture],
  pseudoIdentity: Seq[NormalizedArg],
  evidence: Evidence,
)

case class Evidence(docID: Int, sentence: String)
case class NormalizedArg(argName: String, normalizedTokens: Seq[String], originalTokens: Seq[String])


object CorpusReader {
  def fromConfig: CorpusReader = {
    val config = ConfigFactory.load()
    val extractorEngine = ExtractorEngine.fromConfig
    val numEvidenceDisplay = config.get[Int]("ui.numEvidenceDisplay").getOrElse(3)
    val consolidateByLemma = config.get[Boolean]("ui.lemmaConsolidation").getOrElse(true)
    new CorpusReader(extractorEngine, numEvidenceDisplay, consolidateByLemma)
  }
}

class CorpusReader(
  val extractorEngine: ExtractorEngine,
  val numEvidenceDisplay: Int,
  consolidateByLemma: Boolean,
) {

  lazy val proc = new FastNLPProcessor

  // todo: remove the query box from the UI
  /**
   * Get extractions for each of the rules.  Since each rule can have different arguments, we keep the
   * extractions separated by rule, and we'll display them separately.
   * @param rules
   * @return Map[ruleName, consolidated extractions for that rule]
   */
  def getExtractions(rules: String): Map[String, Seq[ConsolidatedMatch]] = {
    val extractors = mkExtractorsFromRules(rules)
    val consolidatedMatches = getMatches(extractors)
    consolidatedMatches.mapValues(rankMatches)
  }

  /**
   * Apply extractors to corpus, consolidate results by rule and return the consolidated Matches, persisting
   * all evidence for downstream users.
   * @param extractors
   * @return Map with consolidated matches (i.e., "deduplicated") for each rule (key)
   */
  def getMatches(extractors: Seq[Extractor]): Map[String, Seq[ConsolidatedMatch]] = {
    val mentions = extractorEngine.extractMentions(extractors)
    // Convert the mentions into our Match objects
    val matches = getMatches(mentions)
    // Each rule may have different args and different numbers of args, so we'll need to display
    // them separately.
    val groupByRule = matches.groupBy(_.foundBy).toSeq
    val consolidated = for {
      (foundBy, matchGroup) <- groupByRule
      // count matches so that we can add them to the consolidator efficiently
      // Group by that unique identity mentioned above
      regroupedMatches = matchGroup
        .groupBy(_.pseudoIdentity)
        // get the count of how many times this result appeared and all the sentences where it happened
        // here the length of the values is the count, and we persist all of the evidence even while consolidating
        .mapValues(vs => (vs.length, vs.map(v => v.evidence)))
      // consolidate matches
      consolidator = new Consolidator(proc)
      (pseudoIdentity, (count, sentences)) <- regroupedMatches.toSeq
      _ = consolidator.add(pseudoIdentity, count, sentences)
      // return results
    } yield (foundBy, consolidator.getMatches)
    consolidated.toMap
  }

  def getMatches(mentions: Seq[Mention]): Seq[Match] = {
    for {
      mention <- mentions
      // Get the OdinsonMatch
      m = mention.odinsonMatch
      // Get the source for the extraction, store in wrapper class Evidence
      docId = mention.luceneDocId
      sentence = extractorEngine.getTokens(docId, extractorEngine.displayField).mkString(" ")
      evidence = Evidence(docId, sentence)
      // Get the name of the rule that found the extraction
      foundBy = mention.foundBy
      // Get the results of the rule
      namedCaptures = m.namedCaptures
      // Do any normalizations and create a unique "name" that captures the names of and content
      // of all the arguments
      pseudoIdentity = mkPseudoIdentity(docId, namedCaptures)
    } yield Match(docId, foundBy, namedCaptures, pseudoIdentity, evidence)
  }

  def rankMatches(matches: Seq[ConsolidatedMatch]): Seq[ConsolidatedMatch] = {
    matches.sortBy(-_.count)
  }

  def mkExtractorsFromRules(rules: String): Seq[Extractor] = {
    extractorEngine.ruleReader.compileRuleFile(rules.mkString)
  }

  /**
   * Create a representation of each the named captures that has: (a) the label of the named capture,
   *  (b) the normlized tokens of the result (if enabled), and (c) the original result
   * @param docId the Lucene DocID
   * @param namedCaptures the named captures for the match
   * @return sequence of NormalizedArg -- the wrapper class for the views described above
   */
  def mkPseudoIdentity(docId: Int, namedCaptures: Array[NamedCapture]): Seq[NormalizedArg] = {
    for {
      nc <- namedCaptures
      argName = nc.name
      capturedMatch = nc.capturedMatch
      tokens = extractorEngine.getTokens(docId, capturedMatch).toSeq
      normalizedTokens = if (consolidateByLemma) convertToLemmas(tokens) else tokens
    } yield NormalizedArg(argName, normalizedTokens, tokens)
  }

  def convertToLemmas(words: Seq[String]): Seq[String] = {
    val s = words.mkString(" ")
    val doc = proc.mkDocument(s)
    proc.tagPartsOfSpeech(doc)
    proc.lemmatize(doc)
    doc.clear()
    val sentence = doc.sentences.head
    // return the lemmas
    sentence.lemmas.get
  }

}
