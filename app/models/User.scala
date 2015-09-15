package jp.co.dwango.twitspike.models

/**
 * User
 *
 * ユーザーモデル
 */
case class User(
  id: Long,
  name: String,
  nickname: String,
  email: String,
  description: String,
  createdAt: String,
  updatedAt: String
  )

object User {
  implicit def userRecordMapToUser[A](m: Map[String, Object]) = {
    User(
      m.get("id").getOrElse(0L).asInstanceOf[Long],
      m.get("name").getOrElse("").toString,
      m.get("nickname").getOrElse("").toString,
      m.get("email").getOrElse("").toString,
      m.get("description").getOrElse("").toString,
      m.get("created_at").getOrElse("").toString,
      m.get("updated_at").getOrElse("").toString
    )
  }
}
