package com.galacticfog.gestalt.cm.sdk

import com.galacticfog.gestalt.cm.io.domain.ChangeDao
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.mvc.Call
import play.api.mvc.Request
import play.api.mvc.AnyContent
import play.api.mvc.RequestHeader
import play.api.{Logger => log}

import com.galacticfog.gestalt.tasks.io._

package object io {
  
  def newPostChange(outState : String, message: Option[String] = None, httpStatus: Int = HttpStatus.ACCEPTED)(implicit request: Request[JsValue]) = {
    newChange(outState, postAction(), httpStatus, message, request.body)
  }
  
  def newPutChange(outState : String, message: Option[String] = None, httpStatus: Int = HttpStatus.ACCEPTED)(implicit request: Request[JsValue]) = {
    newChange(outState, putAction(), httpStatus, message, request.body)
  }

  def newPatchChange(outState : String, message: Option[String] = None, httpStatus: Int = HttpStatus.ACCEPTED)(implicit request: Request[JsValue]) = {
    newChange(outState, patchAction(), httpStatus, message, request.body)
  }
  
  def newDeleteChange(outState : String, message: Option[String] = None, httpStatus: Int = HttpStatus.ACCEPTED)(implicit request: Request[JsValue]) = {
    newChange(outState, deleteAction(), httpStatus, message, request.body)
  }

  /**
   *
   * NOTE : the out state name should have been in the form : task.<service>.<resource>.<action>.<final_state>
   *
   **/
  private def newChange(
      outStateName : String,
      action: ApiAction,
      httpStatus: Int = HttpStatus.ACCEPTED,
      message: Option[String] = None,
      request_data: JsValue ) : ChangeMessage = {

    val pendingState = getPendingState( outStateName )
    val changeData = (request_data \ "change_management").validate[ChangeInfo] match {
      case JsSuccess( s, _ ) => s
      case err@JsError(_) => {
        throw new Exception( "Error reading \"change_management\" information from payload : " + JsError.toFlatJson(err).toString() )
      }
    }

    val event = ApiTaskEvent( pendingState, ApiTask.make(TaskStatus.Pending, action, request_data = Some(request_data) /*Option(request.body)*/ ))
    val change = new ChangeDao(
      deadline = changeData.deadline,
      taskUUID = event.asEvent.uuid,
      outStateName = outStateName,
      reason = Some( changeData.reason),
      schedule = Some( changeData.schedule ),
      dependencies = changeData.dependencies,
      payload = Some(request_data.toString),
      statusText = None
    )

    new ChangeMessage( change = change, event = event )
  }

  private def getPendingState( outState : String ) : String = {
    val parts = outState.split( "\\." )
    if( parts.size < 5 )
    {
      log.error( "ERROR : out state names should be in the form : task.<service>.<resource>.<action>.<final_state>" )
      throw new Exception( "ERROR : out state names should be in the form : task.<service>.<resource>.<action>.<final_state>" )
    }

    val pendingState = parts.slice(0, parts.size - 1).mkString( "." ) + ".pending"
    log.debug( "pending state : " + pendingState )
    pendingState
  }
  
  
  
  private def postAction(
      httpStatus: Int = HttpStatus.ACCEPTED, 
      resource: Option[String] = None,
      message: Option[String] = defaultPost, 
      errors: Seq[ApiError] = Seq())(implicit request: RequestHeader) = {
    apiAction(
        HttpVerbs.POST, httpStatus, 
        resource, 
        message, errors )
  }
  
  private def putAction(
      httpStatus: Int = HttpStatus.ACCEPTED,  
      resource: Option[String] = None,
      message: Option[String] = defaultPut, 
      errors: Seq[ApiError] = Seq())(implicit request: RequestHeader) = {
    apiAction(
        HttpVerbs.PUT, httpStatus, 
        resource, 
        message, errors )
  }
  
  private def patchAction(
      httpStatus: Int = HttpStatus.ACCEPTED, 
      resource: Option[String] = None, 
      message: Option[String] = defaultPatch, 
      errors: Seq[ApiError] = Seq())(implicit request: RequestHeader) = {
    apiAction(
        HttpVerbs.PATCH, httpStatus, 
        resource, 
        message, errors )
  }
  
  private def deleteAction(
      httpStatus: Int = HttpStatus.ACCEPTED, 
      resource: Option[String] = None, 
      message: Option[String] = defaultDelete, 
      errors: Seq[ApiError] = Seq())(implicit request: RequestHeader) = {
    apiAction(
        HttpVerbs.DELETE, httpStatus, 
        resource, 
        message, errors )
  }  
  
  private def apiAction(
      verb: String, 
      httpStatus: Int, 
      resource: Option[String], 
      message: Option[String], 
      errors: Seq[ApiError])(implicit request: RequestHeader) = {
    ApiAction(
        verb, 
        Some(httpStatus.toString), 
        resource = (if (resource.isEmpty) Some(route()) else resource), 
        message  = message,
        errors   = (if (errors.isEmpty) None else Option(errors)) )
  }

  def route(secure: Boolean = false)(implicit request: RequestHeader) = {
    def protocol = { if (secure) "https" else "http" } 
    "%s://%s%s".format(protocol, request.host, request.uri)
  }  
  
  private val defaultPost   = Some("Creating Resource.")
  private val defaultPut    = Some("Updating Resource.")
  private val defaultPatch  = Some("Patching Resource.")
  private val defaultDelete = Some("Deleting Resource.")
  
  object HttpVerbs {
    val GET     = "GET"
    val POST    = "POST"
    val PUT     = "PUT"
    val PATCH   = "PATCH"
    val DELETE  = "DELETE"
    val HEAD    = "HEAD"
    val OPTIONS = "OPTION"
    val TRACE   = "TRACE"
    val CONNECT = "CONNECT"
  }
  
  object HttpStatus {
    val OK          = 200
    val FULFILLED   = 201
    val ACCEPTED    = 202
    val BAD_REQUEST = 400
    val REFUSED     = 403
    val NOT_FOUND   = 404    
    val CONFLICT    = 409
  }
  
  private def current1 = Thread.currentThread.getStackTrace()(2).getMethodName
  private def current2 = new Exception().getStackTrace().apply(1).getMethodName()
  
  
}