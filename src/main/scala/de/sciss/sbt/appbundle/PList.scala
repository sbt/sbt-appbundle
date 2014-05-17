package de.sciss.sbt.appbundle

import java.io.Writer
import scala.language.implicitConversions

case class PList(dict: PListDict) {
  def toXML = <plist version="1.0">{dict.toXML}</plist>

  def write(w: Writer): Unit =
    xml.XML.write(w, node = toXML, enc = "UTF-8", xmlDecl = true, doctype = PList.docType)
}

object PList {
  lazy val docType = xml.dtd.DocType("plist", xml.dtd.PublicID("-//Apple//DTD PLIST 1.0//EN",
    "http://www.apple.com/DTDs/PropertyList-1.0.dtd"), Nil)

  type PListArrayEntries = Seq[PListValue]
  type PListDictEntries = Map[String, PListValue]

  implicit def stringToPListString(value: String): PListString = PListString(value)
  implicit def booleanToPListBoolean(value: Boolean): PListBoolean = PListBoolean(value)
  implicit def stringMapToPListDict(map: Map[String, String]): PListDict = PListDict(map.mapValues(PListString))
  implicit def valueMapToPListDict(map: Map[String, PListValue]): PListDict = PListDict(map)
  implicit def stringSeqToPListArray(seq: Seq[String]): PListArray = PListArray(seq.map(PListString))
  implicit def valueSeqToPListArray(seq: Seq[PListValue]): PListArray = PListArray(seq)

  // helper functions that either return a non none PListValue or PListNull
  def arrayOrNull(seq: Seq[String]): PListValue =
    if (seq.nonEmpty) PListArray(seq.map(PListString)) else PListNull
  def arrayOrNull[T](seq: Seq[T], conv: T => String): PListValue =
    if (seq.nonEmpty) PListArray(seq.map(i => PListString(conv(i)))) else PListNull

  // helper functions that either return a non empty PListArray or PListNull
  def valueOrNull(value: Option[String]): PListValue =
    if (value.isDefined) PListString(value.get) else PListNull
  def valueOrNull[T](value: Option[T], conv: T => String): PListValue =
    if (value.isDefined) PListString(conv(value.get)) else PListNull
}

sealed trait PListValue {
  def toXML: xml.Node
}

/** Entries with value PListNull are ommitted. */
case object PListNull extends PListValue {
  def toXML = ???
}

case class PListBoolean(value: Boolean) extends PListValue {
  def toXML = if (value) <true/> else <false/>
}

case class PListString(value: String) extends PListValue {
  def toXML = <string>{value}</string>
}

case class PListDict(map: Map[String, PListValue]) extends PListValue {
  def toXML = <dict>{map.filter(_._2 != PListNull).map {
    case (key, value) => <key>{key}</key> ++ value.toXML
  }}</dict>
}

case class PListArray(seq: Seq[PListValue]) extends PListValue {
  def toXML = <array>{seq.filter(_ != PListNull).map(_.toXML)}</array>
}
