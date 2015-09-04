package jp.co.dwango.twitspike.controllers

import jp.co.dwango.twitspike.models.User
import jp.co.dwango.twitspike.models.response.UserResponseData
import jp.co.dwango.twitspike.services.AerospikeService
import jp.co.dwango.twitspike.services.UserService
import jp.co.dwango.twitspike.validations.UserRequestDataConstraint
import jp.co.dwango.twitspike.exceptions.TwitSpikeException
import jp.co.dwango.twitspike.actions.UserAction
import play.api.libs.json.Json
import play.api.mvc.Action
import scala.util.control.Exception.allCatch
import jp.co.dwango.twitspike.exceptions.TwitSpikeException.writes

/**
 * UserController
 *
 * ユーザー関連のAPIを管理するコントローラー
 */
class UserController extends BaseController {

  def index = TODO

  def post = Action(parse.json) { implicit request =>
    (for {
      client <- AerospikeService.getClient.right
      u <- getRequestData(UserRequestDataConstraint.post).right
      id <- new UserService(client).create(u.name, u.nickname, u.email, u.description, u.rawPassword).right
      _ <- (allCatch either client.close).right
    } yield id) match {
      case Left(e) => {
        e match {
          case TwitSpikeException(VALIDATIONS_ERROR, _) => BadRequest(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case _ => InternalServerError(Json.obj("error" -> internalServerErrorMessage))
        }
      }
      case Right(id) => Created(Json.obj("id" -> id))
    }
  }

  def delete(userId: Long) = UserAction { implicit request =>
    (for {
      sessionKey <- request.sessionKey.toRight(
        new TwitSpikeException(SESSION_KEY_REQUIRED_ERROR, sessionKeyRequiredErrorMessage)).right
      user <- request.user.toRight(
        new TwitSpikeException(AUTH_USER_NOT_FOUND_ERROR, userNotFoundErrorMessage)).right
    } yield user) match {
      case Left(e) => {
        e match {
          case TwitSpikeException(SESSION_KEY_REQUIRED_ERROR, _) => BadRequest(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case TwitSpikeException(AUTH_USER_NOT_FOUND_ERROR, _) => NotFound(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case _ => InternalServerError(Json.obj("error" -> internalServerErrorMessage))
        }
      }
      case Right(user) => {
        // 削除する権限があるかどうか確認
        if (user.id == userId) {
          (for {
            client <- AerospikeService.getClient.right
            result <- new UserService(client).delete(userId).right
            _ <- (allCatch either client.close).right
          } yield result) match {
            case Left(e) => InternalServerError(Json.obj("error" -> internalServerErrorMessage))
            case Right(result) => NoContent
          }
        } else {
          Forbidden(Json.obj("error" -> permissionErrorMessage))
        }
      }
    }
  }

  def get(userId: Long) = Action { implicit request =>
    (for {
      client <- AerospikeService.getClient.right
      user <- new UserService(client).findOneById(userId).toRight(
        new TwitSpikeException(USER_NOT_FOUND_ERROR, userNotFoundErrorMessage)).right
      userResponse <- Right(getUserResponseData(user)).right
      _ <- (allCatch either client.close).right
    } yield userResponse) match {
      case Left(e) => {
        e match {
          case TwitSpikeException(USER_NOT_FOUND_ERROR, _) => NotFound(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case _ => InternalServerError(Json.obj("error" -> internalServerErrorMessage))
        }
      }
      case Right(user) => {
        import UserResponseData.writes
        Ok(Json.obj("user" -> Json.toJson(user)))
      }
    }
  }

  private[this] def getUserResponseData[A](u: User) = {
    UserResponseData(
      u.id,
      u.nickname,
      u.nickname,
      u.email,
      u.description,
      None,
      None
    )
  }

  def list() = TODO

  def auth() = Action(parse.json) { implicit request =>
    (for {
      client <- AerospikeService.getClient.right
      u <- getRequestData(UserRequestDataConstraint.auth).right
      sessionKey <- new UserService(client).auth(u.email, u.rawPassword).right
      _ <- (allCatch either client.close).right
    } yield sessionKey) match {
      case Left(e) => {
        e match {
          case TwitSpikeException(VALIDATIONS_ERROR, _) => BadRequest(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case TwitSpikeException(AUTH_FAILED_ERROR, _) => Unauthorized(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case TwitSpikeException(AUTH_USER_NOT_FOUND_ERROR, _) => NotFound(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case _ => InternalServerError(Json.obj("error" -> internalServerErrorMessage))
        }
      }
      case Right(sessionKey) => Ok(Json.obj("session_key" -> sessionKey))
    }
  }

  def self() = UserAction { implicit request =>
    (for {
      sessionKey <- request.sessionKey.toRight(
        new TwitSpikeException(SESSION_KEY_REQUIRED_ERROR, sessionKeyRequiredErrorMessage)).right
      u <- request.user.toRight(
        new TwitSpikeException(AUTH_USER_NOT_FOUND_ERROR, userNotFoundErrorMessage)).right
      user <- Right(getUserResponseData(u)).right
    } yield user) match {
      case Left(e) => {
        e match {
          case TwitSpikeException(SESSION_KEY_REQUIRED_ERROR, _) => BadRequest(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case TwitSpikeException(AUTH_USER_NOT_FOUND_ERROR, _) => NotFound(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case _ => InternalServerError(Json.obj("error" -> internalServerErrorMessage))
        }
      }
      case Right(user) => {
        import UserResponseData.writes
        Ok(Json.obj("user" -> user))
      }
    }
  }

  def fans(userId: Long) = TODO
  def celebs(userId: Long) = TODO
  def tweets(userId: Long) = TODO
  def timeline(userId: Long) = TODO

}
