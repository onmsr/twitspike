package jp.co.dwango.twitspike.models

/**
 * User
 *
 * ユーザーモデル
 */
case class User(
  id: String,
  name: String,
  nickname: String,
  email: String,
  description: String,
  fanIds: Option[List[String]],
  celebIds: Option[List[String]],
  tweetIds: Option[Map[String, String]],
  fans: Option[List[User]],
  celebs: Option[List[User]],
  tweets: Option[List[Tweet]],
  createdAt: String,
  updatedAt: String
)
