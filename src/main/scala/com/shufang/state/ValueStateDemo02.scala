package com.shufang.state

import com.shufang.entities.SensorReading
import com.shufang.source.MyUDFSensorReadingSource
import org.apache.flink.api.common.functions.{RichFlatMapFunction, RichMapFunction}
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.{KeyedProcessFunction, ProcessFunction}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector


/**
 * 需求：找出传感器中 当前温度与上个温度相差超过 1 的传感器
 * source： SensorReading(id,name,timestamp,temperture)
 * transformation: 我们的逻辑就要写在相应的方法里面，我们需要保存上个传感器温度的状态
 * sink: (SensorReading，lastTemp)
 */
object ValueStateDemo02 {
  def main(args: Array[String]): Unit = {

    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val source: DataStream[SensorReading] = env.addSource(new MyUDFSensorReadingSource)

//    source.print()

    /** **********************自定义source输入数据样例******************************
     * 8> SensorReading(1000,sendorReading1001,1587893531023,36.75647979483103)
     * 1> SensorReading(1000,sendorReading1006,1587893533035,36.33364356765155)
     * 2> SensorReading(1001,sendorReading1002,1587893535040,36.5970659543919)
     * 3> SensorReading(1000,sendorReading1008,1587893537045,34.79520336147952)
     * 4> SensorReading(1000,sendorReading1007,1587893539050,35.98610864303115)
     * 5> SensorReading(1001,sendorReading1005,1587893541055,34.93745165176868)
     * **************************************************************************/


    /**
     * 方式一、通过RichFlatMapFunction富函数来处理
     */
        source.keyBy(1).flatMap(
          new RichFlatMapFunction[SensorReading, (SensorReading, Double)] {
            var lastTemp: ValueState[Double] = _

            override def open(parameters: Configuration): Unit = {
              lastTemp = getRuntimeContext.getState[Double](new ValueStateDescriptor[Double]("lastTemperture", classOf[Double]))
            }

            override def flatMap(value: SensorReading, out: Collector[(SensorReading, Double)]): Unit = {

              val valueOfLastTemp: Double = lastTemp.value()

              //TODO TRANS LOGIC，如果第一次获取状态值，肯定是为0，如果是第一次就与自己比较，不会温度异常
              val ltemp: Double =
                if (valueOfLastTemp != 0) {
                  valueOfLastTemp
                } else value.temperture


              //如果同样的传感器，当前温度与上次温度超过1度，报温度异常
              if ((value.temperture - ltemp).abs > 1){
                println("温度异常")
                out.collect((value,ltemp))
              }

              //每次都需要更新一次状态,将当前温度更新进去
              lastTemp.update(value.temperture)
            }

          }).setParallelism(1).print()//.setParallelism(1)



    env.execute()

  }


  /** ************************************示例结果****************************************************
   * 4> (SensorReading(1001,sendorReading1007,1587893077879,36.27034794287735),37.50624610318885)
   * 温度异常
   * 5> (SensorReading(1000,sendorReading1008,1587893079883,34.98390525327701),37.134001023048015)
   * 温度异常
   * 6> (SensorReading(1001,sendorReading1001,1587893085895,35.33491929122922),36.801838067228715)
   * 温度异常
   * 7> (SensorReading(1000,sendorReading1008,1587893087900,36.59715872814715),34.98390525327701)
   * 温度异常
   * 8> (SensorReading(1001,sendorReading1004,1587893095919,36.70736640123999),35.556945718669375)
   * ************************************************************************************************/


}



