package com.shufang.state

import com.shufang.broadcast.People
import com.shufang.entities.WorkPeople
import com.shufang.source.MyUDFPeopleSource
import org.apache.flink.streaming.api.functions.{KeyedProcessFunction, ProcessFunction}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

object MapStateDemo {
  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    val ds: DataStream[WorkPeople] = env.addSource(new MyUDFPeopleSource)



    env.execute("-->")
  }
}
