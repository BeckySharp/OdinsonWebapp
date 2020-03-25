package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import org.clulab.taxero._

import scala.collection.mutable

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  // -------------------------------------------------
  println("Taxero is getting started ...")
  val taxero = TaxonomyReader.fromConfig
  println("Taxero is ready to go ...")
  // -------------------------------------------------

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def dev() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.dev())
  }

  def getHypernyms(query: String) = Action {
    val tokens = query.split(" ")
    val hypernyms = taxero.getRankedHypernyms(tokens)
    val json = JsonUtils.mkJson(hypernyms)
    Ok(json)
  }

  def getHyponyms(query: String) = Action {
    val tokens = query.split(" ")
    val hyponyms = taxero.getRankedHyponyms(tokens)
    val json = JsonUtils.mkJson(hyponyms)
    Ok(json)
  }

  def getCohyponyms(query: String) = Action {
    val tokens = query.split(" ")
    val cohyponyms = taxero.getRankedCohyponyms(tokens)
    val json = JsonUtils.mkJson(cohyponyms)
    Ok(json)
  }

  def getExpandedHypernyms(query: String, n: Int) = Action {
    val tokens = query.split(" ")
    val hypernyms = taxero.getExpandedHypernyms(tokens, n)
    val json = JsonUtils.mkJson(hypernyms)
    Ok(json)
  }

  def getCustomRuleResults(query: String, rules: String) = Action {
    // println(s"[DEV] Query: <<$query>>\tRULE: <<$rule>>")
    val tokens = query.split(" ")
    val results = taxero.executeGivenRules(tokens, rules)
    println(s"num results: ${results.length}")
    val json = JsonUtils.mkJsonDict(results.slice(0,3))
    println(Json.prettyPrint(json))
    Ok(json)
  }

}
