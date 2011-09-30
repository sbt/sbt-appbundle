/*
 * Plugin.scala
 * (sbt-appbundle)
 *
 * Copyright (c) 2011 Hanns Holger Rutz. All rights reserved.
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

   private def appbundleTask( name: String, jarFile: File, classpath: Classpath, log: Logger ) {
      def appBundleName          = name + ".app"
      def appBundleContentsDir   = new File( appBundleName, "Contents" )
      def appBundleJavaDir       = new File( new File( appBundleContentsDir, "Resources" ), "Java" )
      val versionedNamePattern   = "(.*?)[-_]\\d.*\\.jar".r // thanks to Don Mackenzie
      val jarFilter              = ClasspathUtilities.isArchive _

      val oldFiles               = {
         val f = appBundleJavaDir.listFiles()
         if( f != null ) f.toSeq.filter( jarFilter ) else Seq.empty[ File ]   // fucking NPE
      }
      oldFiles.foreach( f => log.info( "Removing " + f.getName ))
      IO.delete( oldFiles )

      val newFiles               = classpath.map( _.data ).filter( jarFilter ) :+ jarFile

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
      appbundle <<= (appbundleName in appbundle, packageBin in Compile,
                     fullClasspath in appbundle, streams) map { (name, file, classpath, s) =>
         appbundleTask( name, file, classpath, s.log )
      },
      fullClasspath in appbundle <<= fullClasspath orr (fullClasspath in Runtime)
   )
}