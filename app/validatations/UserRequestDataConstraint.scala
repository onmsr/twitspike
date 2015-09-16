package jp.co.dwango.twitspike.validations

import jp.co.dwango.twitspike.models.request.{AuthRequestData, PostUserRequestData}
import play.api.data.Form
import play.api.data.Forms._

object UserRequestDataConstraint {
  val post = Form(
    mapping(
      "name" -> nonEmptyText(maxLength = 30),
      "nickname" -> nonEmptyText(maxLength = 15),
      "email" -> email,
      "description" -> text(maxLength = 160),
      "password" -> nonEmptyText(maxLength = 30)
    )(PostUserRequestData.apply)(PostUserRequestData.unapply)
  )
  val auth = Form(
    mapping(
      "email" -> email,
      "password" -> nonEmptyText(maxLength = 30)
    )(AuthRequestData.apply)(AuthRequestData.unapply)
  )
}
