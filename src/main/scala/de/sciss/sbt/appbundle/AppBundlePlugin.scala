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
import sbt.Scoped.RichTaskable9

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
      val stub             = SettingKey[ File ]( "stub", "Path to the java application stub executable" ) in Config
      val screenMenu       = SettingKey[ Boolean ]( "screenMenu", "Whether to display the menu bar in the screen top" ) in Config
      val quartz           = SettingKey[ Option[ Boolean ]]( "quartz", "Whether to use the Apple Quartz renderer (true) or the default Java renderer" ) in Config
      val systemProperties = SettingKey[ Map[ String, String ]]( "systemProperties", "A key-value map passed as Java -D arguments (system properties)" ) in Config
      val javaVersion      = SettingKey[ String ]( "javaVersion", "Minimum Java version required to launch the application" ) in Config
      val mainClass        = TaskKey[ Option[ String ]]( "mainClass", "The main class entry point into the application" ) in Config
      val organization     = Keys.organization in Config
      val normalizedName   = Keys.normalizedName in Config
      val name             = Keys.name in Config
      val version          = Keys.version in Config
      val fullClasspath    = Keys.fullClasspath in Config
      private val suckers  = SettingKey[ Helper ]( "_suckers" )

      val settings   = Seq[ Setting[ _ ]](
         stub             := file( "/System/Library/Frameworks/JavaVM.framework/Versions/Current/Resources/MacOS/JavaApplicationStub" ),
         fullClasspath   <<= (Keys.fullClasspath in Compile) orr (Keys.fullClasspath in Runtime),
         mainClass       <<= mainClass orr (selectMainClass in Runtime),
         screenMenu       := true,
         quartz           := None,
         javaVersion      := "1.6+",
         systemProperties <<= (Keys.javaOptions in Runtime, screenMenu, quartz) { (seq, _screenMenu, _quartz) =>
            val m0: Map[ String, String ] = seq.collect({ case JavaDOption( key, value ) => (key, value) })( breakOut )
            val m1 = m0 + ("apple.laf.useScreenMenuBar" -> _screenMenu.toString)
            val m2 = _quartz match {
               case Some( value ) => m1 + ("apple.awt.graphics.UseQuartz" -> value.toString)
               case _ => m1
            }
            m2
         },
         suckers <<= (organization, normalizedName, name, version, /* mainClass, */ javaVersion) apply Helper.apply,
//         appbundle <<= (organization, normalizedName, name, version, stub, systemProperties, /* javaVersion, */ /* mainClass, */
//                        packageBin in Compile, fullClasspath, streams) map appbundleTask
         appbundle <<= (suckers, mainClass, stub, systemProperties, packageBin in Compile, fullClasspath, streams) map appbundleTask
      )

      final case class Helper( organization: String, normalizedName: String, name: String, version: String,
                               /* mainClassOption: Option[ String ], */ javaVersion: String )
   }


   private def appbundleTask( suckers: appbundle.Helper, mainClassOption: Option[ String ], stub: File,
                              systemProperties: Map[ String, String ], jarFile: File,
                              classpath: Classpath, streams: TaskStreams ) {
      import streams.log
      import suckers._

      val mainClass              = mainClassOption.getOrElse( "Main class undefined" )

      val appBundleDir           = file( name + ".app" )
      log.info( "Bundling " + appBundleDir )

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

      // ---- info.plist ----

      val javaRootFile     = file( BundleVar_JavaRoot )
      val bundleClassPath  = outFiles.map( javaRootFile / _.name )

      val jEntries: PListDictEntries = Map(
         JavaKey_MainClass    -> mainClass,
         JavaKey_Properties   -> systemProperties,
         JavaKey_ClassPath    -> PListValue.fromArray( bundleClassPath.map( _.toString )), // XXX why doesn't the implicit work?
         JavaKey_JVMVersion   -> javaVersion
      )

      val iterVersion   = version  // XXX TODO: append incremental build number
      val bundleID      = organization + "." + normalizedName

      val entries: PListDictEntries = Map(
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

      val w = new FileWriter( infoPListFile )
      try {
         PList( entries ).write( w )
      } finally {
         w.close()
      }

      log.info( "Done bundling." )

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
   private val BundleVar_JavaRoot               = "$JAVAROOT"
   private val BundleVar_AppPackage             = "$APP_PACKAGE"
   private val BundleVar_UserHome               = "$USER_HOME"

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

   // Sssssssssssscukers

//   implicit private def t10ToTable10[A,B,C,D,E,F,G,H,I, J](t10: (ScopedTaskable[A], ScopedTaskable[B], ScopedTaskable[C],
//      ScopedTaskable[D], ScopedTaskable[E], ScopedTaskable[F], ScopedTaskable[G], ScopedTaskable[H],
//      ScopedTaskable[I], ScopedTaskable[J]) ): RichTaskable9[A,B,C,D,E,F,G,H,I] = new RichTaskable10(t10)
//
//   private final class RichTaskable10[A,B,C,D,E,F,G,H,I,J](t10: (ScopedTaskable[A], ScopedTaskable[B], ScopedTaskable[C],
//      ScopedTaskable[D], ScopedTaskable[E], ScopedTaskable[F], ScopedTaskable[G], ScopedTaskable[H],
//      ScopedTaskable[I], ScopedTaskable[J])) extends RichTaskables(k10(t10))
//  	{
//  		type Fun[M[_],Ret] = (M[A],M[B],M[C],M[D],M[E],M[F],M[G],M[H],M[I]) => Ret
//  		def identityMap = map(mkTuple9)
//  		protected def convertH[R](z: Fun[Id,R]) = hf9(z)
//  		protected def convertK[M[_],R](z: Fun[M,R]) = { case a :^: b :^: c :^: d :^: e :^: f :^: g :^: h :^: i :^: KNil => z(a,b,c,d,e,f,g,h,i) }
//  	}
}