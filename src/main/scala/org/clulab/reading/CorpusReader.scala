package org.clulab.reading

import java.io.PrintWriter

import ai.lum.common.ConfigUtils._
import ai.lum.common.ConfigFactory
import ai.lum.odinson._
import org.clulab.processors.Processor
import org.clulab.processors.fastnlp.FastNLPProcessor
import ujson.Value
import upickle.default._
import upickle.default.{macroRW, ReadWriter => RW}

case class Match(
  docId: String,
  foundBy: String,
//  namedCaptures: Array[NamedCapture],
  pseudoIdentity: Seq[NormalizedArg],
  evidence: Evidence,
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
    val consolidateByLemma = config.get[Boolean]("ui.lemmaConsolidation").getOrElse(true)
    new CorpusReader(extractorEngine, numEvidenceDisplay, consolidateByLemma)
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
  def extractMatchesFromRules(rules: String): Seq[Match] = {
    val extractors = extractorEngine.compileRuleString(rules)
    extractMatches(extractors)
  }

  def extractMatchesFromRules(rules: Seq[Rule]): Seq[Match] = {
    val extractors = extractorEngine.ruleReader.mkExtractors(rules)
    extractMatches(extractors)
  }


  /**
   * Apply extractors to corpus to get the matches
   * @param extractors
   * @return sequence of Match
   */
  def extractMatches(extractors: Seq[Extractor]): Seq[Match] = {
    val mentions = extractorEngine.extractMentions(extractors).toArray
    // Convert the mentions into our Match objects
    getMatches(mentions)
  }

  private def getMatches(mentions: Seq[Mention]): Seq[Match] = {
    for {
      mention <- mentions
      // Get the OdinsonMatch
      m = mention.odinsonMatch
      // Get the source for the extraction, store in wrapper class Evidence
      luceneDocID = mention.luceneDocId
      docId = s"${mention.docId}:${mention.sentenceId}"
      sentence = extractorEngine.getTokens(luceneDocID, extractorEngine.displayField).mkString(" ")
      evidence = Evidence(docId, sentence)
      // Get the name of the rule that found the extraction
      foundBy = mention.foundBy
      // Get the results of the rule
      namedCaptures = m.namedCaptures ++ triggerNamedCaptureOpt(m).toSeq
      // Do any normalizations and create a unique "name" that captures the names of and content
      // of all the arguments
      pseudoIdentity = mkPseudoIdentity(docId, luceneDocID, namedCaptures)
    } yield Match(docId, foundBy, pseudoIdentity, evidence)
  }

  private def triggerNamedCaptureOpt(m: OdinsonMatch): Option[NamedCapture] = {
    m match {
      case em: EventMatch => Some(NamedCapture("trigger", None, em.trigger))
      case _ => None
    }
  }


  /**
   * Create a representation of each the named captures that has: (a) the label of the named capture,
   *  (b) the normlized tokens of the result (if enabled), and (c) the original result
   * @param docId the Lucene DocID
   * @param namedCaptures the named captures for the match
   * @return sequence of NormalizedArg -- the wrapper class for the views described above
   */
  private def mkPseudoIdentity(docId: String, luceneDocID: Int, namedCaptures: Array[NamedCapture]): Seq[NormalizedArg] = {
    for {
      nc <- namedCaptures
      argName = nc.name
      capturedMatch = nc.capturedMatch
      tokens = extractorEngine.getTokens(luceneDocID, capturedMatch).toSeq
      normalizedTokens = if (consolidateByLemma) convertToLemmas(tokens) else tokens
    } yield NormalizedArg(argName, normalizedTokens, tokens)
  }

  private def convertToLemmas(words: Seq[String]): Seq[String] = {
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
