package com.shufang.source

import com.shufang.entities.SensorReading
import org.apache.flink.api.common.accumulators.LongCounter
import org.apache.flink.api.common.functions.RichReduceFunction
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

object TestSensorReadingWaterMarkDemo {
  def main(args: Array[String]): Unit = {

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime) //指定时间字符定义
    env.getConfig.setAutoWatermarkInterval(5000) //每5s生成一个递增的watermark


    //获取数据流
    val sourceStream: DataStream[SensorReading] = env.addSource[SensorReading](new MyUDFSensorReadingSource)

    //指定时间戳抽取&watermark的生成
    val streamWithEventTime: DataStream[SensorReading] = sourceStream.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[SensorReading](Time.seconds(5)) {
      override def extractTimestamp(element: SensorReading): Long = {
        element.timestamp //指定eventTime的度量字段
      }
    })

    streamWithEventTime
      .keyBy(_.name)
      .timeWindow(Time.seconds(10), Time.seconds(5))
      .maxBy(3)
      .print().setParallelism(1)


    env.execute("udf-watermark")
  }
}
