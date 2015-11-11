package com.galacticfog.gestalt.vertx.sdk

import com.galacticfog.gestalt.vertx.io.domain.{VertxEvent, VertxDao}

import com.galacticfog.gestalt.vertx.sdk.config.VertxClientConfig
import play.api.Application
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient

import scala.util.{Failure, Success, Try}

class VertxClient( val client : VertxWebClient, val protocol: String, val hostname: String, val port: Int,
                    val username: String, val password: String)
{

  //----------------------
  //      VERTICLES
  //----------------------

  def createVerticle( info : VertxDao ) : Try[VertxDao] = {
    convertVerticle( client.easyPost( "/verticles", Json.toJson( info ) ) )
  }

  def getVerticle( id : String ) : Try[VertxDao] = {
    convertVerticle( client.easyGet( "/verticles/" + id ) )
  }

  def invokeVerticle( id : String, event : VertxEvent ) : Try[VertxDao] = {
    convertVerticle( client.easyPost( s"/verticles/$id/invoke", Json.toJson(event) ) )
  }

  def searchVerticle( params : Map[String,String] ) : Try[VertxDao] = {
    convertVerticle( client.easyGet( "/verticles", Some(params) ) )
  }

  def deleteVerticle( id : String ) : Try[VertxDao] = {
    convertVerticle( client.easyDelete( "/verticles/" + id ) )
  }

  def convertVerticle( info : Try[JsValue] ) : Try[VertxDao] = Try {
    info match {
      case Success( s ) => {
        s.validate[VertxDao] getOrElse {
          throw new Exception( "Error parsing return JsValue for changes" )
        }
      }
      case Failure(ex) => {
        ex.printStackTrace()
        throw new Exception( "Error : " + ex.getMessage() )
      }
    }
  }
}

object VertxClient
{
  def apply(wsclient: WSClient, protocol: String, hostname: String, port: Int, username: String, password: String) = {
    val client = VertxWebClient( wsclient, protocol, hostname, port, username, password )
    new VertxClient( client = client, protocol = protocol, hostname = hostname, port = port, username = username, password = password )
  }

  def apply(protocol: String, hostname: String, port: Int, username: String, password: String)(implicit app: Application) = {
    val client = VertxWebClient( protocol, hostname, port, username, password )
    new VertxClient( client = client, protocol = protocol, hostname = hostname, port = port, username = username, password = password )
  }

  def apply( billingConfig: VertxClientConfig )(implicit app: Application) = {
    val client = VertxWebClient( billingConfig )
    new VertxClient( client = client, billingConfig.protocol, billingConfig.host, billingConfig.port, billingConfig.username, billingConfig.password )
  }
}