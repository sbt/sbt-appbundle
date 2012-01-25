/*
 * Plugin.scala
 * (sbt-appbundle)
 *
 * Copyright (c) 2011-2012 Hanns Holger Rutz. All rights reserved.
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
import Project.{Initialize, Setting}
import java.io.{FileWriter, Writer, File}
import collection.breakOut

object AppBundlePlugin extends Plugin {
//   val appbundle        = TaskKey[ Unit ]( "appbundle" )
//   val appbundleName    = SettingKey[ String ]( "appbundle-name", "Name of the application bundle (minus .app extension)" )

   // What the *!&?
   private implicit def wrapTaskKey[ T ]( key: TaskKey[ T ]) : WrappedTaskKey[ T ] = WrappedTaskKey( key )
   private final case class WrappedTaskKey[ A ]( key: TaskKey[ A ]) {
      def orr[ T >: A ]( rhs: Initialize[ Task[ T ]]) : Initialize[ Task[ T ]] =
         (key.? zipWith rhs)( (x,y) => (x :^: y :^: KNil) map Scoped.hf2( _ getOrElse _ ))
   }

   private val jarExt = ".jar"

   object appbundle {
      val Config           = config( "appbundle" )
      val appbundle        = TaskKey[ Unit ]( "appbundle" )
      val executable       = SettingKey[ File ]( "executable", "Path to the java application stub executable" ) in Config
      val screenMenu       = SettingKey[ Boolean ]( "screenMenu", "Whether to display the menu bar in the screen top" ) in Config
      val quartz           = SettingKey[ Option[ Boolean ]]( "quartz", "Whether to use the Apple Quartz renderer (true) or the default Java renderer" ) in Config
      val systemProperties = SettingKey[ Seq[ (String, String) ]]( "systemProperties", "A key-value map passed as Java -D arguments (system properties)" ) in Config
      val javaVersion      = SettingKey[ String ]( "javaVersion", "Minimum Java version required to launch the application" ) in Config
      val mainClass        = TaskKey[ Option[ String ]]( "mainClass", "The main class entry point into the application" ) in Config
      val icon             = SettingKey[ Option[ File ]]( "icon", "Image file (.png or .icns) which is used as application icon" ) in Config
      // this needs to be a taskkey, because it is one in main scope, and we cannot reuse the key
      // while changing the type...
      val resources        = TaskKey[ Seq[ File ]]("resources", "Extra resource files to be copied to Contents/Resources.") in Config // Keys.resources in Config
      val workingDirectory = SettingKey[ File ]( "workingDirectorty", "Path corresponding to the application's current directory" ) in Config
      val organization     = Keys.organization in Config
      val normalizedName   = Keys.normalizedName in Config
      val name             = Keys.name in Config
      val version          = Keys.version in Config
      val fullClasspath    = Keys.fullClasspath in Config
      val javaOptions      = Keys.javaOptions in Config
      private val infos    = SettingKey[ InfoSettings ]( "_aux_info" )
      private val java     = TaskKey[ JavaSettings ]( "_aux_java" )
      private val bundle   = TaskKey[ BundleContents ]( "_aux_bundle" )

      val settings = Seq[ Setting[ _ ]](
         executable         := file( "/System/Library/Frameworks/JavaVM.framework/Versions/Current/Resources/MacOS/JavaApplicationStub" ),
         fullClasspath     <<= (Keys.fullClasspath in Compile) orr (Keys.fullClasspath in Runtime),
         mainClass         <<= mainClass orr (selectMainClass in Runtime),
         screenMenu         := true,
         quartz             := None,
         icon               := None,
         javaVersion        := "1.6+",
         javaOptions       <<= Keys.javaOptions in Runtime,
         resources          := Seq.empty,
         workingDirectory   := file( BundleVar_AppPackage ),
         systemProperties  <<= (javaOptions, screenMenu, quartz) { (seq, _screenMenu, _quartz) =>
            val m0: Map[ String, String ] = seq.collect({ case JavaDOption( key, value ) => (key, value) })( breakOut )
            val m1 = m0 + ("apple.laf.useScreenMenuBar" -> _screenMenu.toString)
            val m2 = _quartz match {
               case Some( value ) => m1 + ("apple.awt.graphics.UseQuartz" -> value.toString)
               case _ => m1
            }
//            m2
            m2.toSeq
         },
         infos             <<= (organization, normalizedName, name, version)( InfoSettings ),
         java              <<= (systemProperties, javaOptions, fullClasspath, packageBin in Compile,
                                mainClass, javaVersion, workingDirectory) map JavaSettings,
         bundle            <<= (executable, icon, resources) map BundleContents,
         appbundle         <<= (infos, java, bundle, streams) map appbundleTask
      )

      final case class InfoSettings( organization: String, normalizedName: String, name: String, version: String )

      final case class JavaSettings( systemProperties: Seq[ (String, String) ], javaOptions: Seq[ String ],
                                     classpath: Classpath, jarFile: File, mainClassOption: Option[ String ],
                                     javaVersion: String, workingDirectory: File )

      final case class BundleContents( executable: File, iconOption: Option[ File ], resources: Seq[ File ])

      val BundleVar_JavaRoot               = "$JAVAROOT"
      val BundleVar_AppPackage             = "$APP_PACKAGE"
      val BundleVar_UserHome               = "$USER_HOME"
   }

   private def appbundleTask( infos: appbundle.InfoSettings, java: appbundle.JavaSettings,
                              bundle: appbundle.BundleContents, streams: TaskStreams ) {
      import streams.log
      import infos._
      import java._
      import bundle._

      val mainClass              = mainClassOption.getOrElse( "Main class undefined" )

      val appBundleDir           = file( name + ".app" )
      log.info( "Bundling " + appBundleDir )

      val contentsDir            = appBundleDir / "Contents"
      val infoPListFile          = contentsDir / "Info.plist"
      val resourcesDir           = contentsDir / "Resources"
      val javaDir                = resourcesDir / "Java"
      val iconFile               = resourcesDir / "application.icns"
      val macOSDir               = contentsDir / "MacOS"
      val appStubFile            = macOSDir / "JavaApplicationStub"
      val pkgInfoFile            = contentsDir / "PkgInfo"

      val versionedNamePattern   = "(.*?)[-_]\\d.*\\.jar".r // thanks to Don Mackenzie
      val jarFilter              = ClasspathUtilities.isArchive _

      // ---- application stub ----
      if( !macOSDir.exists() ) macOSDir.mkdirs()
      if( !appStubFile.exists() ) {
         IO.copyFile( executable, appStubFile, false )
         appStubFile.setExecutable( true, false )
      }

      // ---- java resources ----
      if( !javaDir.exists() ) javaDir.mkdirs()

      val oldFiles = {
         val f = javaDir.listFiles()
         if( f != null ) f.toSeq.filter( jarFilter ) else Seq.empty[ File ]   // fucking NPE
      }
      oldFiles.foreach( f => log.verbose( "Removing " + f.getName ))
      IO.delete( oldFiles )

      val newFiles = classpath.map( _.data ).filter( jarFilter ) :+ jarFile

      val copyFiles = newFiles.flatMap { inPath =>
         val vName = inPath.getName
         if( !vName.contains( "-javadoc" ) && !vName.contains( "-sources" )) {
            val plainName = vName match {
               case versionedNamePattern( n ) if( n != "scala" ) => n + jarExt
               case n => n
            }
            val outPath = javaDir / plainName
//            log.verbose( "Copying to file " + outPath )
//            IO.copyFile( inPath, outPath, true )
            Some( (inPath, outPath) )
         } else None
      }

      copyFiles.foreach { case (inPath, outPath) =>
         log.verbose( "Copying to file " + outPath )
         IO.copyFile( inPath, outPath, true )
      }

      val outFiles = copyFiles.map( _._2 )

      // ---- other resources ----
      if( resources.nonEmpty ) {
         val copyResources = resources.map( from => (from, resourcesDir / from.name) )
         IO.copy( copyResources, preserveLastModified = true )
         copyResources.foreach {
            case (from, to) => if( from.canExecute ) to.setExecutable( true, false )
         }
      }

      // ---- icon ----
      iconOption.foreach { imageFile =>
         if( imageFile.ext == "icns" ) {
            IO.copyFile( imageFile, iconFile, true )
         } else {
            import sys.process._
            val lines         = Seq( "sips", "-g", "pixelHeight", "-g", "pixelWidth", imageFile.getPath ).lines
            val PixelWidth    = "\\s+pixelWidth: (\\d+)".r
            val PixelHeight   = "\\s+pixelHeight: (\\d+)".r
            val srcWidth      = lines.collect({ case PixelWidth(  s ) => s.toInt }).head
            val srcHeight     = lines.collect({ case PixelHeight( s ) => s.toInt }).head
            val supported     = IndexedSeq( 16, 32, 48, 128, 256, 512 )
            val srcSize       = math.min( 512, math.max( srcWidth, srcHeight ))
            val tgtSize       = supported( supported.indexWhere( _ >= srcSize ))
            val args0         = Seq( imageFile.getPath, "--out", iconFile.getPath )
            val args1         = if( tgtSize != srcWidth || tgtSize != srcHeight ) {
               Seq( "-z", tgtSize.toString, tgtSize.toString )
            } else {
               Seq.empty
            }
            val args          = Seq( "sips", "-s", "format", "icns" ) ++ args1 ++ args0
            args.!!
         }
      }

      // ---- info.plist ----
      val javaRootFile     = file( appbundle.BundleVar_JavaRoot )
      val bundleClassPath  = outFiles.map( javaRootFile / _.name )
      val vmOptions        = javaOptions.filterNot {
         case JavaDOption( _, _ ) => true // they have been already passed to Properties
         case _ => false
      }

      val jEntries: PListDictEntries = Map(
         JavaKey_MainClass          -> mainClass,
         JavaKey_Properties         -> systemProperties.toMap[ String, String ],
         JavaKey_ClassPath          -> PListValue.fromArray( bundleClassPath.map( _.toString )), // XXX why doesn't the implicit work?
         JavaKey_JVMVersion         -> javaVersion,
         JavaKey_VMOptions          -> PListValue.fromArray( vmOptions ), // XXX why doesn't the implicit work?
         JavaKey_WorkingDirectory   -> workingDirectory.getPath
      )

      val iterVersion   = version  // XXX TODO: append incremental build number
      val bundleID      = organization + "." + normalizedName

      val entries0 = Map[ String, PListValue ](
         CFBundleInfoDictionaryVersion    -> PListVersion,
         CFBundleIdentifier               -> bundleID,
         CFBundleName                     -> name,
         CFBundlePackageType              -> bundlePackageType,
         CFBundleExecutable               -> appStubFile.getName,
         CFBundleShortVersionString       -> version,
         CFBundleSignature                -> bundleSignature,
         CFBundleVersion                  -> iterVersion,
         CFBundleAllowMixedLocalizations  -> true.toString,
         BundleKey_Java                   -> jEntries
      )

      val entries: PListDictEntries = if( iconOption.isDefined ) {
         entries0 + (CFBundleIconFile -> iconFile.name)
      } else entries0

      val w = new FileWriter( infoPListFile )
      try {
         PList( entries ).write( w )
      } finally {
         w.close()
      }

      // ---- pkginfo ----
      if( !pkgInfoFile.exists() ) {
         val bytes = (bundlePackageType + bundleSignature).getBytes( "UTF-8" )  // 7-bit ascii anyway...
         require( bytes.length == 8 )
         IO.write( pkgInfoFile, bytes )
      }

      // ---- done ----
      log.info( "Done bundling." )

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

   private val CFBundleInfoDictionaryVersion    = "CFBundleInfoDictionaryVersion"
   private val CFBundleName                     = "CFBundleName"
   private val CFBundlePackageType              = "CFBundlePackageType"
   private val CFBundleAllowMixedLocalizations  = "CFBundleAllowMixedLocalizations"
   private val CFBundleExecutable               = "CFBundleExecutable"
   private val CFBundleIconFile                 = "CFBundleIconFile"
   private val CFBundleIdentifier               = "CFBundleIdentifier"
   private val CFBundleShortVersionString       = "CFBundleShortVersionString"
   private val CFBundleSignature                = "CFBundleSignature"
   private val CFBundleVersion                  = "CFBundleVersion"
   private val PListVersion                     = "6.0"
   private val BundlePackageTypeAPPL            = "APPL"
   private val BundleSignatureUnknown           = "????"

   private def bundlePackageType = BundlePackageTypeAPPL
   private def bundleSignature   = BundleSignatureUnknown

   private val BundleKey_Java                   = "Java"
   private val JavaKey_MainClass                = "MainClass"
   private val JavaKey_Properties               = "Properties"
   private val JavaKey_ClassPath                = "ClassPath"
   private val JavaKey_JVMVersion               = "JVMVersion"
   private val JavaKey_VMOptions                = "VMOptions"
   private val JavaKey_WorkingDirectory         = "WorkingDirectory"

   private lazy val JavaDOption  = "-D(.*?)=(.*?)".r

   private final case class PList( dict: PListDict ) {
      def toXML =
// <?xml version="1.0" encoding="UTF-8"?>
// <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
{dict.toXML}
</plist>

      def write( w: Writer ) {
         xml.XML.write( w, node = toXML, enc = "UTF-8", xmlDecl = true, doctype = PListDocType )
      }
   }

   private lazy val PListDocType = xml.dtd.DocType( "plist", xml.dtd.PublicID( "-//Apple//DTD PLIST 1.0//EN",
      "http://www.apple.com/DTDs/PropertyList-1.0.dtd"), Nil )

   private type PListDictEntries    = Map[ String, PListValue ]
   private type PListArrayEntries   = Seq[ PListValue ]
   private trait PListValueLow {
      implicit def fromArray[ A <% PListArray ]( a: A ) : PListValue = a: PListArray
   }
   private object PListValue extends PListValueLow {
      implicit def fromString( s: String ) : PListValue = PListString( s )
      implicit def fromDict[ A <% PListDict ]( a: A ) : PListValue = a: PListDict
   }
   private sealed trait PListValue {
      def toXML : xml.Node
   }
   private final case class PListString( value: String ) extends PListValue {
      def toXML = <string>{value}</string>
   }
   private object PListDict {
      implicit def fromValueMap( map: PListDictEntries ) : PListDict = PListDict( map )
      implicit def fromStringMap( map: Map[ String, String ]) : PListDict = PListDict( map.mapValues( PListString( _ )))
   }
   private final case class PListDict( map: PListDictEntries ) extends PListValue {
      def toXML = <dict>{map.map { case (key, value) => <key>{key}</key> ++ value.toXML }}</dict>
   }
   private object PListArray {
      implicit def fromValueSeq( seq: PListArrayEntries ) : PListArray = PListArray( seq )
      implicit def fromStringSeq( seq: Seq[ String ]) : PListArray = PListArray( seq.map( PListString( _ )))
   }
   private final case class PListArray( seq: PListArrayEntries ) extends PListValue {
      def toXML = <array>{seq.map( _.toXML )}</array>
   }
}