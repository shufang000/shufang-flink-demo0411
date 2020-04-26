package com.shufang.stream

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.scala.{DataStream, StreamExecutionEnvironment}

object FlinkStreamDemo {
  def main(args: Array[String]): Unit = {

    //拦截非法参数
    //    if (args == null || args.length != 2) {
    //      println("警告：！参数不符合规范～")
    //      sys.exit(-1)
    //    }

    //    ParameterTool.fromPropertiesFile()
    val tool: ParameterTool = ParameterTool.fromArgs(args)
    val hostname: String = tool.get("hostname")
    val port: Int = tool.getInt("port")

    //获取执行环境
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    //获取流
    val stream: DataStream[String] = env.socketTextStream(hostname, port)

    import org.apache.flink.streaming.api.scala._
    stream.flatMap(_.split("\\s+"))
      .filter(_.nonEmpty)
      .map((_, 1))
      .keyBy(0)
      .sum(1)
      .print()


    //启动
    env.execute("stream_test")

  }
}
