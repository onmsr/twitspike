package jp.co.dwango.twitspike.services

import com.aerospike.client.AerospikeClient
import com.aerospike.client.Bin
import com.aerospike.client.Key
import com.aerospike.client.Operation
import java.util.UUID
import jp.co.dwango.twitspike.models.User
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import org.mindrot.jbcrypt.BCrypt;

/**
 * UserService
 *
 * ユーザーサービス
 *
 */
class UserService(_client: AerospikeClient) extends AerospikeService {

  val client = _client
  val ns = "twitspike"
  val set = "users"

  /**
   * 次のユーザーIDを取得する
   * 
   * @return 新しいユーザーID
   */
  def nextId = {
    val key = new Key(ns, null, "user_last_id")
    val bin = new Bin("id", 1)
    val record = client.operate(null, key, Operation.add(bin), Operation.get())
    record.getLong("id")
  }

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
    val id = nextId
    val salt = BCrypt.gensalt
    val password = BCrypt.hashpw(rawPassword, salt)

    createUser(id, name, nickname, email, description, salt)
    createAuthentication(id, email, password)
    createNickname(id, nickname)
  }

  /**
   * ユーザーを取得する。ユーザーレコードを取得しUserオブジェクトを生成する。
   * 
   * @param userId ユーザーID
   * @return Userオブジェクト
   */
  def find(userId: Long): User = {
    implicit def userRecordMapToUser[A](m: Map[String, Object]) = {
      User(
        m.get("id").getOrElse(0).asInstanceOf[Long],
        m.get("name").getOrElse("").toString,
        m.get("nickname").getOrElse("").toString,
        m.get("email").getOrElse("").toString,
        m.get("description").getOrElse("").toString,
        None,
        None,
        None,
        None,
        None,
        None,
        m.get("created_at").getOrElse("").toString,
        m.get("updated_at").getOrElse("").toString
      )
    }
    findUser(userId)
  }

  /**
   * ユーザーを削除する
   * 
   * @param userId ユーザーID
   * @return boolean
   */
  def delete(userId: Long) = {
    val key = new Key(ns, "users", userId)
    remove(client, key)
  }

  def findAll() = {

  }

  /**
   * ユーザーの認証を行う。成功した場合はセッションキーが作成される。
   * 
   * @param email       メールアドレス
   * @param rawPassword パスワード
   * @return セッションキー
   */
  def auth(email: String, rawPassword: String) = {
    val key = new Key(ns, "authentications", email)
    val authInfo = read(client, key).get
    val isAuth = BCrypt.checkpw(rawPassword, authInfo.getString("password"))
    if (isAuth) {
      // 認証成功 + セッションキー発行
      val id = authInfo.getLong("user_id")
      val sessionKey = UUID.randomUUID().toString
      val ts = new DateTime().toString(ISODateTimeFormat.dateTimeNoMillis)

      val userIdBin = new Bin("user_id", id)
      val sessionKeyBin = new Bin("sessionkey", sessionKey)
      val timestampBin = new Bin("timestamp", ts)

      val key = new Key(ns, "sessionkeys", sessionKey)
      write(client, key, Array(userIdBin, sessionKeyBin, timestampBin))
      sessionKey
    } else {
      // 認証失敗

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
    val key = new Key(ns, "sessionkeys", sessionKey)
    read(client, key) match {
      // case None => 
      case Some(sessionInfo) => {
        val userId = sessionInfo.getLong("user_id")
        find(userId)
      }
    }
  }

  /**
   * ユーザーレコードを取得する。
   * 
   * @param userId ユーザーID
   * @return ユーザー情報のはいったMapオブジェクト
   */
  private[this] def findUser(userId: Long) = {
    val key = new Key(ns, "users", userId)
    read(client, key).map(_.bins).map(scala.collection.JavaConversions.mapAsScalaMap(_).toMap).getOrElse(Map[String, Object]())
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
  private[this] def createUser(id: Long, name: String, nickname: String, email: String, description: String, salt: String) = {
    val ts = new DateTime().toString(ISODateTimeFormat.dateTimeNoMillis)

    val idBin = new Bin("id", id)
    val nameBin = new Bin("name", name)
    val nicknameBin = new Bin("nickname", nickname)
    val emailBin = new Bin("email", email)
    val descriptionBin = new Bin("description", description)
    val saltBin = new Bin("salt", salt)
    val createdAtBin = new Bin("created_at", ts)
    val updatedAtBin = new Bin("updated_at", ts)

    val key = new Key(ns, "users", id)
    write(client, key, Array(idBin, nameBin, nicknameBin, emailBin, descriptionBin, saltBin, createdAtBin, updatedAtBin))
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
    val key = new Key(ns, "authentications", email)
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
    val key = new Key(ns, "nicknames", nickname)
    write(client, key, Array(userIdBin))
  }

}
