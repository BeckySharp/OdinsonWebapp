package controllers

import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date

import javax.inject._
import org.clulab.reading.{CorpusReader, Match, TextReader}
import org.clulab.reading.CorpusReader._
import play.api.mvc._

import scala.util.matching.Regex
import scala.collection.mutable

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  // -------------------------------------------------
  println("CorpusReader is getting started ...")
  val reader = CorpusReader.fromConfig
  val proc = reader.proc
  println("CorpusReader is ready to go ...")
  // -------------------------------------------------

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
//  def index() = Action { implicit request: Request[AnyContent] =>
//    Ok(views.html.index())
//  }

  def dev() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.dev())
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
    val resultsByRule = consolidateMatches(matches, reader.proc)
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

  def saveRules(rules: String, filename: String) = Action {
    val pw = new PrintWriter(filename)
    pw.write(rules)
    pw.close()

    println(s"saved rules to $filename")

    val json = JsonUtils.mkJsonDict(Map.empty)
    Ok(json)
  }


  def processText: Action[AnyContent] = Action { request =>
    val data = request.body.asJson.get.toString()
    val j = ujson.read(data)
    val rules = j("rulefile").str
    val textReader = TextReader.fromFile(proc, rules)
    assert(!( j.obj.keySet.contains("textfile") && j.obj.keySet.contains("text") ),
      "You cannot pass both a textfile and text in a single request.")
    val matches = if (j.obj.get("textfile").isDefined) {
      val textFile = j("textfile").str
      textReader.extractMatchesFromFile(textFile)
    } else if (j.obj.get("text").isDefined) {
      val text = j("text").str
      textReader.extractMatches(text)
    } else {
      throw new RuntimeException("You must pass either a `textfile` or a `text`")
    }
    val jsonMatches = JsonUtils.asJsonArray(matchesAsJsonStrings(matches))
    Ok(jsonMatches)
  }

}
