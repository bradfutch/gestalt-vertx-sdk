package com.galacticfog.gestalt.cm.sdk.io

import com.galacticfog.gestalt.{Res, Gestalt}
import com.galacticfog.gestalt.cm.io.domain.ChangeDao
import com.galacticfog.gestalt.cm.sdk.CMClient
import com.galacticfog.gestalt.cm.sdk.config.CMClientConfig
import com.galacticfog.gestalt.streaming.io.internal.HandlerFunction
import com.galacticfog.gestalt.streaming.io._
import play.api.Play.current
import play.api.libs.EventSource
import play.api.libs.iteratee._
import play.api.libs.json.Json
import play.api.libs.ws._
import play.api.mvc._
import play.api.{Logger => log}

import scala.collection.concurrent.TrieMap
import scala.util.{Success, Failure}


object CMTaskController extends Controller {

  println("**********CMTaskController::static_init()***********")

  val meta = new Gestalt
  val loggerConfig = Json.parse( meta.getConfig( "stream-logger" ).get ).validate[LoggerConfig].get
  log.debug( "found logger config : " + loggerConfig.asJsString() )
  val writer = new GestaltEventWriter( loggerConfig )

  //val clientConfig = Json.parse( meta.getConfig( "cm-client" ).get ).validate[LoggerConfig].get
  //@HACK - TEMP
  val clientConfig = new CMClientConfig( "http", "localhost", 9002, "brad", "letmein" )
  val client = CMClient( clientConfig )

  //@HACK - TEMP
  //val bEnabled = cmConfig.enabled
  val bEnabled = true

  def handleChange( change : ChangeMessage ) = {

    if( !bEnabled )
    {
      writeEvent( change.event.copy( event_name = change.change.outStateName ).asEvent )
    }
    else
    {
      writeEvent( change.event.asEvent )
      client.createChange( change.change )
    }
  }

  def writeEvent( event : GestaltEvent ) = {
      writer.write( event )
  }
}

