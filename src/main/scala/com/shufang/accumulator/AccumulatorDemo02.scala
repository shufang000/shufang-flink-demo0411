package com.shufang.accumulator

import com.shufang.entities.SensorReading
import com.shufang.source.MyUDFSensorReadingSource
import org.apache.flink.api.common.JobExecutionResult
import org.apache.flink.api.common.accumulators.LongCounter
import org.apache.flink.api.common.functions.{RichMapFunction, RuntimeContext}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala._

object AccumulatorDemo02 {
  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    val sourceStream: DataStream[SensorReading] = env.addSource[SensorReading](new MyUDFSensorReadingSource)


    //开始使用累加器
    sourceStream.map(
      new RichMapFunction[SensorReading, String] {

        //声明累加器，必须是全局的，保证每个方法都能调用
        var normal: LongCounter = _
        var unnormal: LongCounter = _
        var total: LongCounter = _

        //初始化工作
        override def open(parameters: Configuration): Unit = {
          normal = new LongCounter
          unnormal = new LongCounter
          total = new LongCounter

          val context: RuntimeContext = getRuntimeContext
          context.addAccumulator("normal", normal)
          context.addAccumulator("unnormal", unnormal)
          context.addAccumulator("total", total)
        }

        override def map(value: SensorReading): String = {
          //不管三7二十一，总数先加
          total.add(1)
          if (value.temperture >= 36.2) {
            unnormal.add(1)
            s"${value.name}的体温为：${value.temperture},超过正常体温 ${value.temperture - 36}度，" +
              s"合理建议：请及时去有关部门隔离观察！～"
          } else {
            normal.add(1)
            s"${value.name}的体温为：${value.temperture},体温正常，请通过～"
          }

        }
      }
    ).print()

    //获取执行结果
    val result: JobExecutionResult = env.execute("红外测温器")

    //累加器的累加结果，同时可以声明多个累加器
    val normal: Long = result.getAccumulatorResult[Long]("normal")
    val unnormal: Long = result.getAccumulatorResult[Long]("unnormal")
    val total: Long = result.getAccumulatorResult[Long]("total")

    println(s"最终结果为：=> \n" +
      s"正常体温人数: $normal" +
      s"异常体温人数: $unnormal" +
      s"总人数为: $total")

  }
}
