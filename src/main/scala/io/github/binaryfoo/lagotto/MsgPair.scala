package io.github.binaryfoo.lagotto

import io.github.binaryfoo.lagotto.Iso8583._
import org.joda.time.DateTime

import scala.collection.mutable

/**
 * A single request paired with its response. Eg an auth (0200) and reply (0210).
 */
case class MsgPair(request: LogEntry, response: LogEntry) extends Coalesced with LogLike {

  def apply(field: String): String = {
    field match {
      case "rtt" => rtt.toString
      case MsgPairFieldAccess.Request(_, f) => request(f)
      case MsgPairFieldAccess.Response(_, f) => response(f)
      case _ =>
        val v = request(field)
        if (v == null) response(field) else v
    }
  }

  def rtt: Long = response.timestamp.getMillis - request.timestamp.getMillis

  def timestamp: DateTime = request.timestamp

  def mti: String = this("mti")

  override def toString: String = s"Pair(req=${request.fields.mkString("{", ",", "}")},resp=${response.fields.mkString("{", ",", "}")})"

  override lazy val lines: String = "<pair>\n" + request.lines + "\n" + response.lines + "\n</pair>"

  override def exportAsSeq: Seq[(String, String)] = request.exportAsSeq ++ response.exportAsSeq
}

object MsgPair {

  def coalesce(seq: Iterator[MsgPair], selector: MsgPair => String): Iterator[Coalesced] = Collapser.coalesce(seq, selector)

  /**
   * Match requests with responses based on MTI, STAN (field 11) and realm.
   */
  def pair(list: Iterator[LogEntry]): Iterator[MsgPair] = {
    val pending = new mutable.LinkedHashMap[String, LogEntry]

    list.flatMap { e =>
      val mti = e.mti
      if (mti != null) {
        val partnersKey = key(invertMTI(mti), e)
        pending.get(partnersKey) match {
          case Some(other) =>
            val m = if (isResponseMTI(mti)) MsgPair(other, e) else MsgPair(e, other)
            pending.remove(partnersKey)
            Some(m)
          case None =>
            val thisKey = key(mti, e)
            pending.put(thisKey, e)
            None
        }
      } else {
        None
      }
    }
  }

  private def key(mti: String, e: LogEntry): String = mti + "-" + toIntIfPossible(e("11"))   + "-" + e.realm.raw

  implicit class RichEntryIterable(val v: Iterator[LogEntry]) extends AnyVal {
    def pair(): Iterator[MsgPair] = MsgPair.pair(v)
  }

  implicit class RichMsgPairIterable(val v: Iterator[MsgPair]) extends AnyVal {
    def coalesce(selector: MsgPair => String): Iterator[Coalesced] = Collapser.coalesce(v, selector)
  }

  def toIntIfPossible(s: String): Any = {
    try {
      s.toInt
    }
    catch {
      case e: NumberFormatException => s
    }
  }
}

object MsgPairFieldAccess {

  val Request = """(req|request)\.(.*)""".r
  val Response = """(resp|response)\.(.*)""".r

  def unapply(expr: String): Option[(String, String)] = expr match {
    case Request(p, f) => Some((p, f))
    case Response(p, f) => Some((p, f))
    case _ => None
  }
}