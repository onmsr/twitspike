package jp.co.dwango.twitspike.services

import com.aerospike.client.AerospikeClient
import com.aerospike.client.large.LargeList
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
import scala.util.control.Exception.catching

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
  val sPolicy = new ScanPolicy
  val qPolicy = new QueryPolicy

  /**
   * レコードを一件読み出す
   */
  def read(client: AerospikeClient, key: Key) = {
    Option(client.get(rPolicy, key))
  }

  /**
   * レコードを複数件読み出す
   */
  def read(client: AerospikeClient, keys: List[Key]) = {
    val records = client.get(rPolicy, keys.toArray).toList
    records.map { record => Option(record) } filter { _.isDefined }
  }

  /**
   * レコードを一件書き込む
   */
  def write(client: AerospikeClient, key: Key, bins: Array[Bin]) = {
    Try { client.put(wPolicy, key, bins: _*) } match {
      case Success(_) => true
      case Failure(e) => {
        // @TODO output log
        false
      }
    }
  }

  /**
   * レコードを一件削除する
   */
  def remove(client: AerospikeClient, key: Key) = {
    Try { client.delete(wPolicy, key) } match {
      case Success(v) => v
      case Failure(e) => {
        // @TODO output log
        false
      }
    }
  }

  /**
   * レコードが存在しているかどうか確認する
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
    val record = client.operate(null, key, Operation.add(bin), Operation.get())
    record.getLong("id")
  }

  /**
   * ラージオーダードリストにハッシュマップデータを追加する
   */
  def addToLargeList(llist: LargeList, m: Map[_ <: Any, Any]) = {
    llist.add(Value.get(scala.collection.JavaConversions.mapAsJavaMap(m)))
  }

  /**
   * ラージオーダードリストにデータを追加する
   */
  def addToLargeList(llist: LargeList, v: Long) = {
    llist.add(Value.get(v))
  }

  /**
   * ラージオーダードリストからデータを削除する
   */
  def removeFromLargeList(llist: LargeList, v: Long) = {
    llist.remove(Value.get(v))
  }

  /**
   * ラージオーダードリストからデータを取得する
   */
  def findFromLargeList(llist: LargeList, v: Long) = {
    import scala.collection.JavaConversions.asScalaBuffer
    val records = Option(llist.find(Value.get(v)))
    records.map {
      catching(classOf[NoSuchElementException]) opt _.toList.head
    } flatten
  }

  /**
   * ラージオーダードリストにデータが存在するかどうか調べる
   */
  def existsInLargeList(llist: LargeList, v: Long) = {
    findFromLargeList(llist, v).isDefined
  }

}

class AerospikeService extends AerospikeServiceTrait {

}

object AerospikeService {

  // @TODO 設定ファイルから取得
  val serverUrl = "192.168.38.200"

  /**
   * Aerospikeクライアントを取得する
   */
  def getClient = {
    new AerospikeClient(serverUrl, 3000)
  }

}
