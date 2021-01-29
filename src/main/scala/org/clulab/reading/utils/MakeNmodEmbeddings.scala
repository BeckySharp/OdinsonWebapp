package org.clulab.reading.utils

import java.io.{File, PrintWriter}

import ai.lum.common.ConfigUtils._
import ai.lum.common.TryWithResources.using
import ai.lum.common.ConfigFactory
import org.clulab.embeddings.WordEmbeddingMap
import org.clulab.reading.DependencySearcher.asTokens

import scala.io.Source

object MakeNmodEmbeddings extends App {

  def loadDependencies(fn: String): Seq[String] = {
    val source = Source.fromFile(fn, "ISO-8859-1")
    val lines = source.getLines().toArray
    source.close()
    lines
  }

  // method borrowed from processors W2V class
  // here we're using it to save the nmod embeddings in the right format to be
  // read in as a W2V object
  def saveMatrix(mf: String, matrix: Map[String, Array[Double]]) {
    val dimensions = w2v.dimensions
    val pw = new PrintWriter(mf)
    pw.println(s"${matrix.size}, $dimensions")
    for ((word, vec) <- matrix) {
      val strRep = vec.map(_.formatted("%.6f")).mkString(" ")
      pw.println(s"$word $strRep")
    }
    pw.close()
  }
  // average word embeddings for the words
  // todo: (?) if any word not in emb, discard whole thing, display warning
  def mapNmodEmbeddings(deps: Seq[String]): Seq[(String, Array[Double])] = {
    for {
      dep <- deps.filter(_.startsWith("nmod"))
      tokens = asTokens(dep)
      emb = w2v.makeCompositeVector(tokens)
    } yield (dep, emb)
  }



  // Load the deps vocab for the index
  val config = ConfigFactory.load()
  val indexDir: String = config[String]("odinson.indexDir")
  val nmodDeps = loadDependencies(s"$indexDir/dependencies.txt")

  // Load w2v Embeddings
  val vectors: String = config[String]("wordVectors")
  val w2v = new WordEmbeddingMap(vectors)

  // for each nmod, split on _ and remove nmod
  val nmodEmbeds = mapNmodEmbeddings(nmodDeps)

  // store embeddings for each nmod, make sure to update header of the file
  val nmodVectorsOut: String = config[String]("nmodVectors")
  saveMatrix(nmodVectorsOut, nmodEmbeds.toMap)
}
