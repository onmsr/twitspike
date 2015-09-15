package jp.co.dwango.twitspike.services

import com.aerospike.client.AerospikeClient
import jp.co.dwango.twitspike.controllers.TSMsgTrait
import jp.co.dwango.twitspike.exceptions.{TwitSpikeException, TwitSpikeExceptionTrait}

/**
 * FollowService
 *
 * フォローサービス
 *
 */
class FollowService(_client: AerospikeClient)
  extends TSAerospikeService
  with TwitSpikeExceptionTrait
  with TSMsgTrait {

  val client = _client

  /**
   * フォロー情報を作成する
   *
   * @param userId          フォローするユーザーID
   * @param targetUserId    フォローされるユーザーID
   * @return
   */
  def create(userId: Long, targetUserId: Long) = {
    val result1 = for {
      userCelebs <- getLargeList(client, wPolicy, getCelebsKey(userId), "celebs").right
      res <- addToLargeList(userCelebs, targetUserId).right
    } yield res

    val result2 = for {
      targetUserFans <- getLargeList(client, wPolicy, getFansKey(targetUserId), "fans").right
      res <- addToLargeList(targetUserFans, userId).right
    } yield res

    (result1, result2) match {
      case (Right(_), Right(_)) => Right(true)
      case _ => Left(new TwitSpikeException(ALREADY_FOLLOW_ERROR, alreadyFollowErrorMessage))
    }
  }

  /**
   * フォロー情報を削除する
   *
   * @param userId          フォローしているユーザーID
   * @param targetUserId    フォローされているユーザーID
   * @return
   */
  def delete(userId: Long, targetUserId: Long) = {
    val result1 = for {
      userCelebs <- getLargeList(client, wPolicy, getCelebsKey(userId), "celebs").right
      res <- removeFromLargeList(userCelebs, targetUserId).right
    } yield res

    val result2 = for {
      targetUserFans <- getLargeList(client, wPolicy, getFansKey(targetUserId), "fans").right
      res <- removeFromLargeList(targetUserFans, userId).right
    } yield res

    (result1, result2) match {
      case (Right(_), Right(_)) => Right(true)
      case _ => Left(new RuntimeException) // どういうとき起こるのかよくわかんない
    }
  }

  /**
   * フォローしているかどうか確認する
   *
   * @param srcUserId        ユーザーID
   * @param targetUserId    フォローされているかもしれないユーザーID
   * @return
   */
  def isFollow(srcUserId: Long, targetUserId: Long) = {
    for {
      userCelebs <- getLargeList(client, wPolicy, getCelebsKey(srcUserId), "celebs").right
      res <- Right(existsInLargeList(userCelebs, targetUserId)).right
    } yield res
  }

  /**
   * フォローされているかどうか確認する
   *
   * @param targetUserId    ユーザーID
   * @param srcUserId       フォローしているかもしれないユーザーID
   * @return
   */
  def isFollowed(targetUserId: Long, srcUserId: Long) = {
    for {
      targetUserFans <- getLargeList(client, wPolicy, getFansKey(targetUserId), "fans").right
      res <- Right(existsInLargeList(targetUserFans, srcUserId)).right
    } yield res
  }

}
