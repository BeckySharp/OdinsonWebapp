package org.clulab.reading.utils

import org.clulab.reading.{Match, NormalizedArg}
import ujson.Value

object DisplayUtils {

  def replaceTriggerName(ruleInfo: Value, matches: Seq[Match]): Seq[Match] = {
    def replaceTrigger(m: Match, alias: String): Match = {
      val newArgs = for {
        na <- m.pseudoIdentity
        newRole = if(na.argName == "trigger") alias else na.argName
      } yield NormalizedArg(newRole, na.normalizedTokens, na.originalTokens)
      Match(m.docId, m.foundBy, newArgs, m.evidence)
    }

    val ruleData = ruleInfo.obj
    val verbInfo = ruleData.get("verb")
    val verbLabel = if (verbInfo.isDefined) verbInfo.get("label").str else "verb"

    matches.map(m => replaceTrigger(m, verbLabel))
  }

}
