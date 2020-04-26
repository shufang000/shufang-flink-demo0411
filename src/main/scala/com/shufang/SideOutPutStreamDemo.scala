package com.shufang



import com.shufang.entities.SensorReading
import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.streaming.api.functions.ProcessFunction
import com.shufang.source.MyUDFSensorReadingSource
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector


/**
 * 侧输出流是1.8以后官方推荐出来用来优化split and select函数的方式，还可以处理延迟数据
 * 使用侧输出流有2中方式：
 * 一、通过process()中使用ctx.output[](new OutputTag("name",data))，低级API
 * 二、通过window函数的 allowLateness().sideOutputStream("name") ，允许延迟数据进入侧输出流
 */
 object SideOutPutStreamDemo {

  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    val ds: DataStream[SensorReading] = env.addSource(new MyUDFSensorReadingSource)

    val mainStream: DataStream[SensorReading] = ds.process(new ProcessFunction[SensorReading, SensorReading] {
      override def processElement(value: SensorReading,
                                  ctx: ProcessFunction[SensorReading, SensorReading]#Context,
                                  out: Collector[SensorReading]): Unit = {
        if (value.temperture > 36) out.collect(value)
        else ctx.output[SensorReading](new OutputTag[SensorReading]("normals"), value)
      }
    })


    mainStream.print("主流>>>>>")
    val outputStream: DataStream[SensorReading] = mainStream.getSideOutput(new OutputTag[SensorReading]("normals"))

    outputStream.print("侧输出流>>>>>")

    mainStream.union(outputStream).print("联合流：")

    env.execute()
  }
}


