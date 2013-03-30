name := "TestApp"

organization := "de.sciss"

version := "1.0.0"

seq(appbundle.settings: _*)

scalaVersion := "2.10.1"

libraryDependencies += "de.sciss" %% "scalaosc" % "1.1.+"

libraryDependencies <+= scalaVersion { sv =>  "org.scala-lang" % "scala-swing" % sv }

retrieveManaged := true

appbundle.name := "TestApplication"

appbundle.javaOptions += "-Xmx1024m"

appbundle.icon := Some(file("help") / "images" / "rg1024_Moon_in_comic_style.png")

appbundle.javaOptions ++= Seq("-ea")

appbundle.systemProperties += "APP_TITLE" -> "Open Sound Control"

appbundle.resources += file("help")

appbundle.workingDirectory := Some(file(appbundle.BundleVar_AppPackage))

appbundle.target <<= baseDirectory
