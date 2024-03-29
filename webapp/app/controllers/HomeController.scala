package controllers

import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date

import ai.lum.common.ConfigFactory
import javax.inject._
import org.clulab.dynet.Utils.initializeDyNet
import org.clulab.processors.clu.CluProcessor
import org.clulab.reading.{CorpusReader, DependencySearcher, Match, RuleBuilder, TextReader}
import org.clulab.reading.Consolidator._
import org.clulab.reading.CorpusReader._
import org.clulab.reading.utils.DisplayUtils
import play.api.mvc.{Action, _}


/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  // -------------------------------------------------
  lazy val reader = CorpusReader.fromConfig
  lazy val proc = {
    println("Initializing the CluProcessor")
    initializeDyNet()
    new CluProcessor()
  }
  lazy val ruleBuilder = new RuleBuilder()
  var nmodSearcher: Option[DependencySearcher] = None
  lazy val bratUtils = BratUtils.fromConfig
  println("OdinsonWebapp is ready to go ...")
  // -------------------------------------------------
  def initializeCorpusReader(): Unit = {
    if (reader.proc.isEmpty) {
      println("Initializing CorpusReader")
      reader.proc = Some(proc)
    }
  }

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */

  def dev() = Action { implicit request: Request[AnyContent] =>
    initializeCorpusReader()
    Ok(views.html.dev())
  }

  def simple() = Action { implicit request: Request[AnyContent] =>
    initializeCorpusReader()
    Ok(views.html.simple())
  }

  def parser(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    initializeCorpusReader()
    Ok(views.html.parse())
  }

  def getCustomRuleResults(rules: String, exportMatches: Boolean) = Action {
    // Make sure the reader is initialized
    initializeCorpusReader()

    val matches = reader.extractMatchesFromRules(rules, true)
    if (exportMatches) {
      exportResults(ruleNameHack(rules), matches)
    }
    val resultsByRule = consolidateMatchesByRule(matches, proc)
    println(s"num results: ${resultsByRule.toSeq.flatMap(_._2).length}")
    val json = JsonUtils.mkJsonDict(resultsByRule)
    Ok(json)
  }

  def ruleNameHack(rules: String): String = {
    val namePattern = """name:\s*([^\s\\]+)""".r
    val m = namePattern.findFirstMatchIn(rules)
    m.get.group(1)
  }

  def buildRules(data:String) = Action {
    // todo: making an Obj in two places now... refactor?
    val j = ujson.read(data)
    val ruleName = j.obj.get("ruleName").map(_.str).getOrElse("NO_NAME")
    val rules = ruleBuilder.buildRules(j)
    val matches = reader.extractMatchesFromRules(rules, true)
    val reformatted = DisplayUtils.replaceTriggerName(j, matches)
    // TODO: export if desired
    val results = consolidateAndRank(reformatted, proc)
    println(s"num results: ${results.length}")
    val out = JsonUtils.mkJsonDict(Map(ruleName -> results))
    Ok(out)
  }

  def exportResults(filePrefix: String, matches: Seq[Match]): Unit = {
    // fixme
    val localDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss")
    val outfile = s"${filePrefix}_${localDateFormat.format(new Date)}.jsonl"
    CorpusReader.writeMatchesTo(matches, outfile)
  }

  // Used to get other nmod suggestions for the modifier panel, given an nmod query
  def getSimilarMods(query: String) = Action {
    if (nmodSearcher.isEmpty) {
      nmodSearcher = Some(new DependencySearcher)
    }
    val similarNmods = nmodSearcher.get.mostSimilar(query)
    val json = JsonUtils.mkJsonSimilarities(similarNmods)
    Ok(json)
  }

  def saveRules(rules: String, filename: String) = Action {
    val pw = new PrintWriter(filename)
    pw.write(rules)
    pw.close()

    println(s"saved rules to $filename")

    val json = JsonUtils.mkJsonDict(Map.empty)
    Ok(json)
  }


  def processTextDynamic(sent: String, rules: String) = Action {
    println(s"Parsing sent: $sent")
    val textReader = new TextReader(proc, rules)
    val doc = textReader.mkPartialAnnotation(sent)
    val mentions = textReader.extractMentions(doc, populate = true, topLevelOnly = false).toVector
    val json = bratUtils.mkJson(sent, doc, mentions)
    Ok(json)
  }

  def processText: Action[AnyContent] = Action { request =>
    val data = request.body.asJson.get.toString()
    val j = ujson.read(data)
    val rules = j("rulefile").str
    val textReader = TextReader.fromFile(proc, rules)
    assert(!( j.obj.keySet.contains("textfile") && j.obj.keySet.contains("text") ),
      "You cannot pass both a textfile and text in a single request.")
    val matches = if (j.obj.contains("textfile")) {
      val textFile = j("textfile").str
      textReader.extractMatchesFromFile(textFile, topLevelOnly = false)
    } else if (j.obj.contains("text")) {
      val text = j("text").str
      textReader.extractMatches(text, topLevelOnly = false)
    } else {
      throw new RuntimeException("You must pass either a `textfile` or a `text`")
    }
    val jsonMatches = JsonUtils.asJsonArray(matchesAsJsonStrings(matches))
    Ok(jsonMatches)
  }
  
}
