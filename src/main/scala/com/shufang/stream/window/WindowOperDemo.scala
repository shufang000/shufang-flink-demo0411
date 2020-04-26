package com.shufang.stream.window

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow


object WindowOperDemo {
  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    val sourceStream: DataStream[String] = env.socketTextStream("localhost", 9999)


    val windowStream: WindowedStream[(String, Int), String, TimeWindow] = sourceStream
      .map((_, 1))
      .keyBy(_._1)
      .timeWindow(Time.seconds(5))
      .allowedLateness(Time.seconds(2))
      .sideOutputLateData(new OutputTag[(String, Int)]("late_data")) // 测输出流

    windowStream.sum(1).print()




    env.execute("window")
  }
}
