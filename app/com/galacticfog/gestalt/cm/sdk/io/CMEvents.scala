package com.galacticfog.gestalt.cm.sdk.io

import com.galacticfog.gestalt.streaming.io.internal.HandlerFunction
import com.galacticfog.gestalt.streaming.io._
import com.galacticfog.gestalt.tasks.io._
import play.api.libs.json.{Json, JsError, JsSuccess, JsValue}
import play.api.{Logger => log}

import scala.util.{Failure, Success, Try}

trait CMEvents {

  def raiseChangeEvent( change : ChangeMessage ): Unit = {

    println( "***")
    println( "*** raising task event ***")
    println( "*** change : " + change.change.toString() )
    println( "*** event : " + change.event.asEvent.asJsString )
    println( "***")

    CMTaskController.handleChange( change )
  }

  /**
   *
   * Used like so :
   * val listener = new GestaltStream
   * listener.addFilters( exactChangeFilter("task.dns.record.create.pending", createRecord) )
   * listener.listen()
   *
   * Where the signature for createRecord is as follows :
   * def createRecord(  taskEvent : ApiTaskEvent ) : Try[Option[ApiTaskResult]]
   *
   */

  def exactChangeFilter( pattern : String, f:ApiTaskEvent => Try[Option[ApiTaskResult]] ) : (EventFilter, HandlerFunction) = {
    filter( pattern, EventMatchType.Exact, taskHandler( f, pattern ) )
  }

  /**
   *
   * This will handle several things automagically.  First it will parse the incoming event and turn
   * the data into an ApiTaskEvent.  This allows users already integrating with the Task API to not
   * change function signatures for handler methods.
   *
   * Secondly, this will handle the completion or fail events
   *
   */

  def taskHandler( func: ApiTaskEvent => Try[Option[ApiTaskResult]], name : String )( in : JsValue ) : Unit = {

    val task : ApiTask = in.validate[ApiTask] match {
      case JsSuccess( s, _ ) => s
      case err@JsError(_) => {
        throw new Exception( "Failed to parse record create info : " + Json.toJson( JsError.toFlatJson(err) ))
      }
    }

    val result = func( ApiTaskEvent( name, task ) ) match {
      case Success( s ) => {
        s match {
          case Some( res ) => {
            if( res.errors.isDefined ) {
              failEvent( name, task.copy( result = res.asOpt ) )
            }
            else {
              completeEvent( name, task.copy( result = res.asOpt ) )
            }
          }
          case None => {
            throw new Exception( "This is a completely unexpected result and represents a programming error")
          }
        }
      }
      case Failure( ex ) => {
        val error = makeError( "Error : ", Some(ex) ).get
        failEvent( name, task.copy( result = error ))
      }
    }
  }

  def makeError( msg : String, ex : Option[Throwable] = None ) : Try[Option[ApiTaskResult]] = Try {

    val exceptionString = ex match {
      case Some(s) => s.getMessage()
      case None => ""
    }

    ApiTaskResult(
      message = Some( msg ),
      errors = Some(Seq(
        ApiError( None, exceptionString ) ))).asOpt
  }

  def getStateEventName( eventName : String, stateName : String ) : String = {
    val parts = eventName.split( "\\." )
    val newState = parts.slice(0, parts.size - 1).mkString( "." ) + "." + stateName
    log.debug( "out state : " + newState )
    newState
  }

  def failEvent( eventName : String, task : ApiTask ) = {
    log.debug( s"failEvent( $eventName ) - task ( ${task.uuid} )" )
    CMTaskController.writeEvent( GestaltEvent( getStateEventName( eventName, "failed" ), task.withStatus(TaskStatus.Halted).asJsValue ) )
  }
  def completeEvent( eventName : String, task : ApiTask ) = {
    log.debug( s"completeEvent( $eventName ) - task ( ${task.uuid} )" )
    CMTaskController.writeEvent( GestaltEvent( getStateEventName( eventName, "complete" ), task.withStatus(TaskStatus.Completed).asJsValue ) )
  }


}