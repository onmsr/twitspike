package jp.co.dwango.twitspike.validations

import play.api.data.Form
import play.api.data.Forms._
import jp.co.dwango.twitspike.models.request.PostUserRequestData
import jp.co.dwango.twitspike.models.request.AuthRequestData

object UserRequestDataConstraint {
  val post = Form(
    mapping(
      "name" -> nonEmptyText,
      "nickname" -> nonEmptyText,
      "email" -> email,
      "description" -> text,
      "password" -> nonEmptyText
    )(PostUserRequestData.apply)(PostUserRequestData.unapply)
  )
  val auth = Form(
    mapping(
      "email" -> email,
      "password" -> nonEmptyText
    )(AuthRequestData.apply)(AuthRequestData.unapply)
  )
}
