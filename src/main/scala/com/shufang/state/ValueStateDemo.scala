package com.shufang.state

import com.shufang.entities.SensorReading
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector


/**
 * @0 getRuntimeContxt().getValueState()获取当前状态
 * @1 state.value() => 获取状态的值
 * @2 state.update(new value) => 更新状态
 * @3 state.clear() => 清除状态
 */
object ValueStateDemo {
  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    //    import scala.collection.JavaConversions._
    val ds: DataStream[(Long, Long)] = env.fromCollection(List(
      (1L, 3L),
      (1L, 5L),
      (1L, 7L),
      (1L, 4L),
      (1L, 2L))
    )

    val ds1: DataStream[(Long, Long)] = ds.keyBy(_._1).flatMap(new MyFlatMapFunction)
    ds1.print()
    env.execute()

  }


  //自定义一个富函数，只有ProcessFunctionAPI 和 RichXXXFunction中才能创建和外部访问
  class MyFlatMapFunction extends RichFlatMapFunction[(Long, Long), (Long, Long)] {

    //声明一个状态,用来接收状态值
    var sum: ValueState[(Long, Long)] = _

    //获取到状态,进行初始化
    override def open(parameters: Configuration): Unit = {
      sum = getRuntimeContext.getState(new ValueStateDescriptor[(Long, Long)]("average", classOf[(Long, Long)]))
    }

    //逻辑方法
    override def flatMap(value: (Long, Long), out: Collector[(Long, Long)]): Unit = {
      //访问状态值
      val tmpCurrentSum:(Long,Long) = sum.value

      //如果状态没有用过,那么它就为null
      val currentSum:(Long,Long) =
        if (tmpCurrentSum != null) {
          tmpCurrentSum
        } else {
          (0L, 0L)
        }

      // 更新值:currentSum._1为累加次数，currentSum._2为累加值
      val newSum:(Long,Long) = (currentSum._1 + 1, currentSum._2 + value._2)

      // 将更新后的值跟新到状态中
      sum.update(newSum)

      // if the count reaches 2, emit the average and clear the state
      if (newSum._1 >= 2) {
        out.collect((value._1, newSum._2 / newSum._1))
        sum.clear()
      }

    }
  }

}
