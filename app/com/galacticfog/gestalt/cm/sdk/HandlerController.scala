package com.galacticfog.gestalt.cm.sdk

import com.galacticfog.gestalt.streaming.io._
import play.api.libs.json.JsValue
import play.api.{Logger => log}
import play.api.Logger

object HandlerController {

  def getHandlerListener( name : String, id : String, f : JsValue => Unit ) : GestaltStreamListener = {

    //TODO : fix this when we're integrated with config
    val host = "events.galacticfog.com"
    val port = 2181
    val channel = s"test/${name}"

    val listenConfig = ListenerConfig( host, port, channel )

    log.info( "[cm]: Creating Change Event Listener with config:\n" + listenConfig.asJsString )

    val listener = new GestaltStreamListener( listenConfig )

    //listener = new GestaltStreamListener()
    listener.addFilters(
      filter( s"(pending)+.*(${id})+", EventMatchType.Regex, f )
    )

    //log.info("[cm]: Change Event Listener started with config:\n" + listener.config.asJsString)
    //log.info("[cm]: Starting Change Event Listener...")
    //listener.listen()
    listener
  }
}
