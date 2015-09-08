package jp.co.dwango.twitspike.services

import com.aerospike.client.AerospikeClient
import com.aerospike.client.large.LargeList
import com.aerospike.client.policy.BatchPolicy
import com.aerospike.client.policy.ClientPolicy
import com.aerospike.client.policy.Policy
import com.aerospike.client.policy.QueryPolicy
import com.aerospike.client.policy.ScanPolicy
import com.aerospike.client.policy.WritePolicy
import com.aerospike.client.Operation
import com.aerospike.client.Value
import com.aerospike.client.Bin
import com.aerospike.client.Key
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import scala.util.control.Exception.allCatch
import play.api.Play
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
        // @TODO output log
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
        // @TODO output log
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
        // @TODO output log
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
        // @TODO output log
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
    allCatch either llist.add(Value.get(mapAsJavaMap(m)))
  }

  /**
   * ラージオーダードリストにデータを追加する
   */
  def addToLargeList(llist: LargeList, v: Long) = {
    allCatch either llist.add(Value.get(v))
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

object AerospikeService {

  val serverUrl = Play.configuration.getString("ts.asServer1").get

  /**
   * Aerospikeクライアントを取得する
   */
  def getClient = {
    allCatch either new AerospikeClient(serverUrl, 3000)
  }

}
