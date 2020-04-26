package com.shufang.event_time

import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}

object FlinkEventTime {

  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    //常规的执行配置，详情可以参考官网的execution-runtime-configuration
    env.setParallelism(2)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    //env.getConfig.setAutoWatermarkInterval(5000)//每5s产生一个watermark


    //checkpoint的相关配置
    env.enableCheckpointing(1000) //每秒checkpoint一次
    env.getCheckpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE) //checkpoint的类型
    env.getCheckpointConfig.setCheckpointTimeout(6000) //设置checkpoint的超时时长为6s


    //Socket stream
    val stream: DataStream[String] = env.socketTextStream("localhost", 9999)


    // 指定相应的timestamp字段和watermark生成器,一般是在source之后transform之前进行指定
    stream.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[String](Time.seconds(5)) {
      override def extractTimestamp(element: String): Long = System.currentTimeMillis()
    })

    stream.print()

    env.execute("event_time")
  }

}
