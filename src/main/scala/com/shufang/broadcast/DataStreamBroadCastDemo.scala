package com.shufang.broadcast

import com.shufang.entities._
import com.shufang.source.MyUDFPeopleSource
import org.apache.flink.api.common.state.MapStateDescriptor
import org.apache.flink.api.common.typeinfo.{BasicTypeInfo, TypeInformation}
import org.apache.flink.streaming.api.datastream.BroadcastStream
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector

object DataStreamBroadCastDemo {
  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    val ds1: DataStream[(Int, Char)] = env.fromElements((1, '男'), (2, '女'))
    val genderinfo = new MapStateDescriptor[Int, Char]("genderinfo", classOf[Int], classOf[Char])
    val bcStream: BroadcastStream[(Int, Char)] = ds1.broadcast(genderinfo)

    val ds2: DataStream[WorkPeople] = env.addSource(new MyUDFPeopleSource)

    ds2.connect(bcStream).process(
      new BroadcastProcessFunction[WorkPeople, (Int, Char), People] {
        override def processElement(value: WorkPeople,
                                    ctx: BroadcastProcessFunction[WorkPeople, (Int, Char), People]#ReadOnlyContext,
                                    out: Collector[People]): Unit = {
          val gendercode: Int = value.genderCode

          val gender: Char = ctx.getBroadcastState(genderinfo).get(gendercode).charValue()

          out.collect(People(value.id,value.name,gender,value.address,value.price))
        }


        override def processBroadcastElement(value: (Int, Char), ctx: BroadcastProcessFunction[WorkPeople, (Int, Char), People]#Context, out: Collector[People]): Unit = {

          ctx.getBroadcastState(genderinfo).put(value._1,value._2)
        }
      }
    ).print()

    env.execute()
  }
}
