package com.galacticfog.gestalt.billing.sdk

import akka.util.Timeout
import org.joda.time.DateTime
import play.api.libs.json.JsValue
import play.api.libs.ws._
import play.api.{Application, Logger => log}
import com.galacticfog.gestalt.tasks.io.ApiTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

case class UnauthorizedAPIException(resp: String) extends Throwable(resp)
case class ForbiddenAPIException(resp: String) extends Throwable(resp)
case class UnknownAPIException(resp: String) extends Throwable(resp)
case class ResourceNotFoundException(url: String) extends Throwable("resource not found: " + url)


class BillingWebClient(val client: WSClient, val protocol: String, val hostname: String, val port: Int, val username: String, val password: String) {

  private val TASK_TIMEOUT = 5000

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

  def easyPost( endpoint : String, payload : JsValue) : Try[JsValue] = {
    log.trace( s"easyPost( $endpoint) : ${payload.toString()}" )
    taskWaiter( post( endpoint, payload ) )
  }

  def easyPost( endpoint : String ) : Try[JsValue] = {
    log.trace( s"easyPost( $endpoint)" )
    taskWaiter( post( endpoint ) )
  }

  def delete(endpoint: String): Future[JsValue] = genRequest(endpoint).delete().map(processResponse)

  def easyDelete( endpoint : String ) : Try[JsValue] = {
    log.trace( s"easyDelete( $endpoint)" )
    taskWaiter( delete( endpoint ) )
  }


  def getTask( href : String ) : Try[ApiTask] = Try {
    log.trace( s"getTask( $href )" )
    val request = genBareRequest( href )
    val future = request.get().map( processResponse )
    val timeout = Timeout( 5 seconds )
    val response = Await.result( future, timeout.duration )
    response.validate[ApiTask].get
  }

  def taskWaiter( future : Future[JsValue] ) : Try[JsValue] = Try {
    log.trace( "taskWaiter()" )
    val timeout = Timeout( 5 seconds )
    val response = Await.result( future, timeout.duration )
    val responseTask = response.validate[ApiTask].get

    var bCompleted = false
    val timeStart = DateTime.now
    while( !bCompleted && (DateTime.now.getMillis - timeStart.getMillis) < TASK_TIMEOUT )
    {
      val completedUri = responseTask.href + "/complete"
      getTask( completedUri ) match {
        case Success(s) => {
          bCompleted = true
        }
        case Failure(ex) => {
          Thread.sleep( 1000 )
        }
      }

      log.debug( "Waiting for task completion..." + (DateTime.now.getMillis - timeStart.getMillis) / 1000.0 + " secs")
    }

    if( !bCompleted )
    {
      throw new Exception( s"Failed to complete task in $TASK_TIMEOUT milliseconds" )
    }

    val completedTask = getTask( responseTask.href + "/complete" ).get
    val resourceUri = completedTask.detail.result.get.link match {
      case Some(s) => {
        s.href
      }
      case None => {
        throw new Exception( handleErrors( completedTask ) )
      }
    }
    easyGet( resourceUri ).get
  }

  def handleErrors( task : ApiTask ) : String = {
    task.detail.result.get.errors.get.foldLeft("")( (r,c) => r + ( c.message + "\n") )
  }

}

object BillingWebClient {
  def apply(wsclient: WSClient, protocol: String, hostname: String, port: Int, username: String, password: String) =
    new BillingWebClient(client = wsclient, protocol = protocol, hostname = hostname, port = port, username = username, password = password)

  def apply(protocol: String, hostname: String, port: Int, username: String, password: String)(implicit app: Application) =
    new BillingWebClient(client = WS.client, protocol = protocol, hostname = hostname, port = port, username = username, password = password)

  def apply( billingConfig: BillingClientConfig )(implicit app: Application) =
    new BillingWebClient(client = WS.client, billingConfig.protocol,billingConfig.host,billingConfig.port,billingConfig.username,billingConfig.password)
}
