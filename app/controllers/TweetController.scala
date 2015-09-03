package jp.co.dwango.twitspike.controllers

import jp.co.dwango.twitspike.services.AerospikeService
import jp.co.dwango.twitspike.services.UserService
import jp.co.dwango.twitspike.services.TweetService
import jp.co.dwango.twitspike.validations.TweetRequestDataConstraint
import jp.co.dwango.twitspike.exceptions.TwitSpikeException
import jp.co.dwango.twitspike.actions.UserAction
import play.api.mvc.Action
import play.api.libs.json.Json
import scala.util.control.Exception.allCatch
import jp.co.dwango.twitspike.models.User
import jp.co.dwango.twitspike.models.Tweet
import jp.co.dwango.twitspike.exceptions.TwitSpikeException.writes
import jp.co.dwango.twitspike.models.response.TweetResponseData

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

  def delete(tweetId: Long) = UserAction { implicit request => 
    (for {
      sessionKey <- request.sessionKey.toRight(
        new TwitSpikeException(SESSION_KEY_REQUIRED_ERROR, sessionKeyRequiredErrorMessage)).right
      user <- request.user.toRight(
        new TwitSpikeException(AUTH_USER_NOT_FOUND_ERROR, userNotFoundErrorMessage)).right
      client <- AerospikeService.getClient.right
      ts <- Right(new TweetService(client)).right
      tweet <- ts.findOneById(tweetId).toRight(
        new TwitSpikeException(TWEET_NOT_FOUND_ERROR, tweetNotFoundErrorMessage)).right
    } yield (user, tweet, ts, client)) match {
      case Left(e) => {
        e match {
          case TwitSpikeException(SESSION_KEY_REQUIRED_ERROR, _) => BadRequest(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case TwitSpikeException(AUTH_USER_NOT_FOUND_ERROR, _) => NotFound(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case TwitSpikeException(TWEET_NOT_FOUND_ERROR, _) => NotFound(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case _ => InternalServerError(Json.obj("error" -> internalServerErrorMessage))
        }
      }
      case Right((user, tweet, ts, client)) => {
        // 削除する権限があるかどうか確認
        if (user.id == tweet.userId) {
          (for {
            result <- ts.delete(tweetId).right
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

  def get(tweetId: Long) = Action { implicit request =>
    (for {
      client <- AerospikeService.getClient.right
      tweet <- new TweetService(client).findOneById(tweetId).toRight(
        new TwitSpikeException(TWEET_NOT_FOUND_ERROR, tweetNotFoundErrorMessage)).right
      user <- new UserService(client).findOneById(tweet.userId).toRight(
        new TwitSpikeException(USER_NOT_FOUND_ERROR, userNotFoundErrorMessage)).right
      tweetResponse <- Right(getTweetResponseData(user, tweet)).right
      _ <- (allCatch either client.close).right
    } yield tweetResponse) match {
      case Left(e) => {
        e match {
          case TwitSpikeException(TWEET_NOT_FOUND_ERROR, _) => NotFound(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case TwitSpikeException(USER_NOT_FOUND_ERROR, _) => NotFound(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case _ => InternalServerError(Json.obj("error" -> internalServerErrorMessage))
        }
      }
      case Right(tweet) => {
        import TweetResponseData.writes
        Ok(Json.obj("tweet" -> Json.toJson(tweet)))
      }
    }
  }

  private[this] def getTweetResponseData[A](user: User, tweet: Tweet) = {
    TweetResponseData(
      tweet.id,
      tweet.content,
      tweet.createdAt,
      user.id,
      user.nickname
    )
  }

}
