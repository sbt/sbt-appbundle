package de.sciss.testapp

import de.sciss.osc._
import Implicits._
import swing.{Action, Alignment, Menu, MenuBar, MenuItem, Swing, MainFrame, TextField, Label, GridPanel, SimpleSwingApplication}
import java.awt.Desktop
import java.io.File

object TestApp extends SimpleSwingApplication {
  lazy val trns = {
    val t = UDP.Transmitter(); t.connect(); t
  }
  lazy val rcv = {
    val r = UDP.Client(trns.localSocketAddress); r.connect(); r
  }

  lazy val top = new MainFrame {
    title     = sys.props.getOrElse("APP_TITLE", "Window Title")
    resizable = false
    contents  = new GridPanel(2, 2) {
      contents += new Label("Transmit:", null, Alignment.Right)
      contents += new TextField(12) {
        action = Action("transmit") {
          trns.send(Message("/info", text), rcv.localSocketAddress)
        }
        tooltip = "Check out the help menu for more info!"
      }
      contents += new Label("Receive:", null, Alignment.Right)
      contents += new TextField(12) {
        editable   = false
        rcv.action = {
          case Message("/info", payload: String) => Swing.onEDT(text = payload)
          case _ =>
        }
      }
    }

    menuBar = new MenuBar {
      contents += new Menu("Help") {
        contents += new MenuItem(Action("Show help file...") {
          Desktop.getDesktop.open(new File("Contents/Resources/help/index.html"))
        })
      }
    }

    pack()
    centerOnScreen()
    open()
  }
}
