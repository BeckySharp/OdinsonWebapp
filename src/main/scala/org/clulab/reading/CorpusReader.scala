package org.clulab.reading

import java.io.PrintWriter

import ai.lum.common.ConfigUtils._
import ai.lum.common.ConfigFactory
import ai.lum.odinson.DataGatherer.VerboseLevels
import ai.lum.odinson._
import org.clulab.processors.Processor
import upickle.default._
import upickle.default.{macroRW, ReadWriter => RW}

case class Match(
  docId: String,
  foundBy: String,
  pseudoIdentity: Seq[NormalizedArg],
  evidence: Evidence
)
object Match {implicit val rw: RW[Match] = macroRW}

case class Evidence(docID: String, sentence: String)
object Evidence {implicit val rw: RW[Evidence] = macroRW}

case class NormalizedArg(argName: String, normalizedTokens: Seq[String], originalTokens: Seq[String])
object NormalizedArg {implicit val rw: RW[NormalizedArg] = macroRW}

object CorpusReader {
  def fromConfig: CorpusReader = {
    val config = ConfigFactory.load()
    val extractorEngine = ExtractorEngine.fromConfig
    val numEvidenceDisplay = config.get[Int]("ui.numEvidenceDisplay").getOrElse(3)
    val displayField = config.get[String]("ui.displayField").getOrElse("raw")
    val wordField = config.get[String]("ui.wordField").getOrElse("word")
    val lemmaField = config.get[String]("ui.lemmaField").getOrElse("lemma")
    val consolidateByLemma = config.get[Boolean]("ui.lemmaConsolidation").getOrElse(true)
    new CorpusReader(extractorEngine, numEvidenceDisplay, displayField, wordField, lemmaField, consolidateByLemma)
  }

  /**
   * Write the matches to a file as json lines format, where each line is a valid json object
   * @param ms matches to write
   * @param filename where to write the file
   */
  def writeMatchesTo(ms: Seq[Match], filename: String): Unit = {
    println(s"writing ${ms.length} matches to $filename")
    val pw = new PrintWriter(filename: String)
    ms.foreach { m =>
      writeTo(m, pw)
      pw.println()
    }
    pw.flush()
    pw.close()
  }

  def matchesAsJsonStrings(ms: Seq[Match]): Seq[String] = {
    ms.map(m => write(m))
  }

}

class CorpusReader(
  val extractorEngine: ExtractorEngine,
  val numEvidenceDisplay: Int,
  displayField: String,
  wordField: String,
  lemmaField: String,
  consolidateByLemma: Boolean,
) {

  var proc: Option[Processor] = None

// todo: remove the query box from the UI
  /**
   * Get extractions for each of the rules.  Since each rule can have different arguments, we keep the
   * extractions separated by rule, and we'll display them separately.
   * @param rules
   * @return Map[ruleName, consolidated extractions for that rule]
   */
  def extractMatchesFromRules(rules: String, topLevelOnly: Boolean): Seq[Match] = {
    getMatches(extractMentionsFromRules(rules, topLevelOnly))
  }

  def extractMatchesFromRules(rules: Seq[Rule], topLevelOnly: Boolean): Seq[Match] = {
    getMatches(extractMentionsFromRules(rules, topLevelOnly))
  }

  def extractMentionsFromRules(rules: String, topLevelOnly: Boolean): Seq[Mention] = {
    val extractors = extractorEngine.compileRuleString(rules)
    extractMentions(extractors, topLevelOnly)
  }

  def extractMentionsFromRules(rules: Seq[Rule], topLevelOnly: Boolean): Seq[Mention] = {
    val extractors = extractorEngine.ruleReader.mkExtractors(rules)
    extractMentions(extractors, topLevelOnly)
  }


  /**
   * Apply extractors to corpus to get the mentions
   * @param extractors
   * @param topLevelOnly whether or not to limit results to top level mentions (i.e., no args)
   * @return sequence of Mention
   */
  def extractMentions(extractors: Seq[Extractor], topLevelOnly: Boolean = true): Seq[Mention] = {
    val mentions = extractorEngine.extractAndPopulate(extractors, level = VerboseLevels.All).toArray
    extractorEngine.clearState()
    if (topLevelOnly) {
      val topLevelLabels = extractors.map(_.label)
      mentions.filter(m => topLevelLabels.contains(m.label))
    } else {
      mentions
    }
  }

  private def getMatches(mentions: Seq[Mention]): Seq[Match] = {
    for {
      mention <- mentions
      _ = println(mention.arguments.keySet.mkString(", "))
      // Get the source for the extraction, store in wrapper class Evidence
      docId = s"${mention.docId}:${mention.sentenceId}"
      sentence = sentenceText(mention)
      evidence = Evidence(docId, sentence)
      // Get the name of the rule that found the extraction
      foundBy = mention.foundBy
      // Get the results of the rule
      args = mention.arguments
      // Do any normalizations and create a unique "name" that captures the names of and content
      // of all the arguments
      pseudoIdentity = mkPseudoIdentity(docId, args)
    } yield {
      val mm = Match(docId, foundBy, pseudoIdentity, evidence)
      println(mm)
      mm
    }
  }


  /**
   * Create a representation of each the named captures that has: (a) the label of the named capture,
   *  (b) the normlized tokens of the result (if enabled), and (c) the original result
   * @param docId the Lucene DocID
   * @param args the mention arguments
   * @return sequence of NormalizedArg -- the wrapper class for the views described above
   */
  private def mkPseudoIdentity(docId: String, args: Map[String, Array[Mention]]): Seq[NormalizedArg] = {
    for {
      (argName, argMentions) <- args.toSeq
      mention <- argMentions
      tokens = getTokens(mention)
      normalizedTokens = if (consolidateByLemma) getLemmas(mention) else tokens
    } yield NormalizedArg(argName, normalizedTokens, tokens)
  }

  def sentenceText(m: Mention): String = m.documentFields(displayField).mkString(" ")

  private def getTokens(mention: Mention): Array[String] = mention.mentionFields(displayField)

  private def getLemmas(mention: Mention): Array[String] = {
    if (mention.mentionFields.contains(lemmaField)) mention.mentionFields(lemmaField)
    else convertToLemmas(getTokens(mention))
  }

  private def convertToLemmas(words: Array[String]): Array[String] = {
    val s = words.mkString(" ")
    assert(proc.isDefined, "The CorpusReader processor wasn't initialized")
    val processor = proc.get
    val doc = processor.mkDocument(s)
    processor.tagPartsOfSpeech(doc)
    processor.lemmatize(doc)
    doc.clear()
    val sentence = doc.sentences.head
    // return the lemmas
    sentence.lemmas.get
  }

}
