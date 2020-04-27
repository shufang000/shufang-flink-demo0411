package com.shufang.state

import com.shufang.entities.SensorReading
import com.shufang.source.MyUDFSensorReadingSource
import org.apache.flink.api.common.state.{StateTtlConfig, ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.time.Time
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
      val describer = new ValueStateDescriptor[Double]("lastTemperture", classOf[Double])

      //声明状态超时设置
      /**
       * 该newBuilder方法的第一个参数是强制性的，它是生存时间值。
       *
       * 更新类型配置何时刷新状态TTL（默认为OnCreateAndWrite）：
       *
       * StateTtlConfig.UpdateType.OnCreateAndWrite -仅在创建和写访问权限时
       * StateTtlConfig.UpdateType.OnReadAndWrite -也具有读取权限
       * 状态可见性用于配置是否清除尚未过期的默认值（默认情况下NeverReturnExpired）：
       *
       * StateTtlConfig.StateVisibility.NeverReturnExpired -永不返回过期值
       * StateTtlConfig.StateVisibility.ReturnExpiredIfNotCleanedUp -如果仍然可用则返回
       * 在的情况下NeverReturnExpired，即使仍必须删除过期状态，其行为也好像不再存在一样。对于严格在TTL之后数据必须变得不可用于读取访问的用例，例如使用隐私敏感数据的应用程序，该选项很有用。
       *
       * 另一个选项ReturnExpiredIfNotCleanedUp允许在清理之前返回过期状态。
       */
      val stateTtlConfig: StateTtlConfig = StateTtlConfig.newBuilder(Time.seconds(1))
        .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
        .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
        .build()
      describer.enableTimeToLive(stateTtlConfig)

      lastTemp = getRuntimeContext.getState[Double](describer)
    }

    override def processElement(value: SensorReading,
                                ctx: KeyedProcessFunction[String, SensorReading, (SensorReading, Double)]#Context,
                                out: Collector[(SensorReading, Double)]): Unit = {

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
