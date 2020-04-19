package org.clulab.reading

import scala.collection.mutable.ArrayBuffer

object RuleVariants {

  val AGENT_DEPS = ">nsubj"
  val DOBJ_DEPS = ">dobj"

  def declarative(
    subj: Option[ArgInfo],
    verb: Option[ArgInfo],
    obj: Option[ArgInfo],
    mods: Seq[ArgInfo]
  ): String = {
    // The verb needs to be defined for SVO currently
    assert(verb.isDefined)

    val ruleElems = new ArrayBuffer[String]
    // Add the trigger
    ruleElems.append(mkTrigger(verb.get.constraintString))
    // Add the subject
    subj foreach { s =>
      ruleElems.append(mkArg(s.label, AGENT_DEPS, s.constraintString, s.optional))
    }
    // Add the object
    obj foreach { o =>
      ruleElems.append(mkArg(o.label, DOBJ_DEPS, o.constraintString, o.optional))
    }
    // Add any extra modifiers
    mods foreach { m =>
      ruleElems.append(mkArg(m.label, m.path, m.constraintString, m.optional))
    }

    // Unnecessary, but useful for debug
    val assembledRule: String = ruleElems.mkString("\n")
    println(assembledRule)

    assembledRule
  }


  // -------------------------------------------------------------------------
  //                        General Methods for all Rules
  // -------------------------------------------------------------------------

  def mkTrigger(tokenConstraint: String): String =  s"""trigger = ${tokenConstraint}"""
  def mkArg(label: String, path: String, tokenConstraint: String, optional: Boolean): String = {
    val optChar = if (optional) "?" else ""
    s"""$label$optChar = $path ${tokenConstraint}"""
  }

}
