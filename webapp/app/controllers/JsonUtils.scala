package controllers

import org.clulab.reading.{ArgString, ConsolidatedMatch, Evidence, ResultView}
import play.api.libs.json._

/** utilities to convert odin mentions into json objects
 *  that can be returned in http responses
 */
object JsonUtils {

//  def mkJson(ms: Seq[ScoredMatch]): JsValue = {
//    Json.arr(ms.map(mkJson): _*)
//  }
//
//  def mkJson(m: ScoredMatch): Json.JsValueWrapper = {
//    Json.arr(
//      m.query.mkString(" "),
//      m.result.mkString(" "),
//      m.count,
//      m.similarity,
//      m.score,
//    )
//  }

  def mkJsonDict(ruleResults: Map[String, Seq[ConsolidatedMatch]]): JsValue = {
    Json.arr(ruleResults.toSeq.map(res => mkJsonDict(res._1, res._2)): _*)
  }
  def mkJsonDict(rule: String, results: Seq[ConsolidatedMatch]): Json.JsValueWrapper = {
    // Each rule has diff arguments, both in number and name
    val argNames = getArgNames(results)
    Json.obj(
      "rule"  -> rule,
      "arguments" -> Json.arr(argNames),
      // put the results in the same order as the argNames above to pop into the table
      "results" -> results.map(r => mkJsonConsolidatedMatch(r, argNames))
    )
  }

//  case class ResultView(args: Seq[ArgString]) {
//    def toArgMap: Map[String, String] = {
//      args.map(a => (a.argName, a.results)).toMap
//    }
//  }

  // case class ArgString(argName: String, results: String)

  def getArgNames(cms: Seq[ConsolidatedMatch]): Seq[String] = {
    cms
      .flatMap(cm => cm.result.toArgMap.keys)
      .distinct
      .sorted
  }

  def mkJsonConsolidatedMatch(cm: ConsolidatedMatch, argNames: Seq[String]): JsValue = {
    Json.obj(
      "result" -> mkJsonResultView(cm.result, argNames),
      "count" -> cm.count.toString,
      "evidence" -> cm.evidence.map(mkJsonEvidence),
    )
  }

  def mkJsonResultView(view: ResultView, argNames: Seq[String]): JsValue = {
    val argMap = view.toArgMap
    val orderedResults = argNames.map(name => argMap.getOrElse(name, ""))
    Json.arr(orderedResults)
  }

  def mkJsonEvidence(e: Evidence): JsValue = {
    Json.obj(
      "id" -> e.docID,
      "sentence" -> e.sentence
    )
  }

}
