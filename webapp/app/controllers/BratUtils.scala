package controllers

import ai.lum.common.ConfigFactory
import ai.lum.common.ConfigUtils.LumAICommonConfigWrapper
import ai.lum.odinson.{Mention, OdinsonMatch}
import org.clulab.processors.{Document, Sentence}
import org.clulab.reading.utils.DisplayUtils
import play.api.libs.json._

import scala.collection.mutable.ArrayBuffer

class BratUtils(displayString: String) {
  protected def mkParseObj(sentence: Sentence, sb: StringBuilder): Unit = {
    def getTdAt(option: Option[Array[String]], n: Int): String = {
      val text = if (option.isEmpty) ""
      else option.get(n)

      "<td>" + xml.Utility.escape(text) + "</td>"
    }

    sentence.words.indices.foreach { i =>
      sb
        .append("<tr>")
        .append("<td>" + xml.Utility.escape(sentence.words(i)) + "</td>")
        .append(getTdAt(sentence.tags, i))
        .append(getTdAt(sentence.lemmas, i))
        .append(getTdAt(sentence.entities, i))
        .append(getTdAt(sentence.norms, i))
        .append(getTdAt(sentence.chunks, i))
        .append("</tr>")
    }
  }

  /** Make the table for the document displaying the annotation from Processors */
  protected def mkParseObj(doc: Document): String = {
    val header =
      """
        |  <tr>
        |    <th>Word</th>
        |    <th>Tag</th>
        |    <th>Lemma</th>
        |    <th>Entity</th>
        |    <th>Norm</th>
        |    <th>Chunk</th>
        |  </tr>
      """.stripMargin
    val sb = new StringBuilder(header)

    doc.sentences.foreach(mkParseObj(_, sb))
    sb.toString
  }

  def mkJson(text: String, doc: Document, mentions: Vector[Mention]): JsValue = {
    mentions.foreach{ m =>
      val display = DisplayUtils.displayString(m)
      println(display)
    }

    val sent = doc.sentences.head
    val syntaxJsonObj = Json.obj(
      "text" -> text,
      "entities" -> mkJsonFromTokens(doc),
      "relations" -> mkJsonFromDependencies(doc)
    )
    val eidosJsonObj = mkJsonForEidos(text, sent, mentions)
    val mentionsDetails = mkMentionDetailTextDisplay(mentions)
    val parseObj = mkParseObj(doc)

    Json.obj(
      "syntax" -> syntaxJsonObj,
      "mentions" -> eidosJsonObj,
      "mentionDetails" -> mentionsDetails,
      "parse" -> parseObj
    )
  }

  def mkMentionDetailTextDisplay(mentions: Vector[Mention]): String = {
    var objectToReturn = ""

    // Mention display format
    if (mentions.nonEmpty){
      objectToReturn += "<h2>Extractions:</h2>"
      for (m <- mentions) {
        objectToReturn += s"${DisplayUtils.webAppMention(m)}"
      }
    }

    objectToReturn += "<br>"
    objectToReturn
  }

  def mkJsonForEidos(sentenceText: String, sent: Sentence, mentions: Vector[Mention]): Json.JsValueWrapper = {
    val (topLevelNonevents, eventsAndRelations) = mentions.partition(_.arguments.isEmpty)
//    val (relations, events) = eventsAndRelations.partition(isRelationMention)
    val topLevelTBMs = topLevelNonevents.map(TextBoundMention.fromMention)

    // Handle Events:
    // collect triggers for event mentions
    val eventTriggers = new ArrayBuffer[TextBoundMention]
    val eventEntities = new ArrayBuffer[TextBoundMention]

    eventsAndRelations.foreach { event =>

      val eTBMs = event.arguments match {
        case hasTriggerArg if hasTriggerArg.contains("trigger") =>
          hasTriggerArg("trigger").toSeq
            .map(_.copy(label = event.label))
            .map(TextBoundMention.fromMention)

        case _ => Seq(TextBoundMention.fromMention(event))
      }
      eventTriggers.appendAll(eTBMs)

      val argTriggers = for {
        a <- event.arguments.values.flatten
        if a.arguments.nonEmpty
        if !(a.start == event.start && a.end == event.end)
      } yield TextBoundMention.fromMention(a)
      eventTriggers.appendAll(argTriggers)

      // collect event arguments as text bound mentions
      val eventEnts = for {
        e <- eventsAndRelations
        a <- e.arguments.values.flatten
      } yield TextBoundMention.fromMention(a)
      eventEntities.appendAll(eventEnts)
    }

    // generate id for each textbound mention
    val tbMentionToId = (eventEntities ++ eventTriggers ++ topLevelTBMs)
      .distinct
      .zipWithIndex
      .map { case (m, i) => (m, i + 1) }
      .toMap

    // return brat output
    Json.obj(
      "text" -> sentenceText,
      "entities" -> mkJsonFromEntities((eventEntities ++ topLevelTBMs).toVector, tbMentionToId),
      "triggers" -> mkJsonFromEntities(eventTriggers.toVector, tbMentionToId),
      "events" -> mkJsonFromEventMentions(eventsAndRelations, tbMentionToId)
    )
  }


  def mkJsonFromEntities(mentions: Vector[TextBoundMention], tbmToId: Map[TextBoundMention, Int]): Json.JsValueWrapper = {
    val entities = mentions.map(m => mkJsonFromTextBoundMention(m, tbmToId(m)))
    Json.arr(entities: _*)
  }

  def mkJsonFromTextBoundMention(m: TextBoundMention, i: Int): Json.JsValueWrapper = {
    Json.arr(
      s"T$i",
      m.label,
      // character offsets
      Json.arr(Json.arr(m.startOffset, m.endOffset))
    )
  }

  def mkJsonFromEventMentions(ee: Seq[Mention], tbmToId: Map[TextBoundMention, Int]): Json.JsValueWrapper = {
    var i = 0
    val jsonEvents = for (e <- ee) yield {
      i += 1
      mkJsonFromEventMention(e, i, tbmToId)
    }
    Json.arr(jsonEvents: _*)
  }

  def mkJsonFromEventMention(ev: Mention, i: Int, tbmToId: Map[TextBoundMention, Int]): Json.JsValueWrapper = {
    val triggerTBM = TextBoundMention.fromMention(ev)
    Json.arr(
      s"E$i",
      s"T${tbmToId(triggerTBM)}",
      Json.arr(mkArgMentions(ev.start, ev.end, ev.arguments, tbmToId): _*)
    )
  }

  def mkArgMentions(start: Int, end: Int, arguments: Map[String, Array[Mention]], tbmToId: Map[TextBoundMention, Int]): Seq[Json.JsValueWrapper] = {
    val args = for {
      argRole <- arguments.keys
      m <- arguments(argRole)
      if !(start == m.start && end == m.end)
    } yield {
      val arg = TextBoundMention.fromMention(m)
      mkArgMention(argRole, s"T${tbmToId(arg)}")
    }
    args.toSeq
  }

  def mkArgMention(argRole: String, id: String): Json.JsValueWrapper = {
    Json.arr(argRole, id)
  }

  def mkJsonFromTokens(doc: Document): Json.JsValueWrapper = {
    var offset = 0

    val tokens = doc.sentences.flatMap { sent =>
      val tokens = sent.words.indices.map(i => mkJsonFromToken(sent, offset, i))
      offset += sent.words.size
      tokens
    }
    Json.arr(tokens: _*)
  }

  def mkJsonFromToken(sent: Sentence, offset: Int, i: Int): Json.JsValueWrapper = {
    Json.arr(
      s"T${offset + i + 1}", // token id (starts at one, not zero)
      sent.tags.get(i), // lets assume that tags are always available
      Json.arr(Json.arr(sent.startOffsets(i), sent.endOffsets(i)))
    )
  }

  def mkJsonFromDependencies(doc: Document): Json.JsValueWrapper = {
    var offset = 1

    val rels = doc.sentences.flatMap { sent =>
      var relId = 0
      val deps = sent.dependencies.get // lets assume that dependencies are always available
      val rels = for {
        governor <- deps.outgoingEdges.indices
        (dependent, label) <- deps.outgoingEdges(governor)
      } yield {
        val json = mkJsonFromDependency(offset + relId, offset + governor, offset + dependent, label)
        relId += 1
        json
      }
      offset += sent.words.size
      rels
    }
    Json.arr(rels: _*)
  }

  def mkJsonFromDependency(relId: Int, governor: Int, dependent: Int, label: String): Json.JsValueWrapper = {
    Json.arr(
      s"R$relId",
      label,
      Json.arr(
        Json.arr("governor", s"T$governor"),
        Json.arr("dependent", s"T$dependent")
      )
    )
  }

  case class TextBoundMention(start: Int, end: Int, text: String, label: String, startOffset: Int, endOffset: Int) {

  }
  object TextBoundMention {
    def apply(odinsonMatch: OdinsonMatch, text: String, label: Option[String], sentenceRaw: Seq[String]): TextBoundMention = {
      var startOffset = 0
      for (raw <- sentenceRaw.slice(0, odinsonMatch.start)) {
        startOffset += raw.length
      }
      startOffset += odinsonMatch.start // add the spaces
      val endOffset = startOffset + text.length
      TextBoundMention(odinsonMatch.start, odinsonMatch.end, text, label.getOrElse("Mention"), startOffset, endOffset)
    }
    def fromMention(m: Mention): TextBoundMention = {
      apply(m.odinsonMatch, m.text, m.label, m.documentFields(displayString))
    }
  }


  def tab():String = "&nbsp;&nbsp;&nbsp;&nbsp;"
}

object BratUtils {
  def fromConfig: BratUtils = {
    val config = ConfigFactory.load()
    val displayField = config.get[String]("ui.displayField").getOrElse("raw")
    new BratUtils(displayField)
  }
}
