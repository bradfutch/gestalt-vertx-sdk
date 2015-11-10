package com.galacticfog.gestalt.cm.sdk

import com.galacticfog.gestalt.cm.io.domain.{HandlerDao, ChangeDao}
import com.galacticfog.gestalt.cm.sdk.config.CMClientConfig
import play.api.Application
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient

import scala.util.{Failure, Success, Try}

class CMClient( val client : CMWebClient, val protocol: String, val hostname: String, val port: Int,
                    val username: String, val password: String)
{

  //----------------------
  //      CHANGES
  //----------------------

  def createChange( info : ChangeDao ) : Try[ChangeDao] = {
    convertChange( client.easyPost( "/changes", Json.toJson( info ) ) )
  }

  def getChange( id : String ) : Try[ChangeDao] = {
    convertChange( client.easyGet( "/changes/" + id ) )
  }

  def searchChange( params : Map[String,String] ) : Try[ChangeDao] = {
    convertChange( client.easyGet( "/changes", Some(params) ) )
  }

  def deleteChange( id : String ) : Try[ChangeDao] = {
    convertChange( client.easyDelete( "/changes/" + id ) )
  }

  def convertChange( info : Try[JsValue] ) : Try[ChangeDao] = Try {
    info match {
      case Success( s ) => {
        s.validate[ChangeDao] getOrElse {
          throw new Exception( "Error parsing return JsValue for changes" )
        }
      }
      case Failure(ex) => {
        ex.printStackTrace()
        throw new Exception( "Error : " + ex.getMessage() )
      }
    }
  }

  //----------------------
  //      HANDLERS
  //----------------------

  def createHandler( info : HandlerDao ) : Try[HandlerDao] = {
    convertHandler( client.easyPost( "/handlers", Json.toJson( info ) ) )
  }

  def getHandler( id : String ) : Try[HandlerDao] = {
    convertHandler( client.easyGet( "/handlers/" + id ) )
  }

  def searchHandler( params : Map[String,String] ) : Try[HandlerDao] = {
    convertHandler( client.easyGet( "/handlers", Some(params) ) )
  }

  def deleteHandler( id : String ) : Try[HandlerDao] = {
    convertHandler( client.easyDelete( "/handlers/" + id ) )
  }

  def convertHandler( info : Try[JsValue] ) : Try[HandlerDao] = Try {
    info match {
      case Success( s ) => {
        s.validate[HandlerDao] getOrElse {
          throw new Exception( "Error parsing return JsValue for handlers" )
        }
      }
      case Failure(ex) => {
        ex.printStackTrace()
        throw new Exception( "Error : " + ex.getMessage() )
      }
    }
  }

}

object CMClient
{
  def apply(wsclient: WSClient, protocol: String, hostname: String, port: Int, username: String, password: String) = {
    val client = CMWebClient( wsclient, protocol, hostname, port, username, password )
    new CMClient( client = client, protocol = protocol, hostname = hostname, port = port, username = username, password = password )
  }

  def apply(protocol: String, hostname: String, port: Int, username: String, password: String)(implicit app: Application) = {
    val client = CMWebClient( protocol, hostname, port, username, password )
    new CMClient( client = client, protocol = protocol, hostname = hostname, port = port, username = username, password = password )
  }

  def apply( billingConfig: CMClientConfig )(implicit app: Application) = {
    val client = CMWebClient( billingConfig )
    new CMClient( client = client, billingConfig.protocol, billingConfig.host, billingConfig.port, billingConfig.username, billingConfig.password )
  }
}