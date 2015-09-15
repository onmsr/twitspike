package jp.co.dwango.twitspike.actions

import scala.concurrent.Future
import scala.util.control.Exception.allCatch

import jp.co.dwango.twitspike.models.User
import jp.co.dwango.twitspike.services.{AerospikeService, UserService}

import play.api.mvc._

class UserRequest[A](val user: Option[User], val sessionKey: Option[String], request: Request[A]) extends WrappedRequest[A](request)

object UserAction extends ActionBuilder[UserRequest] with ActionTransformer[Request, UserRequest] {

  def getSessionKeyFromRequest[A](implicit request: RequestHeader) = request.cookies.get("TS-Session-Key").map(_.value)

  def getUserFromSessionKey(sessionKey: String) = {
    (for {
      client <- AerospikeService.getClient.right
      user <- Right(new UserService(client).self(sessionKey)).right
      _ <- (allCatch either client.close).right
    } yield user) match {
      case Left(e) => None
      case Right(user) => user
    }
  }

  def transform[A](request: Request[A]) = Future.successful {
    val sessionKey = getSessionKeyFromRequest(request)
    new UserRequest(getUserFromSessionKey(sessionKey.getOrElse("")), sessionKey, request)
  }

}
