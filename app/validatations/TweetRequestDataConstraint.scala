package jp.co.dwango.twitspike.validations

import play.api.data.Form
import play.api.data.Forms._
import jp.co.dwango.twitspike.models.request.PostTweetRequestData

object TweetRequestDataConstraint {
  val post = Form(
    mapping(
      "content" -> text(maxLength = 140)
    )(PostTweetRequestData.apply)(PostTweetRequestData.unapply)
  )
}
