package de.sciss.sbt.appbundle

import sbt._
import sbt.Keys._

case class InfoSettings(organization: String, normalizedName: String, name: String, version: String)

case class JavaSettings(systemProperties: Seq[(String, String)], javaOptions: Seq[String],
                        classpath: Classpath, jarFile: File, mainClassOption: Option[String],
                        javaVersion: String, javaArchs: Seq[String], workingDirectory: Option[File])

case class BundleSettings(path: File, executable: File, iconOption: Option[File], resources: Seq[File],
                          signature: String, documents: Seq[Document], highResolution: Boolean)

case class Document(name: String, role: Document.Role = Document.Undefined,
                    rank: Document.Rank = Document.Undefined,
                    icon: Option[File] = scala.None, extensions: Seq[String] = Nil,
                    mimeTypes: Seq[String] = Nil, osTypes: Seq[String] = Nil,
                    isPackage: Boolean = false)

object Document {
  sealed trait Role {
    def valueOption: Option[String]
  }

  sealed trait Rank {
    def valueOption: Option[String]
  }

  case object Editor extends Role {
    val valueOption = Some("Editor")
  }

  case object Viewer extends Role {
    val valueOption = Some("Viewer")
  }

  case object Shell extends Role {
    val valueOption = Some("Shell")
  }

  case object Owner extends Rank {
    val valueOption = Some("Owner")
  }

  case object Alternate extends Rank {
    val valueOption = Some("Alternate")
  }

  case object Default extends Rank {
    val valueOption = Some("Default")
  }

  case object None extends Role with Rank {
    val valueOption = Some("None")
  }

  case object Undefined extends Role with Rank {
    val valueOption = Option.empty
  }
}
