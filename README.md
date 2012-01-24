## sbt-appbundle

### statement

sbt-appbundle is a plugin for xsbt (sbt 0.11) that adds the `appbundle` task to create a standlone OS X application bundle.

sbt-appbundle is (C)opyright 2011-2012 by Hanns Holger Rutz. All rights reserved. It is released under the [GNU Lesser General Public License](http://github.com/Sciss/sbt-appbundle/blob/master/licenses/sbt-appbundle-License.txt) and comes with absolutely no warranties. To contact the author, send an email to `contact at sciss.de`.

### usage

To use the plugin in your sbt project, add the following entry to `project/plugins.sbt`:

    addSbtPlugin( "de.sciss" % "sbt-appbundle" % "0.11")

You can find an example of its usage in `src/test-project`. Basically you add the following statement to the beginning of the main `build.sbt`:

    seq(appbundle.settings: _*)

And can then configure the `appbundle` task. Without any additional configuration, the task will create the app bundle in the base directory under the name `project-name.app`. The following keys are available:

 - `name` : `SettingKey[String]` &ndash; Name for the bundle, without the .app extension. (defaults to `name` in main scope)
 - `normalizedName` : `SettingKey[String]` &ndash; Lower case namem used as second part in the bundle identifier. (defaults to `normalizedName` in main scope)
 - `organization` : `SettingKey[String]` &ndash; Your publishing domain (reverse website style), used as first part in the bundle identifier. (defaults to `organization` in main scope)
 - `version` : `SettingKey[String]` &ndash; Version string which is shown in the Finder and About menu. (defaults to `version` in main scope)
 - `mainClass` : `TaskKey[Option[String]]` &ndash; Main class entry when application is launched. Appbundle fails when this is not specified or inferred. (defaults to `mainClass` in main scope)
 - `stub` : `SettingKey[File]` &ndash; Path to the java application stub executable. (defaults to `file( "/System/Library/Frameworks/JavaVM.framework/Versions/Current/Resources/MacOS/JavaApplicationStub" )`)
 - `fullClasspath` : `TaskKey[Classpath]` &ndash; Constructed from the `fullClasspath` entries in `Compile` and `Runtime`.
 - `javaOptions` : `SettingKey[Seq[String]]` &ndash; Options passed to the `java` command when launching the application. (defaults to `javaOptions` in main scope)
 - `systemProperties` : `SettingKey[Seq[(String, String)]]` &ndash; A key-value map passed as Java `-D` arguments (system properties). (by default extracts `-D` entries from `javaOptions` and adds entries for `screenMenu` and `quartz`)
 - `screenMenu` : `SettingKey[Boolean]` &ndash; Whether to display the menu bar in the screen top. (defaults to `true`)
 - `quartz` : `SettingKey[Option[Boolean]]` &ndash; Whether to use the Apple Quartz renderer (true) or the default Java renderer. (Defaults to `None`. In this case Quartz is used for Java 1.5, but not for Java 1.6+)
 - `icon` : `SettingKey[ Option[ File ]]` &ndash; Image or icon file which is used as application icon. A native `.icns` file will be copied unmodified to the bundle, while an image (such as `.png`) will be converted through the OS X shell utility `sips`, scaling down the image to the next supported size, which is either of 16, 32, 48, 128, 256, or 512 pixels width/height. (defaults to `None`)

Example:

    appbundle.name := "CamelCase"
    appbundle.javaOptions += "-Xmx1024m"
    appbundle.javaOptions ++= Seq( "-ea" )
    appbundle.systemProperties += "SC_HOME" -> "../scsynth"
    appbundle.icon = file( "myicon.png" )

### credits

The test application icon is in the public domain and was obtained from the [Open Clip Art Library](http://openclipart.org/detail/20299/moon-in-comic-style-by-rg1024-20299)                     .
