package jp.co.dwango.twitspike.controllers

import play.api.mvc.Controller
import play.api.Play
import play.api.Play.current

trait TSMsgTrait {
  val validationErrorMessage = Play.configuration.getString("ts.msgs.error.validation").get
  val internalServerErrorMessage = Play.configuration.getString("ts.msgs.error.internalServerError").get
  val userNotFoundErrorMessage = Play.configuration.getString("ts.msgs.error.userNotFound").get
  val sessionKeyRequiredErrorMessage = Play.configuration.getString("ts.msgs.error.sessionkeyRequired").get
  val emailNotFoundErrorMessage = Play.configuration.getString("ts.msgs.error.emailNotFound").get
  val authFailedErrorMessage = Play.configuration.getString("ts.msgs.error.authFailed").get
  val permissionErrorMessage = Play.configuration.getString("ts.msgs.error.permissionError").get

}

/**
 * BaseController
 * 
 * APIコントローラーの基底クラス
 */
class BaseController extends Controller with TSMsgTrait {
}
