package jp.co.dwango.twitspike.services

import com.aerospike.client.Key

/**
 * TSAerospikeTrait
 *
 * TwitSpikeのAerospikeのセット情報
 *
 */
trait TSAerospikeTrait {

  val ns = "twitspike"
  val usersSet = "users"
  val authenticationsSet = "authentications"
  val sessionkeysSet = "sessionkeys"
  val nicknamesSet = "nicknames"
  val tweetsSet = "tweets"
  val userTweetsSet = "user_tweets"
  val fansSet = "fans"
  val celebsSet = "celebs"

  def getUsersKey(userId: Long) = new Key(ns, usersSet, userId)

  def getUsersKeys(userIds: List[Long]) = userIds.map { getUsersKey(_) }

  def getAuthenticationsKey(email: String) = new Key(ns, authenticationsSet, email)

  def getSessionkeysKey(sessionKey: String) = new Key(ns, sessionkeysSet, sessionKey)

  def getNicknamesKey(nickname: String) = new Key(ns, nicknamesSet, nickname)

  def getTweetsKey(tweetId: Long) = new Key(ns, tweetsSet, tweetId)

  def getTweetsKeys(tweetIds: List[Long]) = tweetIds.map { getTweetsKey(_) }

  def getUserTweetsKey(userId: Long) = new Key(ns, userTweetsSet, userId)

  def getCelebsKey(userId: Long) = new Key(ns, celebsSet, userId)

  def getFansKey(userId: Long) = new Key(ns, fansSet, userId)

}

/**
 * TSAerospikeService
 *
 * Aerospikeとの通信部分をラップするサービス
 *
 */
class TSAerospikeService extends AerospikeService with TSAerospikeTrait {

}
