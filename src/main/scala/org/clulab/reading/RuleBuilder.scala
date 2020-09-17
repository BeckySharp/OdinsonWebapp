package org.clulab.reading

import ai.lum.odinson.Rule
import ujson.Value
import org.clulab.reading.RuleVariants._
import org.clulab.reading.DependencySearcher.nmod
import org.clulab.utils.StringUtils.porterStem
import org.clulab.processors.clu.tokenizer.EnglishLemmatizer
import org.slf4j.{Logger, LoggerFactory}
import RuleBuilder.logger

class RuleBuilder {

  lazy val random = scala.util.Random
  lazy val lemmatizer = new EnglishLemmatizer()
  // todo: read from config:
  //  - whether or not to lemmatize subj and obj (default=true)
  //  - whether or not to "expand" the subj and obj to NP (default=true)

  // todo: what do entitiy rules look like? they're prob not SVO...
  //  - or maybe they are, but you don't particularly need to keep the results?
  //  - but they should HAVE to be SVO -- consider model names or drugs such as X...
  //  - so maybe other templates...?

  // While the Odin Engine really needs Extractors, here we produce the rules themselves so that they can
  // be displayed in the "expert" version of the webapp for editing
  def buildRules(ruleInfo: Value): Seq[Rule] = {
    val ruleData = ruleInfo.obj
    println(ruleData)
    val ruleName = getRuleName(ruleData.get("ruleName"))
    // todo: handle if S/V/O missing
    // todo: should we stem the subj?
    val subjectArg = mkArgInfo(ruleData.get("subj"), lemmatize = true)
    // Here we stem the verb to get the different formulations
    val verbArg = mkArgInfo(ruleData.get("verb"), stem = true)
    // todo: should we stem the obj?
    val objArg = mkArgInfo(ruleData.get("obj"), lemmatize = true)

    // Create arguments from any given nmod modifiers
    // todo: handle other args!
    val mods = getNModArgs(ruleData.get("mods"))

    val rules = combineArgs(ruleName, subjectArg, verbArg, objArg, mods)

    // Debug display for dev work
    println("******************")
    println(RuleBuilder.displayRules(rules))
    println("******************")

    rules
  }

  private def getRuleName(v: Option[Value]): String = {
    if (v.isDefined) {
      v.get.str
    } else {
      s"rule-${random.nextString(10)}"
    }
  }


  private def mkArgInfo(arg: Option[Value], lemmatize: Boolean = false, stem: Boolean = false): Option[ArgInfo] = {
    if (arg.isDefined) {
      val label = arg.get("label").str
      // todo -- convert to constraints
      val words = arg.get("words").str
      val constraints = mkLexicalConstraints(words, lemmatize, stem)
      // todo: convert to optional
      val optional = arg.get("argType").str == "optional"
      return Some(ArgInfo(label, constraints, optional))
    }

    None
  }

  private def mkLexicalConstraints(w: String, lemmatize: Boolean, stem: Boolean): Seq[Constraint] = {
    if (lemmatize && stem) {
      logger.warn("Both lemmatization and stemming are selected, this is likely to be inefficient and may not produce what you're expecting...")
    }
    // If there are no constraints, should be a wildcard
    if (w == "") return Seq(EmptyConstraint())

    val elements = w.split(",")
      .map(s => s.trim)
      .map(s => maybeLemmatize(lemmatize, s))
      .map(s => maybeStem(stem, s))
    // TODO: check for NER

    val field = if(lemmatize) "lemma" else "norm"
    val value = s"""/${elements.mkString("|")}/"""
    val wordConstraint = TokenConstraint(field, value)
    // todo: assert that there should only be one of each type of constraint
    Seq(wordConstraint)
  }

  private def maybeLemmatize(lemmatize: Boolean, s: String): String = {
    if (!lemmatize) return s
    lemmatizer.lemmatizeWord(s)
  }

  private def maybeStem(stem: Boolean, s: String): String = {
    if (!stem) return s
    s"""${porterStem(s)}.*"""
  }

  // If there were any nmod arguments passed in, convert them to ArgInfos, with a `path` made from the words
  private def getNModArgs(args: Option[Value]): Seq[ArgInfo] = {
    def getNmodArg(arg: Value): ArgInfo = {
      val label = arg("label").str
      val words = arg("words").str
      // convert the words to an nmod path
      // Split on comma to get each selected option
      val nmodOptions = words.split(",")
        // convert each to the corresponding nmod dependency
        .map(nmod)
        // make each outgoing
        // todo: always outgoing?
        .map(n => s">$n")
        // make the regex for the options
        .mkString(" | ")
      val path = s"($nmodOptions)"
      val constraints = mkLexicalConstraints("", lemmatize=false, stem = false) // pass an empty string so that the constraints are a [] wildcard
      val optional = arg("argType").str == "optional"
      ArgInfo(label, constraints, optional, path)
    }

    if (args.isDefined) {
      args.get.arr.map(getNmodArg)
    } else {
      Seq()
    }
  }

  private def combineArgs(
    ruleName: String,
    subj: Option[ArgInfo],
    verb: Option[ArgInfo],
    obj: Option[ArgInfo],
    mods: Seq[ArgInfo]
  ): Seq[Rule] = {
    // todo: use mods
    // todo: expansion
    val declarativeRule = Rule(s"$ruleName-decl", None, "event", "1", declarative(subj, verb, obj, mods))
    val passiveRule = Rule(s"$ruleName-passive", None, "event", "1", passive(subj, verb, obj, mods))
    val prepNominalizationRule = Rule(s"$ruleName-prepNominalization", None, "event", "1", prepositionalNominalization(subj, verb, obj, mods))

    Seq(declarativeRule, passiveRule, prepNominalizationRule)
  }



}

object RuleBuilder {
  lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)
  def displayRules(rr: Seq[Rule]): String = {
   rr.map(_.toString).mkString("\n\n")
  }
}
