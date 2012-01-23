package de.sciss.testapp

import de.sciss.osc.{Message, UDP}
import swing.{Alignment, Swing, Action, MainFrame, TextField, Label, GridPanel, SimpleSwingApplication}

object TestApp extends SimpleSwingApplication {
   lazy val rcv   = { val r = UDP.Receiver(); r.connect(); r }
   lazy val trns  = { val t = UDP.Transmitter( rcv.localSocketAddress ); t.connect(); t }

   lazy val top = new MainFrame {
      title       = "Open Sound Control"
      resizable   = false
      contents    = new GridPanel( 2, 2 ) {
         contents += new Label( "Transmit:", null, Alignment.Right )
         contents += new TextField( 12 ) {
            action = Action( "transmit" ) {
               trns ! Message( "/info", text )
            }
         }
         contents += new Label( "Receive:", null, Alignment.Right )
         contents += new TextField( 12 ) {
            editable = false
            rcv.action = {
               case (Message( "/info", payload: String ), _) => Swing.onEDT( text = payload )
               case _ =>
            }
         }
      }
      pack()
      centerOnScreen()
      open()
   }
}

//object Gaga extends App