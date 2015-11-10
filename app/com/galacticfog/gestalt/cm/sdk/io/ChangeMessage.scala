package com.galacticfog.gestalt.cm.sdk.io

import com.galacticfog.gestalt.cm.io.domain.ChangeDao
import com.galacticfog.gestalt.tasks.io.ApiTaskEvent
import play.api.libs.json.{JsValue, Json}

case class ChangeMessage( change : ChangeDao, event : ApiTaskEvent ) {
  def withActionArgs( args : Map[String, JsValue] ) : ChangeMessage = {
    this.copy( event = event.withActionArgs( args ) )
  }
}
