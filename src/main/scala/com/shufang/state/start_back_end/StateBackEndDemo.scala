package com.shufang.state.start_back_end

import org.apache.flink.contrib.streaming.state.RocksDBStateBackend
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

object StateBackEndDemo {
  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    //配置状态后端
    //可以在flink-conf.yaml 中配置 state.checkpoints.dir: hdfs:///checkpoints/
    env.setStateBackend(new RocksDBStateBackend("hdfs:///checkpoints/"))
  }
}
