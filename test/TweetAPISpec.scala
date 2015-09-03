import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.http.HeaderNames

import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import org.specs2.mutable._

/**
 * ツイート関連のAPIのテスト
 */
@RunWith(classOf[JUnitRunner])
class TweetAPISpec extends Specification {

  // @TODO サービスをモック化
  "ツイートAPI" should {

    "[POST /tweets] ツイートの登録" should {
      "入力に間違いがなくツイートが作成される" in new WithApplication{
        val sessionKey = "451e53cb-be5a-4285-b2c3-3f2e21bb4010"
        val json = Json.obj("content" -> "testtweet!!!")

        val req = FakeRequest(POST, "/tweets")
          .withHeaders(HeaderNames.COOKIE -> ("TS-Session-Key=" + sessionKey))
          .withHeaders("Content-Type" -> "application/json")
          .withBody(json)

        val fres = route(req).get

        status(fres) must equalTo(CREATED)
        contentType(fres) must beSome("application/json")
        contentAsString(fres) must contain ("id")
      }

    }
  }
}

