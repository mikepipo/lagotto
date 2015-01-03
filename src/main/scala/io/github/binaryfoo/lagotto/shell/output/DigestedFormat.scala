package io.github.binaryfoo.lagotto.shell.output

import io.github.binaryfoo.lagotto.dictionary.DataDictionary
import io.github.binaryfoo.lagotto.dictionary.NameType.NameType
import io.github.binaryfoo.lagotto.shell.OutputFormat
import io.github.binaryfoo.lagotto.{DefaultDateTimeFormat, LogEntry, LogLike}

case class DigestedFormat(dictionary: DataDictionary, nameType: NameType) extends OutputFormat {

  override def header(): Option[String] = None

  override def footer(): Option[String] = None

  override def apply(e: LogLike): Option[String] = {
    Some(e match {
      case l: LogEntry => format(l)
      case _ => e.exportAsSeq.mkString("\n")
    })
  }

  def format(e: LogEntry): String = {
    e.exportAsSeq.collect { case (k, v) if !headerAttributes.contains(k) =>
      val translatedValue = dictionary.translateValue(k, e, v).map(t => s"$v ($t)").getOrElse(v)
      dictionary.nameOf(nameType, k, e).map(name => s"  $k ($name): $translatedValue").getOrElse(s"  $k: $translatedValue")
    }.mkString(entryHeading(e), "\n", "")
  }

  private def entryHeading(e: LogEntry) = {
    val b = new StringBuilder("<log")
    addAttribute(b, "realm", e.realm.toString)
    addAttribute(b, "at", DefaultDateTimeFormat.print(e.timestamp))
    val lifespan = e.lifespan
    if (e.msgType != null) {
      addAttribute(b, "type", e.msgType)
    }
    if (lifespan.isDefined) {
      addAttribute(b, "lifespan", lifespan.get.toString)
    }
    b.append(">\n")
    b.toString()
  }

  private def addAttribute(b: StringBuilder, name: String, value: String) = {
    b.append(' ').append(name).append("=\"").append(value).append('"')
  }

  private val headerAttributes = Set("realm", "at", "msgType", "lifespan")
}
