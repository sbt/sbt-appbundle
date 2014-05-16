/*
 * Plugin.scala
 * (sbt-appbundle)
 *
 * Copyright (c) 2011-2013 Hanns Holger Rutz. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 *
 * For further information, please contact Hanns Holger Rutz at
 * contact@sciss.de
 */

package de.sciss.sbt.appbundle

import sbt._
import classpath.ClasspathUtilities
import Keys._
import java.io.{FileWriter, Writer, File}
import collection.breakOut
import language.implicitConversions

object AppBundlePlugin extends Plugin {
  //  // What the *!&?
  //  private implicit def wrapTaskKey[T](key: TaskKey[T]): WrappedTaskKey[T] = WrappedTaskKey(key)
  //  private final case class WrappedTaskKey[A](key: TaskKey[A]) {
  //    def orr[T >: A](rhs: Initialize[Task[T]]): Initialize[Task[T]] =
  //      (key.? zipWith rhs)((x, y) => (x :^: y :^: KNil) map Scoped.hf2(_ getOrElse _))
  //  }

  private val jarExt = ".jar"

  object appbundle {
    val BundleVar_JavaRoot    = "$JAVAROOT"
    val BundleVar_AppPackage  = "$APP_PACKAGE"
    val BundleVar_UserHome    = "$USER_HOME"

    val JavaArch_i386         = "i386"
    val JavaArch_x86_64       = "x86_64"
    val JavaArch_ppc          = "ppc"

    val Signature_Unknown     = "????"

    val Config            = config("appbundle")

    val appbundle         = TaskKey[Unit]("appbundle")
    val executable        = SettingKey[File]("executable", "Path to the java application stub executable") in Config
    val screenMenu        = SettingKey[Boolean]("screenMenu", "Whether to display the menu bar in the screen top") in Config
    val quartz            = SettingKey[Option[Boolean]]("quartz", "Whether to use the Apple Quartz renderer (true) or the default Java renderer") in Config
    val highResolution    = SettingKey[Boolean]("highResolution", "Whether the app supports high resolution displays")
    val systemProperties  = TaskKey[Seq[(String, String)]]("systemProperties", "A key-value map passed as Java -D arguments (system properties)") in Config
    val javaVersion       = SettingKey[String]("javaVersion", "Minimum Java version required to launch the application") in Config
    val javaArchs         = SettingKey[Seq[String]]("javaArchs", "Entries for the JVMArchs entry, specifying supported processor architectures in order of their preference") in Config
    val mainClass         = TaskKey[Option[String]]("mainClass", "The main class entry point into the application") in Config
    val icon              = SettingKey[Option[File]]("icon", "Image file (.png or .icns) which is used as application icon") in Config
    // this needs to be a taskkey, because it is one in main scope, and we cannot reuse the key
    // while changing the type...
    val resources         = TaskKey[Seq[File]]("resources", "Extra resource files to be copied to Contents/Resources.") in Config
    // Keys.resources in Config
    val workingDirectory  = SettingKey[Option[File]]("workingDirectorty", "Path corresponding to the application's current directory") in Config
    val organization      = Keys.organization in Config
    val normalizedName    = Keys.normalizedName in Config
    val name              = Keys.name in Config
    val target            = Keys.target in Config
    val outputPath        = SettingKey[File]("outputPath", "Target appbundle (.app) directory") in Config
    val version           = Keys.version in Config
    val fullClasspath     = Keys.fullClasspath in Config
    val javaOptions       = Keys.javaOptions in Config
    val signature         = SettingKey[String]("signature", "The four characters identifying the creator of the bundle (formerly Creator Code)") in Config
    val documents         = SettingKey[Seq[Document]]("documents", "A list of document types which the application supports") in Config
    private val infos     = SettingKey[InfoSettings]("_aux_info")
    private val java      = TaskKey[JavaSettings]("_aux_java")
    private val bundle    = TaskKey[BundleSettings]("_aux_bundle")

    val settings = Seq[Def.Setting[_]](
      executable        := file("/System/Library/Frameworks/JavaVM.framework/Versions/Current/Resources/MacOS/JavaApplicationStub"),
      fullClasspath    <<= Keys.fullClasspath or (Keys.fullClasspath in Runtime), // (Keys.fullClasspath in Compile) orr (Keys.fullClasspath in Runtime),
      mainClass        <<= mainClass or (mainClass in Runtime), // mainClass orr (selectMainClass in Runtime),
      screenMenu        := true,
      quartz            := None,
      highResolution    := true,
      icon              := None,
      javaVersion       := "1.6+",
      javaArchs         := Seq.empty,
      javaOptions      <<= Keys.javaOptions in Runtime,
      resources         := Seq.empty,
      signature         := Signature_Unknown,
      documents         := Seq.empty,
      workingDirectory  := None, // file( BundleVar_AppPackage ),
      systemProperties <<= (javaOptions, screenMenu, quartz) map {
        (seq, _screenMenu, _quartz) =>
          val m0: Map[String, String] = seq.collect({
            case JavaDOption(key, value) => (key, value)
          })(breakOut)
          val m1 = m0 + ("apple.laf.useScreenMenuBar" -> _screenMenu.toString)
          val m2 = _quartz match {
            case Some(value) => m1 + ("apple.awt.graphics.UseQuartz" -> value.toString)
            case _           => m1
          }
          m2.toSeq
      },
      outputPath <<= (target, name) {
        (t, n) => t / (n + ".app")
      },
      infos            <<= (organization, normalizedName, name, version)(InfoSettings),
      java             <<= (systemProperties, javaOptions, fullClasspath, packageBin in Compile,
        mainClass, javaVersion, javaArchs, workingDirectory) map JavaSettings,
      bundle           <<= (outputPath, executable, icon, resources, signature, documents, highResolution) map BundleSettings,
      appbundle        <<= (infos, java, bundle, streams) map appbundleTask
    )

    final case class InfoSettings(organization: String, normalizedName: String, name: String, version: String)

    final case class JavaSettings(systemProperties: Seq[(String, String)], javaOptions: Seq[String],
                                  classpath: Classpath, jarFile: File, mainClassOption: Option[String],
                                  javaVersion: String, javaArchs: Seq[String], workingDirectory: Option[File])

    final case class BundleSettings(path: File, executable: File, iconOption: Option[File], resources: Seq[File],
                                    signature: String, documents: Seq[Document], highResolution: Boolean)

    // TODO: LSItemContentTypes, LSTypeIsPackage
    object Document {
      sealed trait Role { def valueOption: Option[ String ]}
      sealed trait Rank { def valueOption: Option[ String ]}

      case object Editor extends Role { val valueOption = Some("Editor")}
      case object Viewer extends Role { val valueOption = Some("Viewer")}
      case object Shell  extends Role { val valueOption = Some("Shell" )}

      case object Owner     extends Rank { val valueOption = Some("Owner"    )}
      case object Alternate extends Rank { val valueOption = Some("Alternate")}
      case object Default   extends Rank { val valueOption = Some("Default"  )}

      case object None      extends Role with Rank { val valueOption = Some( "None" )}
      case object Undefined extends Role with Rank { val valueOption = Option.empty }
    }
    final case class Document(name: String, role: Document.Role = Document.Undefined,
                              rank: Document.Rank = Document.Undefined,
                              icon: Option[File] = scala.None, extensions: Seq[String] = Nil,
                              mimeTypes: Seq[String] = Nil, osTypes: Seq[String] = Nil,
                              isPackage: Boolean = false)
  }

  private final case class PList(dict: PListDict) {
    def toXML =
// <?xml version="1.0" encoding="UTF-8"?>
// <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
{dict.toXML}
</plist>

    def write(w: Writer) {
      xml.XML.write(w, node = toXML, enc = "UTF-8", xmlDecl = true, doctype = PListDocType)
    }
  }

  private lazy val PListDocType = xml.dtd.DocType("plist", xml.dtd.PublicID("-//Apple//DTD PLIST 1.0//EN",
    "http://www.apple.com/DTDs/PropertyList-1.0.dtd"), Nil)

  private type PListDictEntries   = Map[String, PListValue]
  private type PListArrayEntries  = Seq[PListValue]

  private trait PListValueLow {
    // implicit def fromArray[ A <% PListArray ]( a: A ) : PListValue = a: PListArray
    implicit def fromValueSeq (seq: PListArrayEntries): PListArray = PListArray(seq)
    implicit def fromStringSeq(seq: Seq[String])      : PListArray = PListArray(seq.map(PListString))
  }

  private object PListValue extends PListValueLow {
    implicit def fromString(s: String): PListValue = PListString(s)
    // implicit def fromDict[ A <% PListDict ]( a: A ) : PListValue = a: PListDict
    implicit def fromValueMap (map: PListDictEntries)   : PListDict = PListDict(map)
    implicit def fromStringMap(map: Map[String, String]): PListDict = PListDict(map.mapValues(PListString))
  }

  private sealed trait PListValue {
    def toXML: xml.Node
  }

  private final case class PListString(value: String) extends PListValue {
    def toXML = <string>{value}</string>
  }

  //   private object PListDict {
  //      implicit def fromValueMap( map: PListDictEntries ) : PListDict = PListDict( map )
  //      implicit def fromStringMap( map: Map[ String, String ]) : PListDict = PListDict( map.mapValues( PListString( _ )))
  //   }
  private final case class PListDict(map: PListDictEntries) extends PListValue {
    def toXML = <dict>{map.map {
        case (key, value) => <key>{key}</key> ++ value.toXML
      }}</dict>
  }

  //   private object PListArray {
  //      implicit def fromValueSeq( seq: PListArrayEntries ) : PListArray = PListArray( seq )
  //      implicit def fromStringSeq( seq: Seq[ String ]) : PListArray = PListArray( seq.map( PListString( _ )))
  //   }
  private final case class PListArray(seq: PListArrayEntries) extends PListValue {
    def toXML = <array>{seq.map(_.toXML)}</array>
  }

  private def appbundleTask(infos: appbundle.InfoSettings, java: appbundle.JavaSettings,
                            bundle: appbundle.BundleSettings, streams: TaskStreams) {
    import streams.log
    import infos._
    import java._
    import bundle._

    val mainClass = mainClassOption.getOrElse("Main class undefined")

    log.info( "Bundling " + path )

    val contentsDir   = path         / "Contents"
    val infoPListFile = contentsDir  / "Info.plist"
    val resourcesDir  = contentsDir  / "Resources"
    val javaDir       = resourcesDir / "Java"
    val macOSDir      = contentsDir  / "MacOS"
    val appStubFile   = macOSDir     / "JavaApplicationStub"
    val pkgInfoFile   = contentsDir  / "PkgInfo"

    val versionedNamePattern = "(.*?)[-_]\\d.*\\.jar".r // thanks to Don Mackenzie
    val jarFilter = ClasspathUtilities.isArchive(_: File)

    // ---- application stub ----
    if (!macOSDir.exists()) macOSDir.mkdirs()
    if (!appStubFile.exists()) {
      IO.copyFile(executable, appStubFile, preserveLastModified = false)
      appStubFile.setExecutable(true, false)
    }

    // ---- java resources ----
    if (!javaDir.exists()) javaDir.mkdirs()

    val oldFiles = {
      val f = javaDir.listFiles()
      if (f != null) f.toSeq.filter(jarFilter) else Seq.empty[File] // fucking NPE
    }
    oldFiles.foreach(f => log.verbose("Removing " + f.getName))
    IO.delete(oldFiles)

    val newFiles = classpath.map(_.data).filter(jarFilter) :+ jarFile

    val copyFiles = newFiles.flatMap { inPath =>
      val vName = inPath.getName
      if (!vName.contains("-javadoc") && !vName.contains("-sources")) {
        val plainName = vName match {
          case versionedNamePattern(n) if n != "scala" => n + jarExt
          case n => n
        }
        val outPath = javaDir / plainName
        //            log.verbose( "Copying to file " + outPath )
        //            IO.copyFile( inPath, outPath, true )
        Some((inPath, outPath))
      } else None
    }

    copyFiles.foreach { case (inPath, outPath) =>
      log.verbose("Copying to file " + outPath)
      IO.copyFile(inPath, outPath, preserveLastModified = true)
    }

    val outFiles = copyFiles.map(_._2)

    // ---- other resources ----
    if (resources.nonEmpty) {
      //         val copyResources = resources.map( from => (from, resourcesDir / from.name) )
      //         IO.copy( copyResources, preserveLastModified = true )
      //         copyResources.foreach {
      //            case (from, to) => if( from.canExecute ) to.setExecutable( true, false )
      //         }

      def checkExecutable(from: File, to: File) {
        if (from.canExecute) to.setExecutable(true, false)
        if (from.isDirectory) {
          from.listFiles().foreach { sub =>
            checkExecutable(sub, to / sub.name)
          }
        }
      }

      resources.foreach { from =>
        val to = resourcesDir / from.name
        if (from.isFile) IO.copyFile(from, to, preserveLastModified = true)
        else if (from.isDirectory) IO.copyDirectory(from, to, overwrite = true, preserveLastModified = true)
        checkExecutable(from, to)
      }
    }

    // ---- icon ----
    def makeIcon(inF: File, outF: File) {
      if (inF.ext == "icns") {
        IO.copyFile(inF, outF, preserveLastModified = true)
      } else {
        import sys.process._
        val lines       = Seq("sips", "-g", "pixelHeight", "-g", "pixelWidth", inF.getPath).lines
        val PixelWidth  = "\\s+pixelWidth: (\\d+)".r
        val PixelHeight = "\\s+pixelHeight: (\\d+)".r
        val srcWidth    = lines.collect { case PixelWidth (s) => s.toInt } .head
        val srcHeight   = lines.collect { case PixelHeight(s) => s.toInt } .head
        val supported   = IndexedSeq(16, 32, 48, 128, 256, 512)
        val srcSize     = math.min(512, math.max(srcWidth, srcHeight))
        val tgtSize     = supported(supported.indexWhere(_ >= srcSize))
        val args0       = Seq(inF.getPath, "--out", outF.getPath)
        val args1       = if (tgtSize != srcWidth || tgtSize != srcHeight) {
          Seq("-z", tgtSize.toString, tgtSize.toString)
        } else {
          Seq.empty
        }
        val args        = Seq("sips", "-s", "format", "icns") ++ args1 ++ args0
        args.!!
      }
    }

    // ---- info.plist ----
    val javaRootFile    = file(appbundle.BundleVar_JavaRoot)
    val bundleClassPath = outFiles.map(javaRootFile / _.name)
    val vmOptions       = javaOptions.filterNot {
      case JavaDOption(_, _)  => true // they have been already passed to Properties
      case _                  => false
    }

    val jEntries0 = Map[String, PListValue](
      JavaKey_MainClass  -> mainClass,
      JavaKey_Properties -> systemProperties.toMap[String, String],
      JavaKey_ClassPath  -> bundleClassPath.map(_.toString),
      JavaKey_JVMVersion -> javaVersion,
      JavaKey_VMOptions  -> vmOptions
    )

    val jEntries1: Map[String, PListValue] = if (javaArchs.nonEmpty) {
      jEntries0 + (JavaKey_JVMArchs -> javaArchs)
    } else jEntries0

    val jEntries: PListDictEntries = workingDirectory match {
      case Some(value) => jEntries1 + (JavaKey_WorkingDirectory -> value.getPath)
      case _ => jEntries1
    }

    val iterVersion = version // XXX TODO: append incremental build number
    val bundleID    = organization + "." + normalizedName

    var entries: PListDictEntries = Map(
      CFBundleInfoDictionaryVersion   -> PListVersion,
      CFBundleIdentifier              -> bundleID,
      CFBundleName                    -> name,
      CFBundlePackageType             -> bundlePackageType,
      CFBundleExecutable              -> appStubFile.getName,
      CFBundleShortVersionString      -> version,
      CFBundleSignature               -> signature,
      CFBundleVersion                 -> iterVersion,
      CFBundleAllowMixedLocalizations -> true.toString,
      NSHighResolutionCapable         -> highResolution.toString,
      BundleKey_Java                  -> jEntries
    )

    iconOption.foreach { imageFile =>
      val iconFile = resourcesDir / "application.icns"
      makeIcon(imageFile, iconFile)
      entries += (CFBundleIconFile -> iconFile.name)
    }

    if (documents.nonEmpty) entries += (CFBundleDocumentTypes -> PListArray(
      documents.map { doc =>
        var d: PListDictEntries = Map(CFBundleTypeName -> doc.name)
        doc.role.valueOption.foreach { roleValue =>
          d += CFBundleTypeRole -> roleValue
        }
        doc.rank.valueOption.foreach { rankValue =>
          d += LSHandlerRank -> rankValue
        }
        if (doc.extensions.nonEmpty) {
          d += CFBundleTypeExtensions -> PListArray(doc.extensions.map(ext => ext: PListValue))
        }
        doc.icon.foreach { imageFile =>
          val iconFile = resourcesDir / ("doc_" + imageFile.base + ".icns")
          makeIcon(imageFile, iconFile)
          d += (CFBundleTypeIconFile -> iconFile.name)
        }
        if (doc.mimeTypes.nonEmpty) {
          d += CFBundleTypeMIMETypes -> PListArray(doc.mimeTypes.map(tpe => tpe: PListValue))
        }
        if (doc.osTypes.nonEmpty) {
          d += CFBundleTypeOSTypes -> PListArray(doc.osTypes.map { tpe =>
            require(tpe.length == 4, "OS Types must be composed of exactly four characters (failed: " + tpe + ")")
            tpe: PListValue
          })
        }
        if (doc.isPackage) {
          d += LSTypeIsPackage -> true.toString
        }
        PListDict(d)
      }
    ))

    //println( "ENTRIES = " + entries )

    val w = new FileWriter(infoPListFile)
    try {
      PList(entries).write(w)
    } finally {
      w.close()
    }

    // ---- pkginfo ----
    if (!pkgInfoFile.exists()) {
      val bytes = (bundlePackageType + signature).getBytes("UTF-8") // 7-bit ascii anyway...
      require(bytes.length == 8)
      IO.write(pkgInfoFile, bytes)
    }

    // ---- done ----
    log.info("Done bundling.")

    // WorkingDirectory (default: $APP_PACKAGE)

    // Arguments (A string or array of strings)

    // Variables: $JAVAROOT, $APP_PACKAGE, $USER_HOME

    // useful existing keys: description, homepage, javaOptions, javacOptions, licenses
    // mainClass, normalizedName, moduleName (= normalizedName???), organization
    // organizationHomepage, organizationName, projectID (= moduleID:version???)

    // ModuleInfo (nameFormal: String, description: String, homepage: Option[URL],
    // startYear: Option[Int], licenses: Seq[(String, URL)], organizationName: String, organizationHomepage: Option[URL])

    // selectMainClass
    // startYear
    // timingFormat
    // version

    // CFBundleDevelopmentRegion --> (Recommended), e.g. English
    // CFBundleDisplayName --> "If you do not intend to localize your bundle, do not include this key in your Info.plist file."
    // CFBundleDocumentTypes --> would be nice to support this eventually
    // CFAppleHelpAnchor
    // CFBundleIconFile --> Mac OS X; "The filename you specify does not need to include the extension, although it may.
    // The system looks for the icon file in the main resources directory of the bundle."
    // CFBundleIconFiles --> iOS
    // CFBundleIcons --> iOS
    // CFBundleURLTypes
  }

  // apple.laf.useScreenMenuBar (default false)
  // apple.awt.brushMetalLook (default false)
  // apple.awt.fileDialogForDirectories (default false)
  // apple.awt.UIElement (default false)

  // apple.awt.fakefullscreen (default false)
  // apple.awt.fullscreencapturealldisplays (false)
  // apple.awt.fullscreenhidecursor (default true)
  // apple.awt.fullscreenusefade (default false)

  // apple.awt.antialiasing (default on? for aqua)
  // apple.awt.textantialiasing (default on? for aqua)
  // apple.awt.rendering = speed | quality
  // apple.awt.interpolation
  // apple.awt.fractionalmetrics

  // apple.awt.graphics.OptimizeShapes
  // apple.awt.graphics.EnableLazyDrawing
  // apple.awt.graphics.EnableLazyDrawingQueueSize
  // apple.awt.graphics.EnableQ2DX
  // apple.awt.graphics.EnableDeferredUpdates

  // apple.awt.graphics.UseQuartz (default true for Java 1.5, false for Java 1.6)

  // apple.awt.graphics.EnableLazyPixelConversion

  private val CFBundleInfoDictionaryVersion   = "CFBundleInfoDictionaryVersion"
  private val CFBundleName                    = "CFBundleName"
  private val CFBundlePackageType             = "CFBundlePackageType"
  private val CFBundleAllowMixedLocalizations = "CFBundleAllowMixedLocalizations"
  private val CFBundleExecutable              = "CFBundleExecutable"
  private val CFBundleIconFile                = "CFBundleIconFile"
  private val CFBundleIdentifier              = "CFBundleIdentifier"
  private val CFBundleShortVersionString      = "CFBundleShortVersionString"
  private val CFBundleSignature               = "CFBundleSignature"
  private val CFBundleVersion                 = "CFBundleVersion"
  private val CFBundleDocumentTypes           = "CFBundleDocumentTypes"
  private val CFBundleTypeExtensions          = "CFBundleTypeExtensions"
  private val CFBundleTypeIconFile            = "CFBundleTypeIconFile"
  private val CFBundleTypeMIMETypes           = "CFBundleTypeMIMETypes"
  private val CFBundleTypeName                = "CFBundleTypeName"
  private val CFBundleTypeOSTypes             = "CFBundleTypeOSTypes"
  private val CFBundleTypeRole                = "CFBundleTypeRole"
  private val LSItemContentTypes              = "LSItemContentTypes"
  private val LSHandlerRank                   = "LSHandlerRank"
  private val LSTypeIsPackage                 = "LSTypeIsPackage"
  private val NSHighResolutionCapable         = "NSHighResolutionCapable"
  private val PListVersion                    = "6.0"
  private val BundlePackageTypeAPPL           = "APPL"

  private def bundlePackageType               = BundlePackageTypeAPPL

  private val BundleKey_Java                  = "Java"
  private val JavaKey_MainClass               = "MainClass"
  private val JavaKey_Properties              = "Properties"
  private val JavaKey_ClassPath               = "ClassPath"
  private val JavaKey_JVMVersion              = "JVMVersion"
  private val JavaKey_VMOptions               = "VMOptions"
  private val JavaKey_WorkingDirectory        = "WorkingDirectory"
  private val JavaKey_JVMArchs                = "JVMArchs"

  //   sealed trait HandlerRank { def value: String }
  //   case object HandlerRank_Owner     extends HandlerRank { val value = "Owner"     }
  //   case object HandlerRank_Alternate extends HandlerRank { val value = "Alternate" }
  //   case object HandlerRank_None      extends HandlerRank { val value = "None"      }
  //   case object HandlerRank_Default   extends HandlerRank { val value = "Default"   }

  private val JavaDOption = "-D(.*?)=(.*?)".r
}