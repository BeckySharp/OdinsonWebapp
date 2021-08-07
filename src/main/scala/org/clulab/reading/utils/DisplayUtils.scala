package org.clulab.reading.utils

import ai.lum.common.StringUtils._
import ai.lum.odinson.Mention
import org.clulab.reading.{Match, NormalizedArg}
import ujson.Value

import scala.collection.mutable.ArrayBuffer

object DisplayUtils {

  protected val nl = "\n"
  protected val tab = "\t"

  implicit class StringImprovements(s: String) {
    def wordWrap(maxLength: Int, indentation: Int): String = {
      val lines = wrap(maxLength)
      lines.map(line => s"${indent(indentation)}$line").mkString("\n")
    }
    private def indent(n: Int): String = " " * n
    private def wrap(maxLength: Int): Array[String] = {
      s.split(" ").foldLeft(Array(""))( (out, in) => {
        if ((out.last + " " + in).trim.length > maxLength) out :+ in
        else out.updated(out.size - 1, out.last + " " + in)
      })
    }
  }

  def displayString(m: Mention): String = {
    // note: assumes that there is only one sentence
    val sentenceTokens = m.documentFields("raw")
    val docId = s"doc: ${m.docId}\tsent: ${m.sentenceId}"
    val lines = new ArrayBuffer[String]
    lines.append(s"{")
    lines.append(s"  source:   ${docId}")
    lines.append(s"  sentence: \n${m.documentFields("raw").mkString(" ").wordWrap(100, 6)}")
    lines.append(s"  foundBy:  ${m.foundBy}")
    lines.append(s"  label:    ${m.label.getOrElse("<NONE>")}")
    lines.append(s"  text/trigger:     ${m.text}")

    // Display the arguments
    if (m.arguments.nonEmpty) {
      lines.append("\n  arguments:")
      val argKeys = m.arguments.keySet
      // Handle each of the arguments
      for (argName <- argKeys) {
        // Get the extractions for the current argument
        val argumentMentions = m.arguments(argName)
        // Convert the Extraction to a string representation
        lines.appendAll(argStrings(argName, argumentMentions, sentenceTokens))
      }
    }
    lines.append("}\n")
    lines.mkString("\n")
  }

  def argStrings(argName: String, argMentions: Array[Mention], sentence: Seq[String]): Seq[String] = {
    for {
      m <- argMentions
      text = m.text
      label = m.label.getOrElse("")
    } yield {
      s"  \t$argName($label): ${text}"
    }
  }


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


  def webAppMention(mention: Mention): String =
    xml.Utility
      .escape(displayString(mention))
      .replaceAll(nl, "<br>")
      .replaceAll(tab, "&nbsp;&nbsp;&nbsp;&nbsp;")


}
