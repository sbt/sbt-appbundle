sbtPlugin := true

name := "sbt-appbundle"

organization := "de.sciss"

version := "0.13"

scalacOptions := Seq( "-deprecation", "-unchecked" )

description := "An sbt plugin to create OS X application bundles"

homepage := Some( url( "https://github.com/Sciss/sbt-appbundle" ))

licenses := Seq( "LGPL v2.1+" -> url( "http://www.gnu.org/licenses/lgpl-2.1.txt" ))

// the following was true for scala-tools.org
//
// publishMavenStyle := true
//
//publishTo <<= version { (v: String) =>
//   Some( "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/".+(
//      if( v.endsWith( "-SNAPSHOT")) "snapshots/" else "releases/"
//   ))
//}
//
// credentials += Credentials( Path.userHome / ".ivy2" / ".credentials" )

// the following is valid for scalasbt.artifactoryonline.com

publishTo <<= version { (v: String) =>
   Some( if( v.endsWith( "-SNAPSHOT")) {
      Resolver.url( "sbt-plugin-snapshots",
         url( "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-snapshots/" )
      )( Resolver.ivyStylePatterns )
   } else {
      Resolver.url( "sbt-plugin-releases",
         url( "http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/" )
      )( Resolver.ivyStylePatterns )
   })
}

publishMavenStyle := false

credentials += Credentials( Path.userHome / ".ivy2" / ".sbtcredentials" )

pomExtra :=
<licenses>
  <license>
    <name>LGPL v2.1+</name>
    <url>http://www.gnu.org/licenses/lgpl-2.1.txt</url>
    <distribution>repo</distribution>
  </license>
</licenses>


// publishArtifact in (Compile, packageDoc) := false

// publishArtifact in (Compile, packageSrc) := false

// seq(ScriptedPlugin.scriptedSettings: _*)

// ---- ls.implicit.ly ----

seq( lsSettings :_* )

(LsKeys.tags in LsKeys.lsync) := Seq( "sbt", "plugin", "application-bundle", "os-x" )

(LsKeys.ghUser in LsKeys.lsync) := Some( "Sciss" )

(LsKeys.ghRepo in LsKeys.lsync) := Some( "sbt-appbundle" )

// bug in ls -- doesn't find the licenses from global scope
(licenses in LsKeys.lsync) := Seq( "LGPL v2.1+" -> url( "http://www.gnu.org/licenses/lgpl-2.1.txt" ))
