package com.galacticfog.gestalt.billing.sdk

import play.api.libs.json.Json

case class BillingClientConfig( protocol : String, host : String, port : Int, username : String, password : String )

object BillingClientConfig {
  lazy implicit val billingClientConfigFormat = Json.format[BillingClientConfig]
}
