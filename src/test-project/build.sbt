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

// ---- publishing ----

publishTo <<= version { (v: String) =>
   Some( "Scala Tools Nexus" at "http://nexus.scala-tools.org/content/repositories/".+(
      if( v.endsWith( "-SNAPSHOT")) "snapshots/" else "releases/"
   ))
}

pomExtra :=
<licenses>
  <license>
    <name>LGPL v2.1+</name>
    <url>http://www.gnu.org/licenses/lgpl-2.1.txt</url>
    <distribution>repo</distribution>
  </license>
</licenses>

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
