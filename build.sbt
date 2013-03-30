sbtPlugin := true

name := "sbt-appbundle"

organization := "de.sciss"

version := "1.0.1"

scalaVersion := "2.9.2"

crossScalaVersions := Seq("2.9.2", "2.9.1")

scalacOptions := Seq("-deprecation", "-unchecked")

description := "An sbt plugin to create OS X application bundles"

homepage <<= name { n => Some(url("https://github.com/sbt/" + n)) }

licenses := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt"))

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

pomExtra <<= name { n =>
<scm>
  <url>git@github.com:sbt/{n}.git</url>
  <connection>scm:git:git@github.com:sbt/{n}.git</connection>
</scm>
<developers>
   <developer>
      <id>sciss</id>
      <name>Hanns Holger Rutz</name>
      <url>http://www.sciss.de</url>
   </developer>
</developers>
}

// publishArtifact in (Compile, packageDoc) := false

// publishArtifact in (Compile, packageSrc) := false

// ---- ls.implicit.ly ----

seq(lsSettings :_*)

(LsKeys.tags in LsKeys.lsync) := Seq("sbt", "plugin", "application-bundle", "os-x")

(LsKeys.ghUser in LsKeys.lsync) := Some( "sbt" )

(LsKeys.ghRepo in LsKeys.lsync) <<= name(Some(_))

