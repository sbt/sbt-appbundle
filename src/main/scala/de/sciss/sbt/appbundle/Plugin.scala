package de.sciss.sbt.appbundle

import sbt._
import classpath.ClasspathUtilities
import Keys._
import Project.Initialize
import java.io.File

object Plugin extends sbt.Plugin {
   val appbundle        = TaskKey[ Unit ]( "appbundle" )
   val appbundleName    = SettingKey[ String ]( "appbundle-name" )

   // What the *!&?
   private implicit def wrapTaskKey[ T ]( key: TaskKey[ T ]) : WrappedTaskKey[ T ] = WrappedTaskKey( key )
   private final case class WrappedTaskKey[ A ]( key: TaskKey[ A ]) {
      def orr[ T >: A ]( rhs: Initialize[ Task[ T ]]) : Initialize[ Task[ T ]] =
         (key.? zipWith rhs)( (x,y) => (x :^: y :^: KNil) map Scoped.hf2( _ getOrElse _ ))
   }

   private val jarExt = ".jar"

   private def appbundleTask( name: String, classpath: Classpath, log: Logger ) {
      def appBundleName          = name + ".app"
      def appBundleContentsDir   = new File( appBundleName, "Contents" )
      def appBundleJavaDir       = new File( new File( appBundleContentsDir, "Resources" ), "Java" )
      val versionedNamePattern   = "(.*?)[-_]\\d.*\\.jar".r // thanks to Don Mackenzie

      val oldFiles               = appBundleJavaDir.listFiles().toSeq
      oldFiles.foreach( f => log.info( "Removing " + f.getName ))
      IO.delete( oldFiles )

      val newFiles               = classpath.map( _.data ).filter( ClasspathUtilities.isArchive )

      newFiles.foreach { inPath =>
         val vName = inPath.getName
         if( !vName.contains( "-javadoc" ) && !vName.contains( "-sources" )) {
            val plainName = vName match {
               case versionedNamePattern( n ) if( n != "scala" ) => n + jarExt
               case n => n
            }
            val outPath = new File( appBundleJavaDir, plainName )
            log.info( "Copying to file " + outPath )
            IO.copyFile( inPath, outPath, true )
         }
      }

      // eventually we could generate `Info.plist`, e.g. using `plutil -convert`
   }

   lazy val appbundleSettings: Seq[ Project.Setting[ _ ]] = Seq(
      appbundle <<= (appbundleName in appbundle, fullClasspath in appbundle, streams) map { (name, classpath, s) =>
         appbundleTask( name, classpath, s.log )
      },
      fullClasspath in appbundle <<= fullClasspath orr (fullClasspath in Runtime)
   )
}