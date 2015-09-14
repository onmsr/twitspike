package jp.co.dwango.twitspike.controllers

import scala.util.control.Exception.allCatch

import com.aerospike.client.{Key, Record, ScanCallback}
import jp.co.dwango.twitspike.exceptions.TwitSpikeException
import jp.co.dwango.twitspike.exceptions.TwitSpikeException.writes
import jp.co.dwango.twitspike.services.{AerospikeService, UserService}
import play.Logger
import play.api.Play
import play.api.Play.current
import play.api.libs.json.Json
import play.api.mvc.Action

/**
 * BatchController
 *
 * バッチ処理用のAPIを提供するコントローラー
 */
class BatchController extends BaseController {

  /**
   * 保持するタイムラインの件数
   */
  val timelineSize = Play.configuration.getInt("ts.timeline.size").getOrElse(3000)

  /**
   * 古いタイムライン削除時のマージン
   */
  val timelineMargin = Play.configuration.getInt("ts.timeline.margin").getOrElse(500)

  /**
   * すべてのユーザーの古いタイムラインを削除する
   *
   * @return
   */
  def sweepTimelines() = Action { implicit request =>
    Logger.info("----------- sweep timeline start --------------------------")
    Logger.info(s"timeline max size : ${timelineSize}, timeline margin : ${timelineMargin}")
    val client = AerospikeService.getClient.right.get
    val us = new UserService(client)
    val scanCallback = new ScanCallback {
      override def scanCallback(key: Key, record: Record): Unit = {
        val id = record.getLong("id")
        val user = us.findOneById(id)
        val timeline = us.getLargeList(client, us.wPolicy, us.getTimelinesKey(id), "timeline").right.get
        val size = timeline.size()
        val result = if (user.isDefined) us.sweepTimeline(id, timelineSize, timelineMargin).right.get else false
        Logger.info(s"sweep timeline user id : ${id}, timeline tweet count : ${size}, result : ${result}")
      }
    }
    client.scanAll(null, "twitspike", "users", scanCallback, "id")
    Logger.info("----------- sweep timeline end --------------------------")
    Ok
  }

  /**
   * 指定したユーザーの古いタイムラインを削除する
   *
   * @return
   */
  def sweepTimeline(userId: Long, n: Int, margin: Int) = Action { implicit request =>
    (for {
      client <- AerospikeService.getClient.right
      us <- Right(new UserService(client)).right
      user <- us.findOneById(userId).toRight(
        new TwitSpikeException(USER_NOT_FOUND_ERROR, userNotFoundErrorMessage)).right
      result <- us.sweepTimeline(userId, n, margin).right
      _ <- (allCatch either client.close).right
    } yield result) match {
      case Left(e) => {
        e match {
          case TwitSpikeException(USER_NOT_FOUND_ERROR, _) => NotFound(Json.obj("error" -> Json.toJson(e.asInstanceOf[TwitSpikeException])))
          case _ => InternalServerError(Json.obj("error" -> internalServerErrorMessage))
        }
      }
      case Right(result) => Ok(Json.obj("result" -> result))
    }
  }

}
