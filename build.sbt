sbtPlugin := true

name := "sbt-appbundle"

organization := "de.sciss"

version := "0.11"

scalacOptions := Seq( "-deprecation", "-unchecked" )

publishMavenStyle := true

publishTo <<= version { (v: String) =>
   Some( "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/".+(
      if( v.endsWith( "-SNAPSHOT")) "snapshots/" else "releases/"
   ))
}

credentials += Credentials( Path.userHome / ".ivy2" / ".credentials" )

publishArtifact in (Compile, packageDoc) := false

publishArtifact in (Compile, packageSrc) := false

// seq(ScriptedPlugin.scriptedSettings: _*)
