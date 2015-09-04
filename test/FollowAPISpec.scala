import org.specs2.runner._
import org.junit.runner._
import play.api.http.HeaderNames

import play.api.test._
import play.api.test.Helpers._
import org.specs2.mutable._

/**
 * フォロー関連のAPIのテスト
 */
@RunWith(classOf[JUnitRunner])
class FollowAPISpec extends Specification {

  // @TODO サービスをモック化
  "フォローAPI" should {

    "[POST /follows] フォロー作成" should {
      "入力に間違いがなくフォローが作成される" in new WithApplication{
        val sessionKey = "451e53cb-be5a-4285-b2c3-3f2e21bb4010"

        val req = FakeRequest(POST, "/follows/2")
          .withHeaders(HeaderNames.COOKIE -> ("TS-Session-Key=" + sessionKey))

        val fres = route(req).get

        status(fres) must equalTo(CREATED)
        contentType(fres) must beSome("application/json")
        contentAsString(fres) must contain ("id")
      }
    }

    "[DELETE /follows/:followId] フォローの解除" should {
      "正常にフォローが解除される" in new WithApplication{
        val sessionKey = "451e53cb-be5a-4285-b2c3-3f2e21bb4010"

        val req = FakeRequest(DELETE, "/follows/2")
          .withHeaders(HeaderNames.COOKIE -> ("TS-Session-Key=" + sessionKey))

        val fres = route(req).get

        status(fres) must equalTo(NO_CONTENT)
        contentType(fres) must beNone
      }
    }
    
  }
}

