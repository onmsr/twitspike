package jp.co.dwango.twitspike.services

import com.aerospike.client.AerospikeClient
import com.aerospike.client.Bin
import jp.co.dwango.twitspike.models.Tweet
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import jp.co.dwango.twitspike.exceptions.TwitSpikeException
import jp.co.dwango.twitspike.exceptions.TwitSpikeExceptionTrait
import jp.co.dwango.twitspike.controllers.TSMsgTrait

/**
 * TweetService
 *
 * ツイートサービス
 *
 */
class TweetService(_client: AerospikeClient)
    extends TSAerospikeService
    with TwitSpikeExceptionTrait
    with TSMsgTrait {

  val client = _client

  /**
   * 次のツイートIDを取得する
   * 
   * @return 新しいツイートID
   */
  def nextId = createNextId(client, ns, "tweet_last_id")

  /**
   * ツイートを作成する
   */
  def create(userId: Long, content: String) = {
    for {
      id <- nextId.right
      now <- Right(System.currentTimeMillis()).right
      ts <- Right(new DateTime(now).toString(ISODateTimeFormat.dateTimeNoMillis)).right
      _ <- createTweet(userId, id, content, ts).right
      _ <- addUserTweets(userId, id, now).right
      _ <- Right(updateTimeline(userId, id, now)).right
      _ <- Right(updateFansTimeline(userId, id, now)).right
    } yield id
  }

  /**
   * ツイート情報の保存を行う
   */
  private[this] def createTweet(userId: Long, id: Long, content: String, ts: String) = {
    val idBin = new Bin("id", id)
    val userIdBin = new Bin("user_id", userId)
    val contentBin = new Bin("content", content)
    val createdAtBin = new Bin("created_at", ts)

    val key = getTweetsKey(id)
    write(client, key, Array(idBin, userIdBin, contentBin, createdAtBin))
  }

  /**
   * ユーザーとツイートの関係情報の保存を行う
   */
  private[this] def addUserTweets(userId: Long, id: Long, time: Long) = {
    val key = getUserTweetsKey(userId)
    val llist = client.getLargeList(wPolicy, key, "tweets")

    val map = Map("key" -> time, "tweetId" -> id)
    addToLargeList(llist, map)
  }

  /**
   * 指定したユーザーのタイムラインにツイートIDを追加する
   */
  private[this] def updateTimeline(userId: Long, id: Long, time: Long) = {
    val key = getTimelinesKey(userId)
    val llist = client.getLargeList(wPolicy, key, "timeline")

    val map = Map("key" -> -time, "tweetId" -> id) // ラージオーダードリストで逆順にするために符号を反転する
    addToLargeList(llist, map).isRight
  }

  /**
   * 指定したユーザーのフォロワーのタイムラインにツイートIDを追加する
   */
  private[this] def updateFansTimeline(userId: Long, id: Long, time: Long) = {
    // 本来なら非同期にしたほうが良い
    val us = new UserService(client)
    val fans = us.findFanIds(userId)
    fans.map { updateTimeline(_, id, time) }
  }

  /**
   * ツイートを削除する
   */
  def delete(tweetId: Long) = {
    val tweetOpt = findOneById(tweetId)
    tweetOpt match {
      case None => {
        // ツイートが存在しないとき
        Left(new TwitSpikeException(TWEET_NOT_FOUND_ERROR, tweetNotFoundErrorMessage))
      }
      case Some(_) => {
        // ユーザーとツイートの関係情報の削除 -> ツイート本体の削除
        val userId = tweetOpt.map(_.userId).get
        val tweets = client.getLargeList(wPolicy, getUserTweetsKey(userId), "tweets")
        removeFromLargeList(tweets, tweetId)
        remove(client, getTweetsKey(tweetId))
      }
    }
  }

  /**
   * ツイートを一件取得する
   */
  def findOneById(tweetId: Long): Option[Tweet] = {
    val key = getTweetsKey(tweetId)
    readAsMap(client, key).map { Tweet.tweetRecordMapToTweet(_) }
  }

  /**
   * ツイートを複数件取得する
   */
  def findByIds(tweetIds: List[Long]): List[Tweet] = {
    val keys = getTweetsKeys(tweetIds)
    readAsMap(client, keys).map { Tweet.tweetRecordMapToTweet(_) }
  }

  /**
   * ユーザーのツイートID一覧を取得する
   */
  def findIds(userId: Long) = {
    val key = getUserTweetsKey(userId)
    val llist = client.getLargeList(wPolicy, key, "tweets")
    scanMapLargeList(llist).asInstanceOf[List[Long]]
  }

}
