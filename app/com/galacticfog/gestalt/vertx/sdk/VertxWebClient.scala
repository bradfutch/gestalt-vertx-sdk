package com.galacticfog.gestalt.vertx.sdk

import akka.util.Timeout
import com.galacticfog.gestalt.vertx.sdk.config.VertxClientConfig
import org.joda.time.DateTime
import play.api.libs.json.JsValue
import play.api.libs.ws._
import play.api.{Application, Logger => log}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

case class UnauthorizedAPIException(resp: String) extends Throwable(resp)
case class ForbiddenAPIException(resp: String) extends Throwable(resp)
case class UnknownAPIException(resp: String) extends Throwable(resp)
case class ResourceNotFoundException(url: String) extends Throwable("resource not found: " + url)


class VertxWebClient(val client: WSClient, val protocol: String, val hostname: String, val port: Int, val username: String, val password: String) {

  private val TASK_TIMEOUT = 10000

  def processResponse( response: WSResponse ): JsValue = {
    response.status match {
      case x if x >= 200 && x < 300 => response.json
      case x if x == 401 => throw new UnauthorizedAPIException( response.body )
      case x if x == 403 => throw new ForbiddenAPIException( response.body )
      case x if x == 404 => throw new ResourceNotFoundException( response.body )
      case _ => throw new UnknownAPIException( s"${response.status}: ${response.body}" )
    }
  }

  private def removeLeadingSlash( endpoint: String ) = {
    if ( endpoint.startsWith( "/" ) ) endpoint.substring( 1 )
    else endpoint
  }

  private def genRequest( endpoint: String ): WSRequestHolder = {
    val href =  s"${protocol}://${hostname}:${port}/${removeLeadingSlash( endpoint )}"
    genBareRequest( href )
  }

  def genBareRequest( href : String ) : WSRequestHolder = {
    client.url( href )
      .withHeaders(
        "Content-Type" -> "application/json",
        "Accept" -> "application/json"
      )
      .withAuth(username = username, password = password, scheme = WSAuthScheme.BASIC)
  }

  def get(endpoint: String, params : Option[Map[String,String]] = None ) : Future[JsValue] = {
    val request = genRequest( endpoint )
    val finalRequest = params match {
      case Some( s ) => request.withQueryString( s.toSeq: _* )
      case None => request
    }
    finalRequest.get( ).map( processResponse )
  }

  def easyGet( endpoint : String, params : Option[Map[String,String]] = None ) : Try[JsValue] = Try {
    val future =  get( endpoint, params )
    val timeout = Timeout( 5 seconds )
    Await.result( future, timeout.duration )
  }

  def post(endpoint: String, payload: JsValue): Future[JsValue] = genRequest(endpoint).post(payload).map(processResponse)

  def post(endpoint: String): Future[JsValue] = genRequest(endpoint).post("").map(processResponse)

  def easyPost( endpoint : String, payload : JsValue) : Try[JsValue] = Try{
    log.trace( s"easyPost( $endpoint) : ${payload.toString()}" )
    val future = post( endpoint, payload )
    val timeout = Timeout( 5 seconds )
    Await.result( future, timeout.duration )
  }

  def easyPost( endpoint : String ) : Try[JsValue] = Try{
    log.trace( s"easyPost( $endpoint)" )
    val future = post( endpoint )
    val timeout = Timeout( 5 seconds )
    Await.result( future, timeout.duration )
  }

  def delete(endpoint: String): Future[JsValue] = genRequest(endpoint).delete().map(processResponse)

  def easyDelete( endpoint : String ) : Try[JsValue] = Try{
    log.trace( s"easyDelete( $endpoint)" )
    val future = delete( endpoint )
    val timeout = Timeout( 5 seconds )
    Await.result( future, timeout.duration )
  }
}

object VertxWebClient {
  def apply(wsclient: WSClient, protocol: String, hostname: String, port: Int, username: String, password: String) =
    new VertxWebClient(client = wsclient, protocol = protocol, hostname = hostname, port = port, username = username, password = password)

  def apply(protocol: String, hostname: String, port: Int, username: String, password: String)(implicit app: Application) =
    new VertxWebClient(client = WS.client, protocol = protocol, hostname = hostname, port = port, username = username, password = password)

  def apply( vertxConfig: VertxClientConfig )(implicit app: Application) =
    new VertxWebClient(client = WS.client, vertxConfig.protocol,vertxConfig.host,vertxConfig.port,vertxConfig.username,vertxConfig.password)
}
