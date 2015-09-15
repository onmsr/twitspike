package jp.co.dwango.twitspike.controllers

import scala.util.control.Exception.allCatch

import jp.co.dwango.twitspike.actions.UserAction
import jp.co.dwango.twitspike.exceptions.TwitSpikeException
import jp.co.dwango.twitspike.exceptions.TwitSpikeException.writes
import jp.co.dwango.twitspike.services.{AerospikeService, FollowService}
import play.api.libs.json.Json

/**
 * FollowController
 *
 * フォロー関連のAPIを管理するコントローラー
 */
class FollowController extends BaseController {

  /**
   * 認証ユーザーが指定したユーザーをフォローする
   *
   * @param targetUserId フォローしたいユーザーのID
   * @return
   */
  def post(targetUserId: Long) = UserAction { implicit request =>
    (for {
      sessionKey <- request.sessionKey.toRight(
        new TwitSpikeException(SESSION_KEY_REQUIRED_ERROR, sessionKeyRequiredErrorMessage)).right
      user <- request.user.toRight(
        new TwitSpikeException(AUTH_USER_NOT_FOUND_ERROR, userNotFoundErrorMessage)).right
      client <- AerospikeService.getClient.right
      _ <- new FollowService(client).create(user.id, targetUserId).right
      _ <- (allCatch either client.close).right
    } yield targetUserId) match {
      case Left(e) => {
        e match {
          case TwitSpikeException(SESSION_KEY_REQUIRED_ERROR, _) => BadRequest(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case TwitSpikeException(AUTH_USER_NOT_FOUND_ERROR, _) => NotFound(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case TwitSpikeException(ALREADY_FOLLOW_ERROR, _) => BadRequest(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case _ => InternalServerError(Json.obj("error" -> internalServerErrorMessage))
        }
      }
      case Right(id) => Created(Json.obj("id" -> id))
    }
  }

  /**
   * 認証ユーザーの指定したユーザーのフォローを解除する
   *
   * @param targetUserId フォローを解除したいユーザーのID
   * @return
   */
  def delete(targetUserId: Long) = UserAction { implicit request =>
    (for {
      sessionKey <- request.sessionKey.toRight(
        new TwitSpikeException(SESSION_KEY_REQUIRED_ERROR, sessionKeyRequiredErrorMessage)).right
      user <- request.user.toRight(
        new TwitSpikeException(AUTH_USER_NOT_FOUND_ERROR, userNotFoundErrorMessage)).right
      client <- AerospikeService.getClient.right
      _ <- new FollowService(client).delete(user.id, targetUserId).right
      _ <- (allCatch either client.close).right
    } yield targetUserId) match {
      case Left(e) => {
        e match {
          case TwitSpikeException(SESSION_KEY_REQUIRED_ERROR, _) => BadRequest(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case TwitSpikeException(AUTH_USER_NOT_FOUND_ERROR, _) => NotFound(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case _ => InternalServerError(Json.obj("error" -> internalServerErrorMessage))
        }
      }
      case Right(id) => NoContent
    }
  }

}
