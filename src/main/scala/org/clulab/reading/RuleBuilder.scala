package org.clulab.reading

import ai.lum.odinson.Rule
import ujson.Value
import org.clulab.reading.RuleVariants._


case class ArgInfo(label: String, constraints: Seq[Constraint], optional: Boolean) {
  def constraintString: String = {
    s"[${constraints.map(c => c.str).mkString(" & ")}]"
  }
}

trait Constraint {
  def str: String
}
case class TokenConstraint(field: String, value: String, negated: Boolean = false) extends Constraint {
  def str: String = s"$field=$value"
}
case class EmptyConstraint() extends Constraint {
  def str: String = ""
}


class RuleBuilder {

  lazy val random = scala.util.Random

  // While the Odin Engine really needs Extractors, here we produce the rules themselves so that they can
  // be displayed in the "expert" version of the webapp for editing
  def buildRules(ruleInfo: Value): Seq[Rule] = {
    val ruleData = ruleInfo.obj
    println(ruleData)
    val ruleName = getRuleName(ruleData.get("ruleName"))
    val subjectArg = mkArgInfo(ruleData.get("subj"))
    val verbArg = mkArgInfo(ruleData.get("verb"))
    val objArg = mkArgInfo(ruleData.get("obj"))

    // todo: modifiers
    val rules = combineArgs(ruleName, subjectArg, verbArg, objArg)

    rules
  }

  private def getRuleName(v: Option[Value]): String = {
    if (v.isDefined) {
      v.get.str
    } else {
      s"rule-${random.nextString(10)}"
    }
  }

  private def mkArgInfo(arg: Option[Value]): Option[ArgInfo] = {
    println("************")
    println(arg)
    if (arg.isDefined) {
      val label = arg.get("label").str
      // todo -- convert to constraints
      val words = arg.get("words").str
      val constraints = mkConstraints(words)
      // todo: convert to optional
      val optional = arg.get("argType").str == "optional"
      return Some(ArgInfo(label, constraints, optional))
    }

    None
  }

  private def mkConstraints(w: String): Seq[Constraint] = {
    // If there are no constraints, should be a wildcard
    if (w == "") return Seq(EmptyConstraint())

    val elements = w.split(",").map(_.trim)
    // TODO: check for NER
    // For now, only handle words, use norm field and, join with OR
    val field = "norm"
    val value = s"""/${elements.mkString("|")}/"""
    val constraint = TokenConstraint(field, value)
    // todo: assert that there should only be one of each type of constraint
    Seq(constraint)
  }

  private def combineArgs(ruleName: String, subj: Option[ArgInfo], verb: Option[ArgInfo], obj: Option[ArgInfo]): Seq[Rule] = {

    val declarativeRule = Rule(s"$ruleName-decl", None, "event", declarative(subj, verb, obj))
    println(declarativeRule)
    Seq(declarativeRule)
  }



}

object RuleBuilder {

}
