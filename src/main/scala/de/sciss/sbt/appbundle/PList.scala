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
}

sealed trait PListValue {
  def toXML: xml.Node
}

case class PListBoolean(value: Boolean) extends PListValue {
  def toXML = if (value) <true/> else <false/>
}

case class PListString(value: String) extends PListValue {
  def toXML = <string>{value}</string>
}

case class PListDict(map: Map[String, PListValue]) extends PListValue {
  def toXML = <dict>{map.map {
    case (key, value) => <key>{key}</key> ++ value.toXML
  }}</dict>
}

case class PListArray(seq: Seq[PListValue]) extends PListValue {
  def toXML = <array>{seq.map(_.toXML)}</array>
}
