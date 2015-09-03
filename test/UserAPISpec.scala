import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.http.HeaderNames

import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import org.specs2.mutable._

/**
 * ユーザー関連のAPIのテスト
 */
@RunWith(classOf[JUnitRunner])
class UserAPISpec extends Specification {

  // @TODO サービスをモック化
  "ユーザーAPI" should {

    "[POST /users] ユーザーの登録" should {
      "入力に間違いがなくユーザーが作成される" in new WithApplication{
        val json = Json.obj(
          "name" -> "testuser",
          "nickname" -> "testnickname",
          "description" -> "test",
          "email" -> "test@dwango.co.jp",
          "password" -> "1qazxsw2"
        )

        val req = FakeRequest(POST, "/users")
          .withHeaders("Content-Type" -> "application/json")
          .withBody(json)

        val fres = route(req).get

        status(fres) must equalTo(CREATED)
        contentType(fres) must beSome("application/json")
        contentAsString(fres) must contain ("id")
      }

      "登録に必要な項目が足りず、登録に失敗" in new WithApplication{
        val json = Json.obj(
          "name" -> "testuser",
          "nickname" -> "testnickname",
          "description" -> "test"
        )

        val req = FakeRequest(POST, "/users")
          .withHeaders("Content-Type" -> "application/json")
          .withBody(json)

        val fres = route(req).get

        status(fres) must equalTo(BAD_REQUEST)
        contentType(fres) must beSome("application/json")
        contentAsString(fres) must contain ("error")
      }

    }

    /*
    "[DELETE /users/:userId] ユーザーの削除" should {

      "指定したユーザーが存在し、なおかつ権限を持っていて、削除が成功する" in new WithApplication{
        val userId = 8
        val sessionKey = "1d6e0f40-9db0-4106-82c6-d28d15b65b72"
        val req = FakeRequest(DELETE, s"/users/${userId}")
          .withHeaders(HeaderNames.COOKIE -> ("TS-Session-Key=" + sessionKey))

        val fres = route(req).get

        status(fres) must equalTo(NO_CONTENT)
      }
    }
     */

    "[GET /users/:userId] ユーザーの取得" should {
      "指定したユーザーが存在して正常にユーザーが取得される" in new WithApplication{
        val userId = 1
        val req = FakeRequest(GET, s"/users/${userId}")
          .withHeaders("Content-Type" -> "application/json")

        val fres = route(req).get

        status(fres) must equalTo(OK)
        contentType(fres) must beSome("application/json")
        contentAsString(fres) must contain ("user")
      }

      " 指定したユーザーが存在せず、ユーザー取得に失敗する" in new WithApplication{
        val userId = 100000
        val req = FakeRequest(GET, s"/users/${userId}")
          .withHeaders("Content-Type" -> "application/json")

        val fres = route(req).get

        status(fres) must equalTo(NOT_FOUND)
        contentType(fres) must beSome("application/json")
        contentAsString(fres) must contain ("error")
      }

    }

    // "[GET /users] ユーザー一覧の取得" should {}

    "[POST /users/auth] ユーザーの認証" should {
      "入力に間違いがなくユーザーが認証される" in new WithApplication{
        val json = Json.obj(
          "email" -> "test@dwango.co.jp",
          "password" -> "1qazxsw2"
        )

        val req = FakeRequest(POST, "/users/auth")
          .withHeaders("Content-Type" -> "application/json")
          .withBody(json)

        val fres = route(req).get

        status(fres) must equalTo(OK)
        contentType(fres) must beSome("application/json")
        contentAsString(fres) must contain ("session_key")
      }

      "リクエストパラメータが足りずに失敗する" in new WithApplication{
        val json = Json.obj(
          "email" -> "test@dwango.co.jp"
        )

        val req = FakeRequest(POST, "/users/auth")
          .withHeaders("Content-Type" -> "application/json")
          .withBody(json)

        val fres = route(req).get

        status(fres) must equalTo(BAD_REQUEST)
        contentType(fres) must beSome("application/json")
        contentAsString(fres) must contain ("error")
      }
    }

    "[GET /users/self] 認証ユーザーの取得" should {
      "正常に認証ユーザーが取得できる" in new WithApplication{
        val sessionKey = "451e53cb-be5a-4285-b2c3-3f2e21bb4010"
        val req = FakeRequest(GET, "/users/self")
          .withHeaders(HeaderNames.COOKIE -> ("TS-Session-Key=" + sessionKey))

        val fres = route(req).get

        status(fres) must equalTo(OK)
        contentType(fres) must beSome("application/json")
        contentAsString(fres) must contain ("user")
      }
    }

    // "[GET /users/:userId/fans] ユーザーのファン一覧取得" should {}
    // "[GET /users/:userId/celebs] ユーザーのセレブ一覧取得" should {}
    // "[GET /users/:userId/tweets] ユーザーのツイート一覧取得" should {}
    
  }
}
