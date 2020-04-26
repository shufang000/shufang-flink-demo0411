package com.shufang.event_time

import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

/**
 * 用来测试watermark的功能
 */
object FlinkWaterMarks {

  def main(args: Array[String]): Unit = {


    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    env.setBufferTimeout(10000)

    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)


    //source of the stream
    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)

    //抽取时间字段，指定时间戳的字段，自动发送watermark到下一个oper
    stream.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[String](Time.seconds(1)) {
      override def extractTimestamp(element: String): Long = System.currentTimeMillis()
    })



    //start to transform stream
    stream.flatMap((a: String) => a.split(" "))
      .map((_, 1))



        env.execute("watermark")
  }
}
