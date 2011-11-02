name := "testapp"

organization := "de.sciss"

version := "0.10"

seq(appbundleSettings: _*)

libraryDependencies ++= Seq(
   "de.sciss" %% "scalaosc" % "0.30",
   "org.scala-lang" % "scala-swing" % "2.9.1"
)

retrieveManaged := true

appbundleName := "TestApp"

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
