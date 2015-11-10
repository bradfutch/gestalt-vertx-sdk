package com.galacticfog.gestalt.cm.sdk.config

import play.api.libs.json.Json

case class CMClientConfig( protocol : String, host : String, port : Int, username : String, password : String )

object CMClientConfig {
  implicit val cmClientConfigFormat = Json.format[CMClientConfig]
}
