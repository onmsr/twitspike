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

object Tweet {
  implicit def tweetRecordMapToTweet[A](m: Map[String, Object]) = {
    Tweet(
      m.get("id").getOrElse(0L).asInstanceOf[Long],
      m.get("user_id").getOrElse(0L).asInstanceOf[Long],
      m.get("content").getOrElse("").toString,
      m.get("created_at").getOrElse("").toString
    )
  }
}
