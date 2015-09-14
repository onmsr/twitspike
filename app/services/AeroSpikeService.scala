package jp.co.dwango.twitspike.services

import com.aerospike.client.AerospikeClient
import com.aerospike.client.large.LargeList
import com.aerospike.client.policy._
import com.aerospike.client.Operation
import com.aerospike.client.Value
import com.aerospike.client.Bin
import com.aerospike.client.Key
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import scala.util.control.Exception.allCatch
import jp.co.dwango.twitspike.globals.GlobalInfo
import play.api.{Logger, Play}
import play.api.Play.current
import scala.collection.JavaConversions.mapAsJavaMap

/**
 * AerospikeService
 *
 * Aerospikeとの通信部分をラップするサービス
 *
 */
sealed trait AerospikeServiceTrait {

  val cPolicy = new ClientPolicy
  val wPolicy = new WritePolicy
  val rPolicy = new Policy
  val bPolicy = new BatchPolicy
  val sPolicy = new ScanPolicy
  val qPolicy = new QueryPolicy

  /**
   * 各種Aerospikeポリシーのデフォルト値のセットアップ
   */
  private[this] def setupPolicy() = {
    // client policy
    cPolicy.timeout = 3000
    cPolicy.maxThreads = 200

    // write policy
    wPolicy.timeout = 500 // 書き込みタイムアウト[ms]
    wPolicy.expiration = 0 // aerospike configurationのdefault-ttlがデフォルト値
    wPolicy.generationPolicy = GenerationPolicy.NONE // 書き込みに厳密なチェックを行わない
    wPolicy.recordExistsAction = RecordExistsAction.UPDATE // 作成と更新
    wPolicy.priority = Priority.HIGH

    // read policy
    rPolicy.timeout = 100 // 読み込みタイムアウト[ms]
    rPolicy.maxRetries = 3 // リトライ回数
    rPolicy.sleepBetweenRetries = 30 // リトライ間隔[ms]
    rPolicy.priority = Priority.LOW
    rPolicy.replica = Replica.MASTER
    rPolicy.consistencyLevel = ConsistencyLevel.CONSISTENCY_ONE
    rPolicy.sendKey = false // 読み込みと書き込み操作のときに追加のキーを送るかどうか
  }
  setupPolicy()

  /**
   * レコードを一件読み出す
   *
   * @param client Aerospikeクライアント
   * @param key キー
   * @return Option[T]
   */
  def read(client: AerospikeClient, key: Key) = {
    Try { client.get(rPolicy, key) } match {
      case Success(v) => Option(v)
      case Failure(e) => {
        Logger("error").error("AerospikeService read error : ", e)
        None
      }
    }
  }

  /**
   * レコードを一件読み出す
   *
   * @param client Aerospikeクライアント
   * @param key キー
   * @return Map[String, T]
   */
  def readAsMap(client: AerospikeClient, key: Key) = {
    import scala.collection.JavaConversions.mapAsScalaMap
    read(client, key).map(_.bins.toMap)
  }

  /**
   * レコードを複数件読み出す
   *
   * @param client Aerospikeクライアント
   * @param keys キー
   * @return List[T]
   */
  def read(client: AerospikeClient, keys: List[Key]) = {
    val records = client.get(bPolicy, keys.toArray).toList
    records.filter { Option(_).isDefined }
  }

  /**
   * レコードを複数件読み出す
   *
   * @param client Aerospikeクライアント
   * @param keys キー
   * @return List[Map[String, T]]
   */
  def readAsMap(client: AerospikeClient, keys: List[Key]) = {
    import scala.collection.JavaConversions.mapAsScalaMap
    val records = client.get(bPolicy, keys.toArray).toList
    records.filter { Option(_).isDefined } map { _.bins.toMap }
  }

  /**
   * レコードを一件書き込む
   *
   * @param client Aerospikeクライアント
   * @param key キー
   * @param bins
   * @return
   */
  def write(client: AerospikeClient, key: Key, bins: Array[Bin]) = {
    Try { client.put(wPolicy, key, bins: _*) } match {
      case Success(_) => Right(true)
      case Failure(e) => {
        Logger("error").error("AerospikeService write error : ", e)
        Left(e)
      }
    }
  }

  /**
   * レコードを一件削除する
   *
   * @param client Aerospikeクライアント
   * @param key キー
   * @return
   */
  def remove(client: AerospikeClient, key: Key) = {
    Try { client.delete(wPolicy, key) } match {
      case Success(v) => Right(v)
      case Failure(e) => {
        Logger("error").error("AerospikeService remove error : ", e)
        Left(e)
      }
    }
  }

  /**
   * レコードが存在しているかどうか確認する
   *
   * @param client Aerospikeクライアント
   * @param key キー
   * @return
   */
  def exist(client: AerospikeClient, key: Key) = {
    Try { client.exists(rPolicy, key) } match {
      case Success(v) => v
      case Failure(e) => {
        Logger("error").error("AerospikeService exist error : ", e)
        false
      }
    }
  }

  /**
   * ネームスペースとプライマリーキーからオートインクリメントなIDの生成および次の値を取得を行う。
   */
  def createNextId(client: AerospikeClient, ns: String, pk: String) = {
    val key = new Key(ns, null, pk)
    val bin = new Bin("id", 1)
    allCatch either client.operate(null, key, Operation.add(bin), Operation.get()).getLong("id")
  }

  /**
   * ラージオーダードリストを取得する
   */
  def getLargeList(client: AerospikeClient, wp: WritePolicy, key: Key, bin: String) = {
    allCatch either client.getLargeList(wp, key, bin)
  }

  /**
   * ラージオーダードリストにハッシュマップデータを追加する
   */
  def addToLargeList(llist: LargeList, m: Map[_ <: Any, Any]) = {
    Try { llist.add(Value.get(mapAsJavaMap(m))) } match {
      case Success(_) => Right(true)
      case Failure(e) => {
        Logger("error").error("AerospikeService LDT write error : ", e)
        Left(e)
      }
    }
  }

  /**
   * ラージオーダードリストにデータを追加する
   */
  def addToLargeList(llist: LargeList, v: Long) = {
    Try { llist.add(Value.get(v)) } match {
      case Success(_) => Right(true)
      case Failure(e) => {
        Logger("error").error("AerospikeService LDT write error : ", e)
        Left(e)
      }
    }
  }

  /**
   * ラージオーダードリストからデータを削除する
   */
  def removeFromLargeList(llist: LargeList, v: Long) = {
    allCatch either llist.remove(Value.get(v))
  }

  /**
   * ラージオーダードリストからデータを取得する
   */
  def findFromLargeList(llist: LargeList, v: Long) = {
    import scala.collection.JavaConversions.asScalaBuffer
    allCatch opt llist.find(Value.get(v)).toList.head
  }

  /**
   * ラージオーダードリストからデータを取得する
   */
  def findRecordsFromLargeList(llist: LargeList, v: Long, count: Int) = {
    import scala.collection.JavaConversions.asScalaBuffer
    import scala.collection.JavaConversions.mapAsScalaMap
    for {
      javaRecords <- (allCatch either llist.findFrom(Value.get(v), count)).right
      records <- Right(Option(javaRecords).map(_.toList).getOrElse(List())).right
      res <- Right(records.map { v => mapAsScalaMap(v.asInstanceOf[java.util.Map[java.lang.String, java.lang.Object]]) }).right
    } yield res
  }

  /**
   * ラージオーダードリストにデータが存在するかどうか調べる
   */
  def existsInLargeList(llist: LargeList, v: Long) = {
    findFromLargeList(llist, v).isDefined
  }

  /**
   * ラージオーダードリストの値をすべて取得する
   */
  def scanLargeList(llist: LargeList) = {
    import scala.collection.JavaConversions.asScalaBuffer
    import scala.collection.JavaConversions.mapAsScalaMap
    val records = Option(llist.scan).map(_.toList).getOrElse(List())
    records.map { v => mapAsScalaMap(v.asInstanceOf[java.util.Map[java.lang.String, java.lang.Object]]) }
  }

}

class AerospikeService extends AerospikeServiceTrait {

}

object AerospikeService extends AerospikeService {

  /**
   * Aerospikeクライアントを取得する
   */
  def getClient = {
    allCatch either new AerospikeClient(GlobalInfo.serverUrl, 3000)
  }

}
