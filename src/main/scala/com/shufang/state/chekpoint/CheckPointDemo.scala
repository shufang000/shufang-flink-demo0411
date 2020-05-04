package com.shufang.state.chekpoint

import com.shufang.broadcast.People
import com.shufang.entities.WorkPeople
import com.shufang.source.MyUDFPeopleSource
import org.apache.flink.api.common.ExecutionMode
import org.apache.flink.api.common.restartstrategy.RestartStrategies
import org.apache.flink.api.common.state.MapStateDescriptor
import org.apache.flink.api.common.time.Time
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.runtime.executiongraph.restart.RestartStrategy
import org.apache.flink.streaming.api.CheckpointingMode
import org.apache.flink.streaming.api.datastream.BroadcastStream
import org.apache.flink.streaming.api.environment.CheckpointConfig
import org.apache.flink.streaming.api.functions.co.{BroadcastProcessFunction, KeyedBroadcastProcessFunction}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

object CheckPointDemo {

  def main(args: Array[String]): Unit = {

    //获取执行环境：
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    //用来决定在region 失败策略中的region范围
    env.getConfig.setExecutionMode(ExecutionMode.PIPELINED)
    /**
     * --------------------------------------checkpoint的配置-----------------------------------------------
     */
    env.enableCheckpointing(1000) //每1s checkpoint 一次

    env.getCheckpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE) //默认是EXACTLY_ONCE
    env.getCheckpointConfig.setCheckpointInterval(1000) //每隔 1s进行一次checkpoint 的工作
    env.getCheckpointConfig.setCheckpointTimeout(6000) //如果checkpoint操作在6s之内没有完成，那么就discard终端该checkpoint操作
    //true：假如在checkpoint过程中产生了Error，那么Task直接显示失败
    //false：产生了error，Task继续运行，checkpoint会降级到之前那个状态
    env.getCheckpointConfig.setFailOnCheckpointingErrors(false) //默认为true
    env.getCheckpointConfig.setMaxConcurrentCheckpoints(1) //在统一时间只能同时有1个checkpoint操作，其他的操作必须等当前操作执行完或者超时之后才能执行
    env.getCheckpointConfig.enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.DELETE_ON_CANCELLATION) //清除或保留状态
    env.getCheckpointConfig.setMinPauseBetweenCheckpoints(0) //下一个checkpoint操作触发之前最小的阻塞时间，必须>=0


    /** --------------------------------------配置重启策略----------------------------------------------------
     * When a task failure happens, (当一个任务失败后)
     * Flink needs to restart the failed task and other affected tasks to recover the job to a normal state.
     * （Flink 需要重启失败的任务和其他受影响的task并恢复到一个正常的状态）
     * 重启配置与checkpoint设置有关：
     * 如果没有开启checkpoint，那么重启策略为：no restart！
     * 如果开启了checkpoint，那么重启策略默认为：fixed-delay strategy is used with Integer.MAX_VALUE
     *
     * restart-strategy 可以在flink-conf.yaml中进行设置，也可以通过env.setRestartStrategy（）设置
     */


    /*env.setRestartStrategy(
      RestartStrategies.failureRateRestart(
        10,
        Time.minutes(5),
        Time.seconds(10))
    )*/

    //env.setRestartStrategy(new RestartStrategies.FallbackRestartStrategyConfiguration) //自动按照fixed-dalay重启策略

    /*env.setRestartStrategy(
      new RestartStrategies.FailureRateRestartStrategyConfiguration(
      10,
      Time.minutes(5),
      Time.seconds(10)))*/

    //env.setRestartStrategy(new RestartStrategies.NoRestartStrategyConfiguration())

    //env.setRestartStrategy(new RestartStrategies.FixedDelayRestartStrategyConfiguration(5,Time.seconds(4)))

    env.setRestartStrategy(RestartStrategies.fixedDelayRestart(5,Time.seconds(4)))

    val config = new RestartStrategies.FailureRateRestartStrategyConfiguration(3, Time.minutes(5), Time.seconds(10))
    env.setRestartStrategy(config)

    val ds: DataStream[WorkPeople] = env.addSource(new MyUDFPeopleSource)

    val ds1: DataStream[(Int, Char)] = env.fromElements((1, '男'), (2, '女'))

    val describer = new MapStateDescriptor[Int, Char]("genderInfo", classOf[Int], classOf[Char])

    val bcStream: BroadcastStream[(Int, Char)] = ds1.broadcast(describer)

    val resultStream: DataStream[People] = ds.connect(bcStream).process(
      new BroadcastProcessFunction[WorkPeople, (Int, Char), People] {
        override def processElement(value: WorkPeople,
                                    ctx: BroadcastProcessFunction[WorkPeople, (Int, Char), People]#ReadOnlyContext,
                                    out: Collector[People]): Unit = {
          val gender: Char = ctx.getBroadcastState(describer).get(value.genderCode).charValue()
          out.collect(People(value.id, value.name, gender, value.address, value.price))
        }

        override def processBroadcastElement(value: (Int, Char), ctx: BroadcastProcessFunction[WorkPeople, (Int, Char), People]#Context, out: Collector[People]): Unit = {
          ctx.getBroadcastState(describer).put(value._1, value._2)

        }
      }
    )


    ds.print("before:")
    resultStream.print("after:")


    env.execute("checkpoint")
  }
}
