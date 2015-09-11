package jp.co.dwango.twitspike.exceptions

import play.api.libs.json.Json
import play.api.libs.json.Writes

/**
 * TwitSpikeException
 *
 * @param code エラーコード
 * @param message エラーメッセージ
 */
case class TwitSpikeException(code: Int, message: String) extends RuntimeException

/**
 * TwitSpikeでのAPIのエラーコード一覧
 * 1000番台は汎用的なエラー, 2000番台はクライアントエラー
 */
trait TwitSpikeExceptionTrait {
  /**
   * ユーザーが見つからない
   */  
  val USER_NOT_FOUND_ERROR = 1000

  /**
   * 指定したURLが見つからない
   */
  val NOT_FOUND_ERROR = 1001

  /**
   * リクエストが間違っている
   */
  val BAD_REQUEST_ERROR = 1002

  /**
   * サーバーサイドエラー
   */
  val TS_INTERNAL_SERVER_ERROR = 1003

  /**
   * バリデーションエラー
   */  
  val VALIDATIONS_ERROR = 2000
   
  /**
   * セッションキーを指定していない
   */
  val SESSION_KEY_REQUIRED_ERROR = 2001

  /**
   * 認証ユーザーが見つからない
   */
  val AUTH_USER_NOT_FOUND_ERROR = 2002

  /**
   * 認証に失敗した。認証ユーザーは存在するが、パスワードが一致しない
   */
  val AUTH_FAILED_ERROR = 2002

  /**
   * ツイートが見つからない
   */
  val TWEET_NOT_FOUND_ERROR = 1000

  /**
   * すでにフォローしている
   */
  val ALREADY_FOLLOW_ERROR = 1000
}

object TwitSpikeException extends TwitSpikeExceptionTrait {
  implicit val writes = new Writes[TwitSpikeException] {
    def writes(e: TwitSpikeException) = Json.obj(
      "code" -> e.code,
      "message" -> e.message
    )
  }
}
