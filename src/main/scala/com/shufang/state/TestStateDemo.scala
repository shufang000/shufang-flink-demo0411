package com.shufang.state

import org.apache.flink.api.common.functions.{ReduceFunction, RichMapFunction}
import org.apache.flink.api.common.state.ValueStateDescriptor
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.functions.ProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector


/*******************************************************************************************
 * Flink中的状态有2种形式：Managed\Row，官方建议使用Managed，通过Flink Runtime进行管理
 *
 * ManagedState又分为2种：
 * 一：KeyedState: 只能用在keyedStream上
 * 二：OperatorState: 可以用在所有的算子中（FlinkKafkaConsumer就是典型的OperatorState）
 *
 * 下面来显示以下2种状态的区别：-----------------------------------------------------------------------
 *      ManagedKeyedState                                  ｜ OperatorState
 *      通过重写RichFunction，getRuntimeContext来创建获取状态 ｜同时实现CheckpointedFunction
 *      每个算子的子任务可以处理多个key的状态，每个key对应       ｜一个算子的一个子任务共享一份状态
 *      一个状态                                             ｜
 *      状态存储在本地                                        ｜状态存储在本地，算子子任务之间不能相互访问
 *      ValueState、ListState、MapState                      ｜ListState、BroadCastState
 ******************************************************************************************************/
object TestStateDemo {
  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    val ds: DataStream[(String, Int)] = env.fromElements(("shufang", 1), ("shufang", 2), ("yt", 1), ("yt", 2))



    env.execute("")
  }


  /**
   * 访问operatorState
   */
  class MyOperatorStateFunction extends CheckpointedFunction{
    override def snapshotState(context: FunctionSnapshotContext): Unit = ???
    override def initializeState(context: FunctionInitializationContext): Unit = ???
  }
}
