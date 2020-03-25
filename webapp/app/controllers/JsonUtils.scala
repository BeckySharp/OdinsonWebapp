package controllers

import play.api.libs.json._
import org.clulab.taxero._

/** utilities to convert odin mentions into json objects
 *  that can be returned in http responses
 */
object JsonUtils {

  def mkJson(ms: Seq[ScoredMatch]): JsValue = {
    Json.arr(ms.map(mkJson): _*)
  }

  def mkJson(m: ScoredMatch): Json.JsValueWrapper = {
    Json.arr(
      m.query.mkString(" "),
      m.result.mkString(" "),
      m.count,
      m.similarity,
      m.score,
    )
  }

  def mkJsonDict(ms: Seq[ScoredMatch]): JsValue = {
//    Json.obj(
//      "data" -> Json.arr(ms.map(mkJsonDict): _*)
//    )
    Json.arr(ms.map(mkJsonDict): _*)
  }
  def mkJsonDict(m: ScoredMatch): Json.JsValueWrapper = {
    Json.obj(
      "query"  -> m.query.mkString(" "),
      "result" -> m.result.mkString(" "),
      "count" -> m.count.toString,
      "similarity" -> m.similarity.toString,
      "score" -> m.score.toString,
      "evidence" -> m.evidence.map(mkJsonEvidence),
    )
  }

  def mkJsonEvidence(e: Evidence): JsValue = {
    Json.obj(
      "id" -> e.docID,
      "sentence" -> e.sentence
    )
  }

}
