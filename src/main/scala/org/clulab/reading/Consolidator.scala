package org.clulab.reading

import scala.collection.mutable.{ArrayBuffer, HashMap}
import org.clulab.processors.Processor
import org.clulab.processors.fastnlp.FastNLPProcessor

case class ResultView(args: Seq[ArgString]) {
  def toArgMap: Map[String, String] = {
    args.map(a => (a.argName, a.results)).toMap
  }
}
/* The `results` is a concatenated string of all results for the same name arg*/
case class ArgString(argName: String, results: String)
case class ConsolidatedMatch(
  result: ResultView,
  count: Int,
  evidence: Seq[Evidence]
)

/** This class represents a Consolidator object that groups
 *  the results of a query by lemmas.
 */
class Consolidator(
  val processor: Processor,
  private val counts: HashMap[ResultView, Int],
  private val display: HashMap[ResultView, Map[String, String]],
  private val evidence: HashMap[ResultView, ArrayBuffer[Evidence]],
) {

  def this(processor: Processor) = this(processor, HashMap.empty, HashMap.empty, HashMap.empty)

  def this() = this(new FastNLPProcessor)

//  def keys = display.values

  def add(m: Seq[NormalizedArg], sentences: Seq[Evidence] = Nil): Unit = add(m, 1, sentences)

  def add(matchArgs: Seq[NormalizedArg], count: Int, sentences: Seq[Evidence]): Unit = {
    val matchKey = mkView(matchArgs, normalized = true)
    val displayView = mkView(matchArgs, normalized = false)
    counts(matchKey) = counts.getOrElse(matchKey, 0) + count
    evidence(matchKey) = evidence.getOrElse(matchKey, ArrayBuffer.empty)
    evidence(matchKey).appendAll(sentences)
    display.getOrElseUpdate(matchKey, displayView.toArgMap)
  }


  /** Returns the results of the consolidation process */
  def getMatches: Seq[ConsolidatedMatch] = {
    counts
      .toIterator
      .map { case (matchKey, count) =>
        ConsolidatedMatch(
          matchKey,
          count,
          evidence(matchKey)
        )
      }
      .toSeq
  }

  // Kinda like a pseudo-hash, makes a unique key based on the argument names and the results for each.
  // Note: Does not assume that the arguments are already sorted, sorts them here
  private def mkView(matchArgs: Seq[NormalizedArg], normalized: Boolean): ResultView = {
    val groupedByArg = matchArgs.groupBy(_.argName)
    val argViews = for {
      (argName, args) <- groupedByArg
      argsAsStrings = args.map(mkView(_, normalized))
      sorted = argsAsStrings.sorted // should be there, else wouldn't be a named capture
      concatenated = sorted.mkString(" ;; ")
    } yield ArgString(argName, concatenated)

    ResultView(argViews.toSeq.sortBy(_.argName))
  }
  private def mkView(n: NormalizedArg, normalized: Boolean): String = {
    if (normalized) n.normalizedTokens.mkString(" ")
    else n.originalTokens.mkString(" ")
  }



}

object Consolidator {
  /**
   * Consolidate results by rule and return the consolidated Matches, persisting
   * all evidence for downstream users.
   * @param matches
   * @return Map with consolidated matches (i.e., "deduplicated") for each rule (key)
   */
  def consolidateMatchesByRule(matches: Seq[Match], proc: Processor): Map[String, Seq[ConsolidatedMatch]] = {
    // Each rule may have different args and different numbers of args, so we'll need to display
    // them separately.
    val groupByRule = matches.groupBy(_.foundBy).toSeq
    val consolidated = for {
      (foundBy, matchGroup) <- groupByRule
      consolidatedRanked = consolidateAndRank(matchGroup, proc: Processor)
    } yield (foundBy, consolidatedRanked)
    consolidated
      .toMap
  }

  def consolidateAndRank(ms: Seq[Match], proc: Processor): Seq[ConsolidatedMatch] = {
    // count matches so that we can add them to the consolidator efficiently
    // Group by that unique identity mentioned above
    val regroupedMatches = ms
      .groupBy(_.pseudoIdentity)
      // get the count of how many times this result appeared and all the sentences where it happened
      // here the length of the values is the count, and we persist all of the evidence even while consolidating
      .mapValues(vs => (vs.length, vs.map(v => v.evidence)))
    // consolidate matches
    val consolidator = new Consolidator(proc)
    for ((pseudoIdentity, (count, sentences)) <- regroupedMatches.toSeq) {
      consolidator.add(pseudoIdentity, count, sentences)
    }
    // Rank the consolidated matches
    rankMatches(consolidator.getMatches)
  }

  private def rankMatches(matches: Seq[ConsolidatedMatch]): Seq[ConsolidatedMatch] = {
    matches.sortBy(-_.count)
  }

}