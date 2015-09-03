package jp.co.dwango.twitspike.models.response

import play.api.libs.json.Json
import play.api.libs.json.Writes

case class TweetResponseData(
  id: Long,
  content: String,
  createdAt: String,
  userId: Long,
  userNickname: String
)

object TweetResponseData {
  implicit val writes = new Writes[TweetResponseData] {
    def writes(tweet: TweetResponseData) = Json.obj(
      "id" -> tweet.id,
      "content" -> tweet.content,
      "created_at" -> tweet.createdAt,
      "user_id" -> tweet.userId,
      "user_nickname" -> tweet.userNickname
    )
  }
}
