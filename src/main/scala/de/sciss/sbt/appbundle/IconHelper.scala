package de.sciss.sbt.appbundle

import sbt._

object IconHelper {
  def convertToICNS(inF: File, outF: File) {
    if (inF.ext == "icns") {
      IO.copyFile(inF, outF, preserveLastModified = true)
    } else {
      import sys.process._
      val lines       = Seq("sips", "-g", "pixelHeight", "-g", "pixelWidth", inF.getPath).lines
      val PixelWidth  = "\\s+pixelWidth: (\\d+)".r
      val PixelHeight = "\\s+pixelHeight: (\\d+)".r
      val srcWidth    = lines.collect { case PixelWidth (s) => s.toInt } .head
      val srcHeight   = lines.collect { case PixelHeight(s) => s.toInt } .head
      val supported   = IndexedSeq(16, 32, 48, 128, 256, 512)
      val srcSize     = math.min(512, math.max(srcWidth, srcHeight))
      val tgtSize     = supported(supported.indexWhere(_ >= srcSize))
      val args0       = Seq(inF.getPath, "--out", outF.getPath)
      val args1       = if (tgtSize != srcWidth || tgtSize != srcHeight) {
        Seq("-z", tgtSize.toString, tgtSize.toString)
      } else {
        Seq.empty
      }
      val args        = Seq("sips", "-s", "format", "icns") ++ args1 ++ args0
      args.!!
    }
  }
}
