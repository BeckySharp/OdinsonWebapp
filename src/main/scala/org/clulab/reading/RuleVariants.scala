package org.clulab.reading

import scala.collection.mutable.ArrayBuffer

object RuleVariants {

  val AGENT_DEPS = ">nsubj|>agent"
  val DOBJ_DEPS = ">dobj"
  val PASSIVE_AGENT_DEPS = ">nmod_by|>nmod_agent"
  val PASSIVE_THEME_DEPS = ">nsubjpass"
  val NMOD_OF = ">nmod_of"

  def declarative(
    subj: Option[ArgInfo],
    verb: Option[ArgInfo],
    obj: Option[ArgInfo],
    mods: Seq[ArgInfo]): String = {

    assert(verb.isDefined)
    svoTemplate(subj, AGENT_DEPS, verb, obj, DOBJ_DEPS, mods)
  }

  def passive(
    subj: Option[ArgInfo],
    verb: Option[ArgInfo],
    obj: Option[ArgInfo],
    mods: Seq[ArgInfo]): String = {

    // The verb needs to be defined for SVO currently
    assert(verb.isDefined)

    svoTemplate(
      subj,
      PASSIVE_AGENT_DEPS,
      // Here there is an additional constraint on the verb for this particular variant
      Some(verb.get.withLocalContraint(TokenConstraint("tag", "VBN"))),
      obj,
      PASSIVE_THEME_DEPS,
      mods)

  }

  def prepositionalNominalization(
    subj: Option[ArgInfo],
    verb: Option[ArgInfo],
    obj: Option[ArgInfo],
    mods: Seq[ArgInfo]): String = {

    assert(verb.isDefined)
    svoTemplate(
      subj,
      PASSIVE_AGENT_DEPS,
        // Here the "verb" is nominalized
      Some(verb.get.withLocalContraint(TokenConstraint("tag", "NN"))),
      obj,
      NMOD_OF,
      mods)
  }

  def svoTemplate(
    subj: Option[ArgInfo],
    subjPath: String,
    verb: Option[ArgInfo],
    obj: Option[ArgInfo],
    objPath: String,
    mods: Seq[ArgInfo]
  ): String = {

    val ruleElems = new ArrayBuffer[String]
    // Add the trigger
    ruleElems.append(mkTrigger(verb.get.constraintString))
    // Add the subject
    subj foreach { s =>
      ruleElems.append(mkArg(s.label, subjPath, s.constraintString, s.optional))
    }
    // Add the object
    obj foreach { o =>
      ruleElems.append(mkArg(o.label, objPath, o.constraintString, o.optional))
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
