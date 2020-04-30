package com.shufang.stream

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

object SideOutputStreamDemo {

  def main(args: Array[String]): Unit = {

    val tool: ParameterTool = ParameterTool.fromPropertiesFile("src/main/resources/netcat.properties")

    val hostname: String = tool.get("hostname")
    val port: Int = tool.getInt("port")

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    val ds: DataStream[String] = env.socketTextStream(hostname, port)


    val function = new MyProcessFunction
    val outputflag: OutputTag[String] = function.outputflag

    val mainStream: DataStream[String] = ds.process(function) //主流
    mainStream.print("正常体温->")

    val outputStream: DataStream[String] = mainStream.getSideOutput(outputflag) //侧输出流
    outputStream.print("过高体温->")


    env.execute("netcat")
  }
}


class MyProcessFunction extends ProcessFunction[String, String] {

  val outputflag = new OutputTag[String]("高温")

  override def processElement(value: String,
                              ctx: ProcessFunction[String, String]#Context,
                              out: Collector[String]): Unit = {
    if (value.split(",")(3).toDouble < 37) {
      // 直接输出
      out.collect(value)
    } else {
      // 输出到测输出流
      ctx.output(outputflag, value)
    }
  }
}
