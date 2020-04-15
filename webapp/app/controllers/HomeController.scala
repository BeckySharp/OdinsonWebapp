package controllers

import java.text.SimpleDateFormat
import java.util.Date

import javax.inject._
import org.clulab.reading.{CorpusReader, Match}
import play.api.libs.json._
import play.api.mvc._


/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  // -------------------------------------------------
  println("CorpusReader is getting started ...")
  val reader = CorpusReader.fromConfig
  println("CorpusReader is ready to go ...")
  // -------------------------------------------------

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */

  def dev() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.dev())
  }

  def simple() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.simple())
  }

  def getCustomRuleResults(rules: String, exportMatches: Boolean) = Action {
    // println(s"[DEV] Query: <<$query>>\tRULE: <<$rule>>")
    val matches = reader.extractMatches(rules)
    if (exportMatches) {
      // fixme
      val localDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss")
      val outfile = s"${ruleNameHack(rules)}_${localDateFormat.format(new Date)}.jsonl"
      CorpusReader.writeMatchesTo(matches, outfile)
    }
    val resultsByRule = reader.consolidateMatches(matches)
    println(s"num results: ${resultsByRule.toSeq.flatMap(_._2).length}")
    val json = JsonUtils.mkJsonDict(resultsByRule)
//    println(Json.prettyPrint(json))
    Ok(json)
  }

  def ruleNameHack(rules: String): String = {
    val namePattern = """name:\s*([^\s\\]+)""".r
    val m = namePattern.findFirstMatchIn(rules)
    m.get.group(1)
  }

  def buildRules(data:String) = Action {
    println(data)
    val j = Json.parse(data)
    val ruleName = j("ruleName")
    val subj = j("subj")
    val verb = j("verb")
    val obj = j("obj")

    val output = Json.toJson("")
    Ok(output)
  }

}
