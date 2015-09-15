package jp.co.dwango.twitspike.utils

import scala.util.control.Exception.allCatch

import org.joda.time.format.ISODateTimeFormat

/**
 * TimeUtil
 *
 * TwitSpikeの時刻処理クラス
 */
object TimeUtil {

  val formats = List(ISODateTimeFormat.date, ISODateTimeFormat.dateHourMinuteSecond, ISODateTimeFormat.dateTimeNoMillis)

  /**
   * タイムスタンプをパースしてミリ秒に変換する
   *
   * @param ts タイムスタンプ
   * @return Option[Long]
   */
  def timestampToMillis(ts: String) = {
    val f = formats.find { allCatch opt _.parseDateTime(ts).getMillis isDefined }
    f.map { _.parseDateTime(ts).getMillis }
  }

}





