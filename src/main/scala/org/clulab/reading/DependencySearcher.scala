package org.clulab.reading

import ai.lum.common.ConfigUtils._
import ai.lum.common.ConfigFactory
import org.clulab.embeddings.word2vec.Word2Vec
import utils.MakeNmodEmbeddings.asTokens


case class DependencySimilarity(dep: String, display: String, score: Double)
class DependencySearcher {
  val config = ConfigFactory.load()
  val nmodW2V = new Word2Vec(config[String]("nmodVectors"))

  def mostSimilar(s: String, n: Int = 25): Seq[DependencySimilarity] = {
    // Sieve -- check nmod first

    mostSimilarNmod(s, n)
  }

  // Assumes plain text word or phrase
  def mostSimilarNmod(s: String, n: Int): Seq[DependencySimilarity] = {
    val query = nmod(s)
    nmodW2V.mostSimilarWords(query, n)
      .map(result => DependencySimilarity(result._1, display(result._1), result._2))
  }

  def nmod(s: String): String = s"nmod_${s.replace(" ", "_")}"
  def display(s: String): String = asTokens(s).mkString(" ")

}
