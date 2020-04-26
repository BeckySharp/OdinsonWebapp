package org.clulab.reading

import ai.lum.odinson.{Document => OdinsonDocument}
import org.clulab.processors.Processor

class TextReader(val proc: Processor) {

  def processText(text: String): Seq[OdinsonDocument] = {
    val procDoc = proc.annotate(text)
    val odinsonDocument =
  }

}
