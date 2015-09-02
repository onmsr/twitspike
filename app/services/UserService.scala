package jp.co.dwango.twitspike.services

import com.aerospike.client.AerospikeClient
import com.aerospike.client.Bin
import com.aerospike.client.Key
import com.aerospike.client.Operation
import java.util.UUID
import jp.co.dwango.twitspike.models.User
import jp.co.dwango.twitspike.exceptions.TwitSpikeException
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.mindrot.jbcrypt.BCrypt;

/**
 * UserService
 *
 * ユーザーサービス
 *
 */
class UserService(_client: AerospikeClient) extends TSAerospikeService {

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
    for {
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
   * @return boolean
   */
  def delete(userId: Long) = {
    val key = getUsersKey(userId)
    remove(client, key)
  }

  def find() = {
    // @TODO
    // get users from user set
  }

  /**
   * ユーザーのフォロワーID一覧を取得する
   */
  def findFanIds(userId: Long) = {
    val fans = client.getLargeList(wPolicy, getFansKey(userId), "fans")
    scanLargeList(fans).asInstanceOf[List[Long]]
  }

  /**
   * ユーザーのフォロイーID一覧を取得する
   */
  def findCelebIds(userId: Long) = {
    val celebs = client.getLargeList(wPolicy, getCelebsKey(userId), "celebs")
    scanLargeList(celebs).asInstanceOf[List[Long]]
  }

  /**
   * ユーザーのタイムラインを取得する。愚直に全部合成版
   */
  def findTimeline(userId: Long) = {
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
        new TwitSpikeException(TwitSpikeException.AUTH_USER_NOT_FOUND_ERROR, "")
      ).right
      isAuth <- Right(BCrypt.checkpw(rawPassword, authInfo.getString("password"))).right
    } yield (isAuth, authInfo)) match {
      case Right((isAuth, authInfo)) => {
        if (isAuth) {
          // 認証成功 + セッションキー発行
          val id = authInfo.getLong("user_id")
          val sessionKey = UUID.randomUUID().toString
          val ts = new DateTime().toString(ISODateTimeFormat.dateTimeNoMillis)

          val userIdBin = new Bin("user_id", id)
          val sessionKeyBin = new Bin("sessionkey", sessionKey)
          val timestampBin = new Bin("timestamp", ts)

          write(client, getSessionkeysKey(sessionKey), Array(userIdBin, sessionKeyBin, timestampBin))
          Right(sessionKey)
        } else {
          Left(new TwitSpikeException(TwitSpikeException.AUTH_FAILED__ERROR, "認証に失敗しました"))
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
    read(client, key) map { sessionInfo => findOneById(sessionInfo.getLong("user_id")) } flatten
  }

  /**
   * ユーザーレコード作成
   * 
   * @param name          ユーザー名
   * @param nickname      ユーザーのニックネーム
   * @param email         メールアドレス
   * @param description   ユーザーの自己紹介文
   * @param salt          サルト値
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
   * @return boolean
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
   * @return boolean
   */
  private[this] def createNickname(userId: Long, nickname: String) = {
    val userIdBin = new Bin("user_id", userId)
    val key = getNicknamesKey(nickname)
    write(client, key, Array(userIdBin))
  }

}
