package de.sciss.sbt.appbundle

object Constants {
  val PListVersion                    = "6.0"

  val CFBundleInfoDictionaryVersion   = "CFBundleInfoDictionaryVersion"
  val CFBundleName                    = "CFBundleName"
  val CFBundlePackageType             = "CFBundlePackageType"
  val CFBundleAllowMixedLocalizations = "CFBundleAllowMixedLocalizations"
  val CFBundleExecutable              = "CFBundleExecutable"
  val CFBundleIconFile                = "CFBundleIconFile"
  val CFBundleIdentifier              = "CFBundleIdentifier"
  val CFBundleShortVersionString      = "CFBundleShortVersionString"
  val CFBundleSignature               = "CFBundleSignature"
  val CFBundleVersion                 = "CFBundleVersion"
  val CFBundleDocumentTypes           = "CFBundleDocumentTypes"
  val CFBundleTypeExtensions          = "CFBundleTypeExtensions"
  val CFBundleTypeIconFile            = "CFBundleTypeIconFile"
  val CFBundleTypeMIMETypes           = "CFBundleTypeMIMETypes"
  val CFBundleTypeName                = "CFBundleTypeName"
  val CFBundleTypeOSTypes             = "CFBundleTypeOSTypes"
  val CFBundleTypeRole                = "CFBundleTypeRole"

  val LSItemContentTypes              = "LSItemContentTypes"
  val LSHandlerRank                   = "LSHandlerRank"
  val LSTypeIsPackage                 = "LSTypeIsPackage"

  val NSHighResolutionCapable         = "NSHighResolutionCapable"

  val BundlePackageTypeAPPL           = "APPL"

  val BundleKey_Java                  = "Java"

  val BundleVar_JavaRoot              = "$JAVAROOT"
  val BundleVar_AppPackage            = "$APP_PACKAGE"
  val BundleVar_UserHome              = "$USER_HOME"

  val JavaKey_MainClass               = "MainClass"
  val JavaKey_Properties              = "Properties"
  val JavaKey_ClassPath               = "ClassPath"
  val JavaKey_JVMVersion              = "JVMVersion"
  val JavaKey_VMOptions               = "VMOptions"
  val JavaKey_WorkingDirectory        = "WorkingDirectory"
  val JavaKey_JVMArchs                = "JVMArchs"

  val JavaArch_i386                   = "i386"
  val JavaArch_x86_64                 = "x86_64"
  val JavaArch_ppc                    = "ppc"

  val Signature_Unknown               = "????"
}

// apple.laf.useScreenMenuBar (default false)
// apple.awt.brushMetalLook (default false)
// apple.awt.fileDialogForDirectories (default false)
// apple.awt.UIElement (default false)

// apple.awt.fakefullscreen (default false)
// apple.awt.fullscreencapturealldisplays (false)
// apple.awt.fullscreenhidecursor (default true)
// apple.awt.fullscreenusefade (default false)

// apple.awt.antialiasing (default on? for aqua)
// apple.awt.textantialiasing (default on? for aqua)
// apple.awt.rendering = speed | quality
// apple.awt.interpolation
// apple.awt.fractionalmetrics

// apple.awt.graphics.OptimizeShapes
// apple.awt.graphics.EnableLazyDrawing
// apple.awt.graphics.EnableLazyDrawingQueueSize
// apple.awt.graphics.EnableQ2DX
// apple.awt.graphics.EnableDeferredUpdates

// apple.awt.graphics.UseQuartz (default true for Java 1.5, false for Java 1.6)

// apple.awt.graphics.EnableLazyPixelConversion



// WorkingDirectory (default: $APP_PACKAGE)

// Arguments (A string or array of strings)

// Variables: $JAVAROOT, $APP_PACKAGE, $USER_HOME

// useful existing keys: description, homepage, javaOptions, javacOptions, licenses
// mainClass, normalizedName, moduleName (= normalizedName???), organization
// organizationHomepage, organizationName, projectID (= moduleID:version???)

// ModuleInfo (nameFormal: String, description: String, homepage: Option[URL],
// startYear: Option[Int], licenses: Seq[(String, URL)], organizationName: String, organizationHomepage: Option[URL])

// selectMainClass
// startYear
// timingFormat
// version

// CFBundleDevelopmentRegion --> (Recommended), e.g. English
// CFBundleDisplayName --> "If you do not intend to localize your bundle, do not include this key in your Info.plist file."
// CFBundleDocumentTypes --> would be nice to support this eventually
// CFAppleHelpAnchor
// CFBundleIconFile --> Mac OS X; "The filename you specify does not need to include the extension, although it may.
// The system looks for the icon file in the main resources directory of the bundle."
// CFBundleIconFiles --> iOS
// CFBundleIcons --> iOS
// CFBundleURLTypes
