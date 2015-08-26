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
