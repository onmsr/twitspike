package jp.co.dwango.twitspike.services

import com.aerospike.client.AerospikeClient

/**
 * FollowService
 *
 * フォローサービス
 *
 */
class FollowService(_client: AerospikeClient) extends TSAerospikeService {

  val client = _client

  /**
   * フォロー情報を作成する
   * 
   * @param userId          フォローするユーザーID
   * @param targetUserId    フォローされるユーザーID
   * @return boolean
   */
  def create(userId: Long, targetUserId: Long) = {
    val userCelebs = client.getLargeList(wPolicy, getCelebsKey(userId), "celebs")
    addToLargeList(userCelebs, targetUserId)
    val targetUserFans = client.getLargeList(wPolicy, getFansKey(targetUserId), "fans")
    addToLargeList(targetUserFans, userId)
  }

  /**
   * フォロー情報を削除する
   * 
   * @param userId          フォローしているユーザーID
   * @param targetUserId    フォローされているユーザーID
   * @return boolean
   */
  def delete(userId: Long, targetUserId: Long) = {
    val userCelebs = client.getLargeList(wPolicy, getCelebsKey(userId), "celebs")
    removeFromLargeList(userCelebs, targetUserId)
    val targetUserFans = client.getLargeList(wPolicy, getFansKey(targetUserId), "fans")
    removeFromLargeList(targetUserFans, userId)
  }

  /**
   * フォローしているかどうか確認する
   * 
   * @param srcUserId        ユーザーID
   * @param targetUserId    フォローされているかもしれないユーザーID
   * @return boolean
   */
  def isFollow(srcUserId: Long, targetUserId: Long) = {
    val userCelebs = client.getLargeList(wPolicy, getCelebsKey(srcUserId), "celebs")
    existsInLargeList(userCelebs, targetUserId)
  }

  /**
   * フォローされているかどうか確認する
   * 
   * @param targetUserId    ユーザーID
   * @param srcUserId       フォローしているかもしれないユーザーID
   * @return boolean
   */
  def isFollowed(targetUserId: Long, srcUserId: Long) = {
    val targetUserFans = client.getLargeList(wPolicy, getFansKey(targetUserId), "fans")
    existsInLargeList(targetUserFans, srcUserId)
  }

}

