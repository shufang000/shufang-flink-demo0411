package com.shufang.udf

import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks
import org.apache.flink.streaming.api.watermark.Watermark


class WaterMarkFunction extends AssignerWithPeriodicWatermarks[String] {

  private var maxOutOrderness: Long = 5000L   //允许的最大乱序时间
  private var curMaxTimeStamp: Long = Long.MinValue  //当前遇到的最大的时间戳
  private var lastEmitedWatermark: Long = _  //上次发送到下一级的水印

  //获取当前的watermark
  override def getCurrentWatermark: Watermark = {

     curMaxTimeStamp - maxOutOrderness

    return new Watermark(1000);
  }

  //指定时间戳
  override def extractTimestamp(element: String, previousElementTimestamp: Long): Long = {
    val timestamp: Long = element.toLong

    timestamp
  }
}
