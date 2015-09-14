package jp.co.dwango.twitspike.services

import com.aerospike.client.{AerospikeClient, Bin}
import java.util.UUID
import jp.co.dwango.twitspike.models.User
import jp.co.dwango.twitspike.exceptions.TwitSpikeException
import jp.co.dwango.twitspike.exceptions.TwitSpikeExceptionTrait
import jp.co.dwango.twitspike.controllers.TSMsgTrait
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.mindrot.jbcrypt.BCrypt
import scala.util.control.Exception.allCatch

/**
 * UserService
 *
 * ユーザーサービス
 *
 */
class UserService(_client: AerospikeClient)
    extends TSAerospikeService
    with TwitSpikeExceptionTrait
    with TSMsgTrait {

  val client = _client

  /**
   * 次のユーザーIDを取得する
   * 
   * @return 新しいユーザーID
   */
  def nextId = createNextId(client, ns, "user_last_id")

  /**
   * ユーザーレコードを作成する
   * 
   * @param name          ユーザー名
   * @param nickname      ユーザーのニックネーム
   * @param email         メールアドレス
   * @param description   ユーザーの自己紹介文
   * @param rawPassword   ユーザーのパスワード
   * @return
   */
  def create(name: String, nickname: String, email: String, description: String, rawPassword: String) = {
    // メールアドレスがすでに利用されているか確認
    val checkEmailAlreadyUsed = read(client, getAuthenticationsKey(email)) match {
      case Some(authInfo) => Left(new TwitSpikeException(EMAIL_ALREADY_REGISTERD_ERROR, emailAlreadyRegisterdErrorMessage))
      case None => Right(true)
    }

    for {
      _ <- checkEmailAlreadyUsed.right
      id <- nextId.right
      password <- Right(BCrypt.hashpw(rawPassword, BCrypt.gensalt(12))).right
      _ <- createUser(id, name, nickname, email, description).right
      _ <- createAuthentication(id, email, password).right
      _ <- createNickname(id, nickname).right
    } yield id
  }

  /**
   * ユーザーを取得する。ユーザーレコードを取得しUserオブジェクトを生成する。
   * 
   * @param userId ユーザーID
   * @return Userオブジェクト
   */
  def findOneById(userId: Long): Option[User] = {
    val key = getUsersKey(userId)
    readAsMap(client, key).map { User.userRecordMapToUser(_) }
  }

  /**
   * ユーザーを取得する。ユーザーレコードを取得しUserオブジェクトを生成する。
   * 
   * @param userIds ユーザーIDのリスト
   * @return Userオブジェクトのリスト
   */
  def findByIds(userIds: List[Long]): List[User] = {
    val keys = getUsersKeys(userIds)
    readAsMap(client, keys).map { User.userRecordMapToUser(_) }
  }

  /**
   * ユーザーを削除する
   * 
   * @param userId ユーザーID
   * @return
   */
  def delete(userId: Long) = {
    remove(client, getUsersKey(userId))
  }

  def find() = {
    // @TODO
    // get users from user set
  }

  /**
   * ユーザーのフォロワーID一覧を取得する
   *
   * @param userId ユーザーID
   * @return
   */
  def findFanIds(userId: Long) = {
    val fans = client.getLargeList(wPolicy, getFansKey(userId), "fans")
    scanLargeList(fans).asInstanceOf[List[Long]]
  }

  /**
   * ユーザーのフォロイーID一覧を取得する
   *
   * @param userId ユーザーID
   * @return
   */
  def findCelebIds(userId: Long) = {
    val celebs = client.getLargeList(wPolicy, getCelebsKey(userId), "celebs")
    scanLargeList(celebs).asInstanceOf[List[Long]]
  }

  /**
   * ユーザーのタイムラインを取得する。
   *
   * @param userId ユーザーID
   * @return
   */
  def findTimeline(userId: Long, count: Int, start: Option[Long], endFilter: Option[Long]) = {
    val s = start.getOrElse(System.currentTimeMillis)
    val e = endFilter.getOrElse(0L)
    for {
      timeline <- getLargeList(client, wPolicy, getTimelinesKey(userId), "timeline").right
      tweetMaps <- findRecordsFromLargeList(timeline, -s, count).right
      filteredTweetMaps <- Right(tweetMaps.filter(_.get("key").get.asInstanceOf[Long] <= -e)).right
      tweetIds <- Right(filteredTweetMaps.map(_.get("tweetId").get.asInstanceOf[Long])).right
      tweets <- Right(new TweetService(client).findByIds(tweetIds)).right
    } yield tweets
  }

  /**
   * ユーザーのタイムラインを取得する。
   *
   * @param userId ユーザーID
   * @return
   */
  def findTimelineAll(userId: Long) = {
    for {
      timeline <- getLargeList(client, wPolicy, getTimelinesKey(userId), "timeline").right
      tweetIds <- Right(scanLargeList(timeline).map { _.get("tweetId").get.asInstanceOf[Long] }).right
      tweets <- Right(new TweetService(client).findByIds(tweetIds)).right
    } yield tweets
  }

  /**
   * ユーザーのタイムラインを取得する。愚直に全部合成版
   *
   * @param userId ユーザーID
   * @return
   */
  def findTimelineSimple(userId: Long) = {
    val celebIds = findCelebIds(userId)
    val ts = new TweetService(client)
    val tweetIds = (userId :: celebIds).map(ts.findIds(_)).flatten
    ts.findByIds(tweetIds)
  }

  /**
   * ユーザーの認証を行う。成功した場合はセッションキーが作成される。
   * 
   * @param email       メールアドレス
   * @param rawPassword パスワード
   * @return セッションキー
   */
  def auth(email: String, rawPassword: String) = {
    (for {
      authInfo <- read(client, getAuthenticationsKey(email)).toRight(
        new TwitSpikeException(AUTH_USER_NOT_FOUND_ERROR, emailNotFoundErrorMessage)).right
      isAuth <- Right(BCrypt.checkpw(rawPassword, authInfo.getString("password"))).right
    } yield (isAuth, authInfo)) match {
      case Right((isAuth, authInfo)) => {
        if (isAuth) {
          // 認証成功 + セッションキー発行
          wPolicy.expiration = 7*86400 // 7days
          val id = authInfo.getLong("user_id")
          val sessionKey = UUID.randomUUID().toString
          val ts = new DateTime().toString(ISODateTimeFormat.dateTimeNoMillis)

          val userIdBin = new Bin("user_id", id)
          val sessionKeyBin = new Bin("sessionkey", sessionKey)
          val timestampBin = new Bin("timestamp", ts)

          write(client, getSessionkeysKey(sessionKey), Array(userIdBin, sessionKeyBin, timestampBin))
          wPolicy.expiration = 0
          Right(sessionKey)
        } else {
          Left(new TwitSpikeException(AUTH_FAILED_ERROR, authFailedErrorMessage))
        }
      }
      case Left(e) => Left(e)
    }
  }

  /**
   * セッションキーからユーザー情報を取得する。
   * 
   * @param sessionKey セッションキー
   * @return Userオブジェクト
   */
  def self(sessionKey: String) = {
    // セッションキーの更新の必要があるかも
    val key = getSessionkeysKey(sessionKey)
    read(client, key) flatMap { sessionInfo => findOneById(sessionInfo.getLong("user_id")) }
  }

  /**
   * ユーザーレコード作成
   * 
   * @param name          ユーザー名
   * @param nickname      ユーザーのニックネーム
   * @param email         メールアドレス
   * @param description   ユーザーの自己紹介文
   * @return
   */
  private[this] def createUser(id: Long, name: String, nickname: String, email: String, description: String) = {
    val ts = new DateTime().toString(ISODateTimeFormat.dateTimeNoMillis)

    val idBin = new Bin("id", id)
    val nameBin = new Bin("name", name)
    val nicknameBin = new Bin("nickname", nickname)
    val emailBin = new Bin("email", email)
    val descriptionBin = new Bin("description", description)
    val createdAtBin = new Bin("created_at", ts)
    val updatedAtBin = new Bin("updated_at", ts)

    val key = getUsersKey(id)
    write(client, key, Array(idBin, nameBin, nicknameBin, emailBin, descriptionBin, createdAtBin, updatedAtBin))
  }

  /**
   * 認証情報の作成
   * 
   * @param userId    ユーザーID
   * @param email     メールアドレス
   * @param password  ハッシュ化されたパスワード
   * @return
   */
  private[this] def createAuthentication(userId: Long, email: String, password: String) = {
    val passwordBin = new Bin("password", password)
    val userIdBin = new Bin("user_id", userId)
    val key = getAuthenticationsKey(email)
    write(client, key, Array(passwordBin, userIdBin))
  }

  /**
   * ニックネーム情報の作成
   * 
   * @param userId    ユーザーID
   * @param nickname  ニックネーム
   * @return
   */
  private[this] def createNickname(userId: Long, nickname: String) = {
    val userIdBin = new Bin("user_id", userId)
    val key = getNicknamesKey(nickname)
    write(client, key, Array(userIdBin))
  }

  /**
   * タイムラインのツイート数がN+MARGIN件を超えていればN件まで古いツイートを削除する
   *
   * @param userId
   * @param n
   * @param margin
   */
  def sweepTimeline(userId: Long, n: Int, margin: Int) = {
    (for {
      timeline <- getLargeList(client, wPolicy, getTimelinesKey(userId), "timeline").right
      size <- (allCatch either timeline.size).right
      _ <- (if (size < n + margin) Left(false) else Right(true)).right // check size > n+margin
      records <- (allCatch either timeline.findLast(size-n)).right // get remove data
      _ <- (allCatch either timeline.remove(records)).right // remove data
    } yield true) match {
      case Left(e) => {
        e match {
          case false => Right(false)
          case _ => Left(e)
        }
      }
      case Right(result) => Right(result)
    }
  }

}
