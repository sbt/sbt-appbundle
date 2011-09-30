package de.sciss.sbt.appbundle

import sbt._
import classpath.ClasspathUtilities
import Keys._
import Project.Initialize

object Plugin extends sbt.Plugin {
   val appbundle        = TaskKey[ Unit ]( "appbundle" )
   val appbundleName    = SettingKey[ String ]( "appbundle-name" )

   // What the *!&?
   private implicit def wrapTaskKey[ T ]( key: TaskKey[ T ]) : WrappedTaskKey[ T ] = WrappedTaskKey( key )
   private final case class WrappedTaskKey[ A ]( key: TaskKey[ A ]) {
      def orr[ T >: A ]( rhs: Initialize[ Task[ T ]]) : Initialize[ Task[ T ]] =
         (key.? zipWith rhs)( (x,y) => (x :^: y :^: KNil) map Scoped.hf2( _ getOrElse _ ))
   }

   private def appbundleTask( name: String, classpath: Classpath, log: Logger ) {
      val jars = classpath.map( _.data ).filter( ClasspathUtilities.isArchive )
      jars.foreach { jar =>
         log.info( "Found : " + jar )
      }
   }

   lazy val appbundleSettings: Seq[ Project.Setting[ _ ]] = Seq(
      appbundle <<= (appbundleName in appbundle, fullClasspath in appbundle, streams) map { (name, classpath, s) =>
         appbundleTask( name, classpath, s.log )
      },
      fullClasspath in appbundle <<= fullClasspath orr (fullClasspath in Runtime)
   )

//   lazy val appbundleSettings: Seq[ Project.Setting[ _ ]] = baseAppbundleSettings
}