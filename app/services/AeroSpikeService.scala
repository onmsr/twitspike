package jp.co.dwango.twitspike.services

import com.aerospike.client.AerospikeClient
import com.aerospike.client.policy.ClientPolicy
import com.aerospike.client.policy.Policy
import com.aerospike.client.policy.QueryPolicy
import com.aerospike.client.policy.ScanPolicy
import com.aerospike.client.policy.WritePolicy
import com.aerospike.client.Bin
import com.aerospike.client.Key
import scala.util.Try
import scala.util.Failure
import scala.util.Success

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

}

class AerospikeService extends AerospikeServiceTrait {

}

object AerospikeService {

  val serverUrl = "192.168.38.200"

  /**
   * Aerospikeクライアントを取得する
   */
  def getClient = {
    new AerospikeClient(serverUrl, 3000)
  }

}
