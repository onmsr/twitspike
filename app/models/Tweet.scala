package jp.co.dwango.twitspike.models

/**
 * Tweet
 *
 * ツイートモデル
 */
case class Tweet(
  id: Long,
  userId: Long,
  content: String,
  createdAt: String
)
