package com.galacticfog.gestalt.vertx.sdk.config

import play.api.libs.json.Json

case class VertxClientConfig( protocol : String, host : String, port : Int, username : String, password : String )

object VertxClientConfig {
  implicit val vertxClientConfigFormat = Json.format[VertxClientConfig]
}
