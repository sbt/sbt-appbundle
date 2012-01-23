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
      val Config        = config( "appbundle" )
      val appbundle     = TaskKey[ Unit ]( "appbundle" )
      val stub          = SettingKey[ File ]( "stub" ) in Config
      val name          = Keys.name in Config
      val fullClasspath = Keys.fullClasspath in Config

      val settings   = Seq[ Setting[ _ ]](
         stub := new File( "/System/Library/Frameworks/JavaVM.framework/Versions/Current/Resources/MacOS/JavaApplicationStub" ),
         fullClasspath <<= (Keys.fullClasspath in Compile) orr (Keys.fullClasspath in Runtime),
         appbundle <<= (name, stub, packageBin in Compile, fullClasspath, streams) map appbundleTask
      )
   }

   private def appbundleTask( name: String, stub: File, jarFile: File, classpath: Classpath, streams: TaskStreams ) {
      import streams.log

      val appBundleDir           = new File( name + ".app" )
      val contentsDir            = appBundleDir / "Contents"
      val infoPListFile          = contentsDir / "Info.plist"
      val resourcesDir           = contentsDir / "Resources"
      val javaDir                = resourcesDir / "Java"
      val macOSDir               = contentsDir / "MacOS"
      val appStubFile            = macOSDir / "JavaApplicationStub"
      val pkgInfoFile            = contentsDir / "PkgInfo"

      val versionedNamePattern   = "(.*?)[-_]\\d.*\\.jar".r // thanks to Don Mackenzie
      val jarFilter              = ClasspathUtilities.isArchive _

      // ---- application stub ----
      if( !macOSDir.exists() ) macOSDir.mkdirs()
      if( !appStubFile.exists() ) {
         IO.copyFile( stub, appStubFile, false )
         appStubFile.setExecutable( true, false )
      }

      // ---- pkginfo ----
      if( !pkgInfoFile.exists() ) {
         val bytes = (bundlePackageType + bundleSignature).getBytes( "UTF-8" )  // 7-bit ascii anyway...
         require( bytes.length == 8 )
         IO.write( pkgInfoFile, bytes )
      }

      // ---- java resources ----

      if( !javaDir.exists() ) javaDir.mkdirs()

      val oldFiles = {
         val f = javaDir.listFiles()
         if( f != null ) f.toSeq.filter( jarFilter ) else Seq.empty[ File ]   // fucking NPE
      }
      oldFiles.foreach( f => log.info( "Removing " + f.getName ))
      IO.delete( oldFiles )

      val newFiles = classpath.map( _.data ).filter( jarFilter ) :+ jarFile

      newFiles.foreach { inPath =>
         val vName = inPath.getName
         if( !vName.contains( "-javadoc" ) && !vName.contains( "-sources" )) {
            val plainName = vName match {
               case versionedNamePattern( n ) if( n != "scala" ) => n + jarExt
               case n => n
            }
            val outPath = new File( javaDir, plainName )
            log.info( "Copying to file " + outPath )
            IO.copyFile( inPath, outPath, true )
         }
      }

      // ---- info.plist ----

      val jEntries: PListDictEntries = Map(
         JavaKey_MainClass    -> "de.sciss.testapp.TestApp"    // XXX
      )

      val entries: PListDictEntries = Map(
         CFBundleInfoDictionaryVersion -> PListVersion,
         CFBundleIdentifier            -> "de.sciss.testapp",  // XXX
         CFBundleName                  -> name,
         CFBundlePackageType           -> bundlePackageType,
         CFBundleExecutable            -> appStubFile.getName,
         CFBundleShortVersionString    -> "1.2.3",  // XXX
         CFBundleSignature             -> "????",
         CFBundleVersion               -> "1.2.3b345",
         BundleKey_Java                -> jEntries
      )

      val w = new FileWriter( infoPListFile )
      try {
         PList( entries ).write( w )
      } finally {
         w.close()
      }

      // required java entries
      // MainClass
      //
      // recommended
      // JVMVersion

      // ClassPath (default: $APP_PACKAGE)
      // (+ architecture specific)

      // WorkingDirectory (default: $APP_PACKAGE)

      // Arguments (A string or array of strings)

      // Properties (a dict; aka java -D; e.g. key=apple.laf.useScreenMenuBar, value=true)

      // VMOptions (e.g. -Xms512m, -Xdock:icon=pathToIconFile)
      // (+ architecture specific)

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

      // eventually we could generate `Info.plist`, e.g. using `plutil -convert`

      // CFBundleAllowMixedLocalizations --> true
      // CFBundleDevelopmentRegion --> (Recommended), e.g. English
      // CFBundleDisplayName --> "If you do not intend to localize your bundle, do not include this key in your Info.plist file."
      // CFBundleDocumentTypes --> would be nice to support this eventually
      // CFBundleExecutable --> required I guess
      // CFAppleHelpAnchor
      // CFBundleIconFile --> Mac OS X; "The filename you specify does not need to include the extension, although it may.
      // The system looks for the icon file in the main resources directory of the bundle."
      // CFBundleIconFiles --> iOS
      // CFBundleIcons --> iOS
      // CFBundleURLTypes
   }

//   lazy val appbundleSettings: Seq[ Project.Setting[ _ ]] = Seq(
//      appbundle <<= (appbundleName in appbundle, packageBin in Compile,
//                     fullClasspath in appbundle, streams) map { (name, file, classpath, s) =>
//         appbundleTask( name, file, classpath, s.log )
//      },
//      fullClasspath in appbundle <<= fullClasspath orr (fullClasspath in Runtime)
//   )

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
   private val JavaKey_ClassPath                = "ClassPath"
   private val BundleVar_JavaRoot               = "$JAVAROOT"
   private val BundleVar_AppPackage             = "$APP_PACKAGE"
   private val BundleVar_UserHome               = "$USER_HOME"

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
   private object PListValue {
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
      implicit def fromMap( map: PListDictEntries ) : PListDict = PListDict( map )
   }
   private final case class PListDict( map: PListDictEntries ) extends PListValue {
      def toXML = <dict>{map.map { case (key, value) => <key>{key}</key> ++ value.toXML }}</dict>
   }
   private object PListArray {
      implicit def fromSeq( seq: PListArrayEntries ) : PListArray = PListArray( seq )
   }
   private final case class PListArray( seq: PListArrayEntries ) extends PListValue {
      def toXML = <array>{seq.map( _.toXML )}</array>
   }
}