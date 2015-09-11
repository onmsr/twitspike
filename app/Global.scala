package jp.co.dwango.twitspike.globals

import scala.concurrent.Future

import jp.co.dwango.twitspike.actions.UserAction
import jp.co.dwango.twitspike.controllers.TSMsg
import jp.co.dwango.twitspike.exceptions.TwitSpikeException
import jp.co.dwango.twitspike.filters.AccessLoggingFilter
import play.Play
import play.api.libs.json.Json
import play.api.mvc.{Handler, RequestHeader, Results, WithFilters}
import play.api.{Application, GlobalSettings, Logger}

object GlobalInfo {
  val debug = Play.application().configuration.getBoolean("ts.debug")
  val serverUrl = Play.application().configuration.getString("ts.asServer1")
}

class BaseGlobal extends WithFilters(AccessLoggingFilter) with GlobalSettings {
}

object Global extends BaseGlobal {

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    Logger.info("executed before every request:" + request.toString)
    super.onRouteRequest(request)
  }

  override def onStart(app: Application) {
    Logger.info("Application has started")
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }

  override def onError(request: RequestHeader, e: Throwable) = {
    Logger.info("internal server error")
    Future.successful(Results.InternalServerError(
      Json.obj("error" -> Json.toJson(new TwitSpikeException(TwitSpikeException.INTERNAL_SERVER_ERROR, TSMsg.internalServerErrorMessage)))
    ))
  }

  override def onHandlerNotFound(request: RequestHeader) = {
    val sessionKey = UserAction.getSessionKeyFromRequest(request).getOrElse("")
    val user = UserAction.getUserFromSessionKey(sessionKey)
    Logger("error").error("[HandlerNotFound] path : " + request.path + ", session key : " + sessionKey + ", user : " + user.toString)
    if (GlobalInfo.debug) {
      super.onHandlerNotFound(request)
    } else {
      Future.successful(Results.NotFound(
        Json.obj("error" -> Json.toJson(new TwitSpikeException(TwitSpikeException.NOT_FOUND_ERROR, TSMsg.notFoundErrorMessage)))
      ))
    }
  }

  override def onBadRequest(request: RequestHeader, error: String) = {
    val sessionKey = UserAction.getSessionKeyFromRequest(request).getOrElse("")
    val user = UserAction.getUserFromSessionKey(sessionKey)
    Logger("error").error("[HandlerNotFound] path : " + request.path + ", session key : " + sessionKey + ", user : " + user.toString)
    if (GlobalInfo.debug) {
      super.onHandlerNotFound(request)
    } else {
      Future.successful(Results.BadRequest(
        Json.obj("error" -> Json.toJson(new TwitSpikeException(TwitSpikeException.BAD_REQUEST_ERROR, TSMsg.badRequestErrorMessage)))
      ))
    }
  }

}
