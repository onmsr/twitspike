package jp.co.dwango.twitspike.controllers

import jp.co.dwango.twitspike.services.AerospikeService
import jp.co.dwango.twitspike.services.TweetService
import jp.co.dwango.twitspike.validations.TweetRequestDataConstraint
import jp.co.dwango.twitspike.exceptions.TwitSpikeException
import jp.co.dwango.twitspike.actions.UserAction
import play.api.libs.json.Json
import scala.util.control.Exception.allCatch
import jp.co.dwango.twitspike.exceptions.TwitSpikeException.writes

/**
 * TweetController
 * 
 * ツイート関連のAPIを管理するコントローラー
 */
class TweetController extends BaseController {

  def post() = UserAction(parse.json) { implicit request =>
    (for {
      sessionKey <- request.sessionKey.toRight(
        new TwitSpikeException(SESSION_KEY_REQUIRED_ERROR, sessionKeyRequiredErrorMessage)).right
      user <- request.user.toRight(
        new TwitSpikeException(AUTH_USER_NOT_FOUND_ERROR, userNotFoundErrorMessage)).right
      client <- AerospikeService.getClient.right
      tweet <- getRequestData(TweetRequestDataConstraint.post).right
      id <- new TweetService(client).create(user.id, tweet.content).right
      _ <- (allCatch either client.close).right
    } yield id) match {
      case Left(e) => {
        e match {
          case TwitSpikeException(SESSION_KEY_REQUIRED_ERROR, _) => BadRequest(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case TwitSpikeException(AUTH_USER_NOT_FOUND_ERROR, _) => NotFound(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case TwitSpikeException(VALIDATIONS_ERROR, _) => BadRequest(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case _ => InternalServerError(Json.obj("error" -> internalServerErrorMessage))
        }
      }
      case Right(id) => Created(Json.obj("id" -> id))
    }
  }

  def delete(tweetId: Long) = TODO
  def get(tweetId: Long) = TODO

}
