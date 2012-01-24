name := "TestApp"

organization := "de.sciss"

version := "0.11"

seq(appbundle.settings: _*)

libraryDependencies ++= Seq(
   "de.sciss" %% "scalaosc" % "0.30",
   "org.scala-lang" % "scala-swing" % "2.9.1"
)

retrieveManaged := true

appbundle.name := "TestApplication"

appbundle.javaOptions += "-Xmx1024m"

appbundle.icon := Some( file( "rg1024_Moon_in_comic_style.png" ))

appbundle.javaOptions ++= Seq( "-ea" )

appbundle.systemProperties += "SC_HOME" -> "../scsynth"

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
