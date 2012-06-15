sbtPlugin := true

name := "sbt-appbundle"

organization := "de.sciss"

version := "0.14"

scalaVersion := "2.9.2"

crossScalaVersions := Seq( "2.9.2", "2.9.1" )

scalacOptions := Seq( "-deprecation", "-unchecked" )

description := "An sbt plugin to create OS X application bundles"

homepage := Some( url( "https://github.com/Sciss/sbt-appbundle" ))

licenses := Seq( "LGPL v2.1+" -> url( "http://www.gnu.org/licenses/lgpl-2.1.txt" ))

// ---- publishing ----

publishMavenStyle := true

publishTo <<= version { (v: String) =>
   Some( if( v.endsWith( "-SNAPSHOT" ))
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
   else
      "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
   )
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra :=
<scm>
  <url>git@github.com:Sciss/sbt-appbundle.git</url>
  <connection>scm:git:git@github.com:Sciss/sbt-appbundle.git</connection>
</scm>
<developers>
   <developer>
      <id>sciss</id>
      <name>Hanns Holger Rutz</name>
      <url>http://www.sciss.de</url>
   </developer>
</developers>

// // the following is valid for scalasbt.artifactoryonline.com
//
// publishTo <<= version { (v: String) =>
//    Some( if( v.endsWith( "-SNAPSHOT")) {
//       Resolver.url( "sbt-plugin-snapshots",
//          url( "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-snapshots/" )
//       )( Resolver.ivyStylePatterns )
//    } else {
//       Resolver.url( "sbt-plugin-releases",
//          url( "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/" )
//       )( Resolver.ivyStylePatterns )
//    })
// }
//
// publishMavenStyle := false
//
// credentials += Credentials( Path.userHome / ".ivy2" / ".sbtcredentials" )
//
// pomExtra :=
// <licenses>
//   <license>
//     <name>LGPL v2.1+</name>
//     <url>http://www.gnu.org/licenses/lgpl-2.1.txt</url>
//     <distribution>repo</distribution>
//   </license>
// </licenses>

// publishArtifact in (Compile, packageDoc) := false

// publishArtifact in (Compile, packageSrc) := false

// ---- ls.implicit.ly ----

seq( lsSettings :_* )

(LsKeys.tags in LsKeys.lsync) := Seq( "sbt", "plugin", "application-bundle", "os-x" )

(LsKeys.ghUser in LsKeys.lsync) := Some( "Sciss" )

(LsKeys.ghRepo in LsKeys.lsync) := Some( "sbt-appbundle" )

// bug in ls -- doesn't find the licenses from global scope
(licenses in LsKeys.lsync) := Seq( "LGPL v2.1+" -> url( "http://www.gnu.org/licenses/lgpl-2.1.txt" ))
