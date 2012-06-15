## sbt-appbundle

### statement

sbt-appbundle is a plugin for xsbt (sbt 0.11) that adds the `appbundle` task to create a standalone OS X application bundle.

sbt-appbundle is (C)opyright 2011&ndash;2012 by Hanns Holger Rutz. All rights reserved. It is released under the [GNU Lesser General Public License](http://github.com/Sciss/sbt-appbundle/blob/master/licenses/sbt-appbundle-License.txt) and comes with absolutely no warranties. To contact the author, send an email to `contact at sciss.de`.

### usage

To use the plugin in your sbt project, add the following line to `project/plugins.sbt`:

    addSbtPlugin( "de.sciss" % "sbt-appbundle" % "0.14" )

sbt-appbundle is now hosted on maven central (sonatype), so it should be found automatically.

You can find an example of its usage in `test-project`. Basically you add the following statement to the beginning of the main `build.sbt`:

    seq(appbundle.settings: _*)

And can then configure the `appbundle` task. Without any additional configuration, the task will create the app bundle in the base directory under the name `project-name.app`. The following keys are available:

|**name**          |**key-type**           |**description**             |**default**|
|------------------|-----------------------|----------------------------|-----------|
|`name`            |`String`               |Name for the bundle, without the .app extension | `name` in main scope |
|`normalizedName`  |`String`               |Lower case namem used as second part in the bundle identifier | `normalizedName` in main scope |
|`organization`    |`String`               |Your publishing domain (reverse website style), used as first part in the bundle identifier | `organization` in main scope |
|`version`         |`String`               |Version string which is shown in the Finder and About menu | `version` in main scope |
|`mainClass`       |`Option[String]`       |Main class entry when application is launched. Appbundle fails when this is not specified or inferred | `mainClass` in main scope |
|`target`          |`File`                 |Directory in which the bundle is to be created | `target` in main scope |
|`outputPath`      |`File`                 |Fully qualified path to the bundle | `target / name + ".app"` |
|`executable`      |`File`                 |Path to the java application stub executable. | /System/ Library/ Frameworks/ JavaVM.framework/ Versions/ Current/ Resources/ MacOS/ JavaApplicationStub |
|`fullClasspath`   |`Classpath`            |Constructed from the `fullClasspath` entries in `Compile` and `Runtime` | |
|`javaVersion`     |`String`               |The minimum Java version required to launch the application | `1.6+` |
|`javaArchs`       |`Seq[String]`          |If not empty, the supported processor architectures in order of their preference | empty |
|`javaOptions`     |`Seq[String]`          |Options passed to the `java` command when launching the application | `javaOptions` in main scope |
|`systemProperties`|`Seq[(String, String)]`|A key-value map passed as Java `-D` arguments (system properties) | extracts `-D` entries from `javaOptions` and adds entries for `screenMenu` and `quartz` |
|`screenMenu`      |`Boolean`              |Whether to display the menu bar in the screen top | `true`
|`quartz`          |`Option[Boolean]`      |Whether to use the Apple Quartz renderer (`true`) or the default Java renderer | `None`. In this case Quartz is used for Java 1.5, but not for Java 1.6+ |
|`icon`            |`Option[File]`         |Image or icon file which is used as application icon. A native `.icns` file will be copied unmodified to the bundle, while an image (such as `.png`) will be converted through the OS X shell utility `sips`, scaling the image to the next supported size, which is either of 16, 32, 48, 128, 256, or 512 pixels width/height | `None` |
|`resources`       |`Seq[File]`            |Any files or directories which should be copied directly into `Contents/Resources` | empty |
|`workingDirectory`|`Option[File]`         |The current directory as seen from the Java runtime | `None` (directory in which the bundle resides (outside the app bundle) |

The following special variables are particularly useful for `workingDirectory`:

     appbundle.BundleVar_AppPackage  // the base directory _inside_ the application bundle
     appbundle.BundleVar_JavaRoot    // the directory in which the Java jars reside
     appbundle.BundleVar_UserHome    // the user's home directory

The following values can be used for the `javaArchs` key:

     appbundle.JavaArch_i386
     appbundle.JavaArch_x86_64
     appbundle.JavaArch_ppc

Example:

    appbundle.name := "CamelCase"
    appbundle.javaOptions += "-Xmx1024m"
    appbundle.javaOptions ++= Seq( "-ea" )
    appbundle.systemProperties += "SC_HOME" -> "../scsynth"
    appbundle.icon := Some( file( "myicon.png" ))

As of version 0.14, the default bundle target directory is `target`. If you want to revert to the previous behaviour, putting it into the main directory, the following can be used:

    appbundle.target <<= baseDirectory

### creating an IntelliJ IDEA project

If you want to work on the plugin in IntelliJ IDEA, you can set up a project like this: Make sure you have the following contents in `~/.sbt/plugins/build.sbt`:

    resolvers += "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"
    
    addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.0.0")

Then to create the IDEA project, run the following two commands from the xsbt shell:

    > set ideaProjectName := "sbt-appbundle"
    > gen-idea

### credits

The test application icon is in the public domain and was obtained from the [Open Clip Art Library](http://openclipart.org/detail/20299/moon-in-comic-style-by-rg1024-20299)                     .
