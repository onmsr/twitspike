package jp.co.dwango.twitspike.controllers

import jp.co.dwango.twitspike.exceptions.{TwitSpikeException, TwitSpikeExceptionTrait}
import play.api.Play
import play.api.Play.current
import play.api.data.Form
import play.api.mvc.Controller

/**
 * エラーメッセージ一覧
 */
trait TSMsgTrait {

  val validationErrorMessage = Play.configuration.getString("ts.msgs.error.validation").get
  val internalServerErrorMessage = Play.configuration.getString("ts.msgs.error.internalServerError").get
  val userNotFoundErrorMessage = Play.configuration.getString("ts.msgs.error.userNotFound").get
  val sessionKeyRequiredErrorMessage = Play.configuration.getString("ts.msgs.error.sessionkeyRequired").get
  val emailNotFoundErrorMessage = Play.configuration.getString("ts.msgs.error.emailNotFound").get
  val authFailedErrorMessage = Play.configuration.getString("ts.msgs.error.authFailed").get
  val permissionErrorMessage = Play.configuration.getString("ts.msgs.error.permissionError").get
  val tweetNotFoundErrorMessage = Play.configuration.getString("ts.msgs.error.tweetNotFound").get
  val alreadyFollowErrorMessage = Play.configuration.getString("ts.msgs.error.alreadyFollowFound").get
  val notFoundErrorMessage = Play.configuration.getString("ts.msgs.error.notFound").get
  val badRequestErrorMessage = Play.configuration.getString("ts.msgs.error.badRequest").get
  val emailAlreadyRegisterdErrorMessage = Play.configuration.getString("ts.msgs.error.emailAlreadyRegisterd").get

}

object TSMsg extends TSMsgTrait

/**
 * BaseController
 *
 * APIコントローラーの基底クラス
 */
class BaseController extends Controller with TSMsgTrait with TwitSpikeExceptionTrait {

  /**
   * リクエストデータの取得とバリデーションを行う
   *
   * @param form フォーム
   * @param request リクエスト
   * @tparam T
   * @return
   */
  def getRequestData[T](form: Form[T])(implicit request: play.api.mvc.Request[_]): Either[Exception, T] = {
    val param = form.bindFromRequest
    if (param.hasErrors || param.hasGlobalErrors) {
      Left(new TwitSpikeException(VALIDATIONS_ERROR, validationErrorMessage))
    } else {
      Right(param.value.get)
    }
  }

}
