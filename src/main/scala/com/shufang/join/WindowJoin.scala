package com.shufang.join

import com.shufang.broadcast.People
import com.shufang.entities.WorkPeople
import com.shufang.source.{GenderInfoSource, MyUDFPeopleSource}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.{ProcessingTimeSessionWindows, SlidingProcessingTimeWindows, TumblingProcessingTimeWindows}
import org.apache.flink.streaming.api.windowing.time.Time

//Flink也是懒加载的 lazy
object WindowJoin {

  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment


    val genderinfo: DataStream[(Int, Char)] = env.addSource(new GenderInfoSource)

    val people: DataStream[WorkPeople] = env.addSource(new MyUDFPeopleSource)



    //滑动窗口join
    /*people.join(genderinfo)
        .where(_.genderCode)
        .equalTo(_._1)
        .window(TumblingProcessingTimeWindows.of(Time.seconds(2)))
        .apply((people,tuple) => People(people.id,people.name,tuple._2,people.address,people.price))
        .print("joinstream ==> ").setParallelism(1)*/


    //滚动窗口join
    /*people.join(genderinfo)
      .where(_.genderCode)
      .equalTo(_._1)
      .window(SlidingProcessingTimeWindows.of(Time.seconds(6),Time.seconds(2)))
      .apply((people, tuple) => People(people.id, people.name, tuple._2, people.address, people.price))
      .print("joinStream ==> ").setParallelism(1)*/

    //会话窗口join
    people.join(genderinfo)
      .where(_.genderCode)
      .equalTo(_._1)
      .window(ProcessingTimeSessionWindows.withGap(Time.seconds(1)))
      .apply((people, tuple) => People(people.id, people.name, tuple._2, people.address, people.price))
      .print("joinStream ==> ").setParallelism(1)
    env.execute("join demo")



  }
}
