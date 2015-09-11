package jp.co.dwango.twitspike.controllers

import com.aerospike.client.AerospikeClient
import jp.co.dwango.twitspike.models.User
import jp.co.dwango.twitspike.models.Tweet
import jp.co.dwango.twitspike.models.response.UserResponseData
import jp.co.dwango.twitspike.models.response.TweetResponseData
import jp.co.dwango.twitspike.services.{TweetService, AerospikeService, UserService}
import jp.co.dwango.twitspike.utils.TimeUtil
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

  /**
   * ユーザーの登録を行う
   *
   * @return
   */
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

  /**
   * ユーザーを削除する
   *
   * @param userId 削除するユーザーのID
   * @return
   */
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

  /**
   * ユーザー情報の取得
   *
   * @param userId 取得したいユーザーのID
   * @return
   */
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

  /**
   * ユーザー一覧を取得する
   *
   * @return
   */
  def list() = TODO

  /**
   * メールアドレスとパスワードからユーザーの認証を行う。成功した場合にセッションキーを発行する
   *
   * @return
   */
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

  /**
   * 認証ユーザーの情報を取得する
   *
   * @return
   */
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

  /**
   * 指定したユーザーのタイムラインを取得する
   *
   * @return
   */
  def timeline(userId: Long, count: Int, cursor: Option[Long], maxId: Option[Long], sinceId: Option[Long], until: Option[String], since: Option[String]) = Action { implicit request =>
    (for {
      client <- AerospikeService.getClient.right
      user <- new UserService(client).findOneById(userId).toRight(
        new TwitSpikeException(USER_NOT_FOUND_ERROR, userNotFoundErrorMessage)).right
      query <- buildQuery(client, count, cursor, maxId, sinceId, until, since).right
      tweets <- new UserService(client).findTimeline(userId, query._1, query._2, query._3).right
      users <- Right(new UserService(client).findByIds(getTweetsUserIds(tweets))).right
      timeline <- Right(getTimelineResponseData(tweets zip users)).right
      _ <- (allCatch either client.close).right
    } yield timeline) match {
      case Left(e) => {
        e match {
          case TwitSpikeException(USER_NOT_FOUND_ERROR, _) => NotFound(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case _ => InternalServerError(Json.obj("error" -> internalServerErrorMessage))
        }
      }
      case Right(timeline) => Ok(Json.obj("timeline" -> timeline))
    }
  }

  private[this] def getCreatedAtByTweetId(client: AerospikeClient, tweetId: Long) = {
    new TweetService(client).findOneById(tweetId).map { _.createdAt }
  }

  /**
   * 検索の取得個数と開始位置と終了位置を生成する
   * cursor・maxId・sinceIdはツイートID, until・sinceはタイムスタンプ
   * 基本的にスタート位置がありそこからcount件取得して終了条件からフィルターされるかんじ
   *
   * @param client
   * @param count
   * @param cursor
   * @param maxId
   * @param sinceId
   * @param until
   * @param since
   * @return (取得件数, 開始位置, 終了位置)
   */
  private[this] def buildQuery(client: AerospikeClient, count: Int, cursor: Option[Long], maxId: Option[Long], sinceId: Option[Long], until: Option[String], since: Option[String]) = {
    // since and until -> milli second
    val sinceMillis = since.flatMap { TimeUtil.timestampToMillis(_) }
    val untilMillis = until.flatMap { TimeUtil.timestampToMillis(_) }

    // sinceId and maxId -> tweet createdAt timestamp -> milli second
    val sinceIdMillis = sinceId.flatMap { getCreatedAtByTweetId(client, _) } flatMap { TimeUtil.timestampToMillis(_) }
    val maxIdMillis = maxId.flatMap { getCreatedAtByTweetId(client, _) } flatMap { TimeUtil.timestampToMillis(_) }

    val starts = List(cursor, maxIdMillis, untilMillis).flatMap { v => v }
    val start = allCatch opt starts.head
    val endFilters = List(sinceIdMillis, sinceMillis).flatMap { v => v }
    val endFilter = allCatch opt endFilters.head

    if (count >= 0 && count <= 200) {
      Right((count, start, endFilter))
    } else {
      Left(new TwitSpikeException(VALIDATIONS_ERROR, "取得件数の指定が間違っています"))
    }
  }

  /**
   *  ツイート一覧からユーザーID一覧を取得
   */
  private[this] def getTweetsUserIds(tweets: List[Tweet]) = tweets.map { _.userId }

  /**
   * ツイートレスポンスデータの作成
   *
   * @return
   */
  private[this] def getTimelineResponseData[A](timeline: List[(Tweet, User)]) = {
    timeline.map { case (t, u) =>
      TweetResponseData(
        t.id,
        t.content,
        t.createdAt,
        u.id,
        u.nickname
      )
    }
  }

  /**
   * ユーザーレスポンスデータの作成
   *
   * @param u ユーザー
   * @tparam A
   * @return
   */
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

}
