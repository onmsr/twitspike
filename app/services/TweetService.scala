package jp.co.dwango.twitspike.services

import com.aerospike.client.AerospikeClient
import com.aerospike.client.Bin
import com.aerospike.client.Operation
import com.aerospike.client.policy.GenerationPolicy
import com.aerospike.client.policy.RecordExistsAction
import jp.co.dwango.twitspike.models.Tweet
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.ISODateTimeFormat
import jp.co.dwango.twitspike.models.User

/**
 * TweetService
 *
 * ツイートサービス
 *
 */
class TweetService(_client: AerospikeClient) extends TSAerospikeService {

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
    val id = nextId
    val ts = new DateTime().toString(ISODateTimeFormat.dateTimeNoMillis)
    createTweet(userId, id, content, ts)
    addUserTweets(userId, id, ts)
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
  private[this] def addUserTweets(userId: Long, id: Long, ts: String) = {
    val key = getUserTweetsKey(userId)
    val llist = client.getLargeList(wPolicy, key, "tweets")

    val time = ISODateTimeFormat.dateTimeNoMillis().parseDateTime(ts).getMillis()
    val map = Map("key" -> time, "tweetId" -> id)
    addToLargeList(llist, map)
  }

  /**
   * ツイートを削除する
   */
  def delete(tweetId: Long) = {
    val tweetOpt = findOneById(tweetId)
    tweetOpt match {
      case None => {
        // @TODO ツイートが存在しないとき
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
    import Tweet.tweetRecordMapToTweet
    val key = getTweetsKey(tweetId)
    readAsMap(client, key).map { _.asInstanceOf[Tweet] }
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
    scanLargeList(llist).asInstanceOf[List[Long]]
  }

}