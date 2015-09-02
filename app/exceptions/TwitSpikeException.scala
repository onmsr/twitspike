package jp.co.dwango.twitspike.exceptions

case class TwitSpikeException(statusCode: Int, message: String) extends RuntimeException

object TwitSpikeException {
  /**
   * ユーザーが見つからない
   */  
  val USER_NOT_FOUND_ERROR = 1000

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
  val AUTH_FAILED__ERROR = 2002

}
