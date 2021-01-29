package org.clulab.reading

import ai.lum.common.ConfigUtils._
import ai.lum.common.ConfigFactory
import DependencySearcher._
import org.clulab.embeddings.WordEmbeddingMap


case class DependencySimilarity(dep: String, display: String, score: Double)
class DependencySearcher {
  val config = ConfigFactory.load()
  val nmodW2V = new WordEmbeddingMap(config[String]("nmodVectors"))

  def mostSimilar(s: String, n: Int = 25): Seq[DependencySimilarity] = {
    // TODO: Sieve -- check nmod first?
    mostSimilarNmod(s, n)
  }

  // Assumes plain text word or phrase
  def mostSimilarNmod(s: String, n: Int): Seq[DependencySimilarity] = {
    val query = nmod(s)
    nmodW2V.mostSimilarWords(query, n)
      .map(result => DependencySimilarity(result._1, asPhrase(result._1), result._2))
  }

}

object DependencySearcher {
  /**
   * Convert a phrase (can be multi-word) string into the corresponding nmod dependency.
   * For example: "in front of" => "nmod_in_front_of"
   * @param s phrase to be converted
   * @return nmod representation
   */
  def nmod(s: String): String = s"nmod_${s.replace(" ", "_")}"

  /**
   * Convert an nmod dependency into the tokens that it consists of, minus the "nmod"
   * @param nmod
   * @return
   */
  def asTokens(nmod: String): Seq[String] = nmod.split("[:_]").slice(1,100)

  /**
   * Make a display-worthy representation of an nmod -- strip the "nmod" and
   * replace underscores with spaces
   * @param nmod
   * @return
   */
  def asPhrase(nmod: String): String = asTokens(nmod).mkString(" ")
}
