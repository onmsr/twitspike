package jp.co.dwango.twitspike.controllers

import jp.co.dwango.twitspike.exceptions.TwitSpikeExceptionTrait
import jp.co.dwango.twitspike.exceptions.TwitSpikeException
import play.api.mvc.Controller
import play.api.Play
import play.api.Play.current
import play.api.data.Form
import play.api.libs.json.Json

trait TSMsgTrait {

  val validationErrorMessage = Play.configuration.getString("ts.msgs.error.validation").get
  val internalServerErrorMessage = Play.configuration.getString("ts.msgs.error.internalServerError").get
  val userNotFoundErrorMessage = Play.configuration.getString("ts.msgs.error.userNotFound").get
  val sessionKeyRequiredErrorMessage = Play.configuration.getString("ts.msgs.error.sessionkeyRequired").get
  val emailNotFoundErrorMessage = Play.configuration.getString("ts.msgs.error.emailNotFound").get
  val authFailedErrorMessage = Play.configuration.getString("ts.msgs.error.authFailed").get
  val permissionErrorMessage = Play.configuration.getString("ts.msgs.error.permissionError").get
  val tweetNotFoundErrorMessage = Play.configuration.getString("ts.msgs.error.tweetNotFound").get

}

/**
 * BaseController
 * 
 * APIコントローラーの基底クラス
 */
class BaseController extends Controller with TSMsgTrait with TwitSpikeExceptionTrait {

  def getRequestData[T](form: Form[T])(implicit request: play.api.mvc.Request[_]): Either[Exception, T] = {
    val param = form.bindFromRequest
    if (param.hasErrors || param.hasGlobalErrors) {
      Left(new TwitSpikeException(VALIDATIONS_ERROR, validationErrorMessage))
    } else {
      Right(param.value.get)
    }
  }

}
