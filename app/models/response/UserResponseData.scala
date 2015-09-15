package jp.co.dwango.twitspike.models.response

import play.api.libs.json.{Json, Writes}

case class UserResponseData(
  id: Long,
  name: String,
  nickname: String,
  email: String,
  description: String,
  tweetIds: Option[List[Long]],
  tweets: Option[Int]
  )

object UserResponseData {
  implicit val writes = new Writes[UserResponseData] {
    def writes(user: UserResponseData) = Json.obj(
      "id" -> user.id,
      "name" -> user.name,
      "nickname" -> user.nickname,
      "email" -> user.email,
      "description" -> user.description
    )
  }
}
