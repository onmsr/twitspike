package jp.co.dwango.twitspike.actions

import jp.co.dwango.twitspike.models.User
import jp.co.dwango.twitspike.services.UserService
import jp.co.dwango.twitspike.services.AerospikeService
import play.api.mvc._
import scala.util.control.Exception.allCatch

import scala.concurrent.Future

class UserRequest[A](val user: Option[User], val sessionKey: Option[String], request: Request[A]) extends WrappedRequest[A](request)

object UserAction extends ActionBuilder[UserRequest] with ActionTransformer[Request, UserRequest] {

  def getSessionKeyFromRequest[A](implicit request: RequestHeader) = request.cookies.get("TS-Session-Key").map(_.value)

  def getUserFromSessionKey(sessionKey: String) = {
    (for {
      client <- Some(AerospikeService.getClient)
      user <- new UserService(client).self(sessionKey)
      _ <- (allCatch opt client.close)
    } yield user)
  }

  def transform[A](request: Request[A]) = Future.successful {
    val sessionKey = getSessionKeyFromRequest(request)
    new UserRequest(getUserFromSessionKey(sessionKey.getOrElse("")), sessionKey, request)
  }

}