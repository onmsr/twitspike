package jp.co.dwango.twitspike.globals

import play.api.{Application, Logger, GlobalSettings}
import play.api.mvc.{Handler, RequestHeader}


object Global extends GlobalSettings {

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

  /*
   override def onError(request: RequestHeader, e: Throwable) = {
   }

   override def onHandlerNotFound(request: RequestHeader) = {
   }

   override def onBadRequest(request: RequestHeader, error: String) = {
   }
   */

}
