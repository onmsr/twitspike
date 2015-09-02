package jp.co.dwango.twitspike.models.request

case class PostUserRequestData(
  name: String,
  nickname: String,
  email: String,
  description: String,
  rawPassword: String
)
