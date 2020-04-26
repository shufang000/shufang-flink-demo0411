package com.shufang.state

import com.shufang.entities.SensorReading
import com.shufang.source.MyUDFSensorReadingSource
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

object ValueStateDemo03 {
  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    val source: DataStream[SensorReading] = env.addSource(new MyUDFSensorReadingSource)

    source.keyBy(_.name).process(new UDFProcessFunction).print()
    env.execute()
  }


  /**
   * 方式二，使用KeyedProcessFunction低级API来处理
   */
  class UDFProcessFunction extends KeyedProcessFunction[String, SensorReading, (SensorReading, Double)] {

    var lastTemp: ValueState[Double] = _

    override def open(parameters: Configuration): Unit = {
      lastTemp = getRuntimeContext.getState[Double](new ValueStateDescriptor[Double]("lastTemperture", classOf[Double]))
    }

    override def processElement(value: SensorReading, ctx: KeyedProcessFunction[String, SensorReading, (SensorReading, Double)]#Context, out: Collector[(SensorReading, Double)]): Unit = {

      val valueOfLastTemp: Double = lastTemp.value()

      //TODO TRANS LOGIC，如果第一次获取状态值，肯定是为0，如果是第一次就与自己比较，不会温度异常
      val ltemp: Double =
        if (valueOfLastTemp != 0) {
          valueOfLastTemp
        } else value.temperture


      //如果同样的传感器，当前温度与上次温度超过1度，报温度异常
      if ((value.temperture - ltemp).abs > 1) {
        println("温度异常")
        out.collect((value, ltemp))
      }

      //每次都需要更新一次状态,将当前温度更新进去
      lastTemp.update(value.temperture)

    }
  }
}
