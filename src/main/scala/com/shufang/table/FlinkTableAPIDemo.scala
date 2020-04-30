package com.shufang.table

import com.shufang.broadcast.People
import com.shufang.entities.WorkPeople
import com.shufang.source.{GenderInfoSource, MyUDFPeopleSource}
import org.apache.calcite.schema.Table
import org.apache.flink.api.java.ExecutionEnvironment
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.{SlidingProcessingTimeWindows, TumblingProcessingTimeWindows}
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.table.api
import org.apache.flink.table.api._
import org.apache.flink.table.api.scala.StreamTableEnvironment


/************************
 * Flink的Table分为2中类型：
 * 1、 InputTable[输入表]
 * 2、 OutputTable[输出表]
 ************************/
object FlinkTableAPIDemo {
  def main(args: Array[String]): Unit = {
    //获取执行环境
    val streamEnv: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    //val batchEnv: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment

    //创建一个Table环境
    val streamTableEnv: StreamTableEnvironment = TableEnvironment.getTableEnvironment(streamEnv)
    //val batchTableEnv: java.BatchTableEnvironment = TableEnvironment.getTableEnvironment(batchEnv)

    val people: DataStream[WorkPeople] = streamEnv.addSource(new MyUDFPeopleSource)
    val genderInfo: DataStream[(Int, Char)] = streamEnv.addSource(new GenderInfoSource)

    //--------------------------------------------------------------------------------------------------

    //获取一个dataStream
    val joinStream: DataStream[People] = people.join(genderInfo)
      .where(_.genderCode)
      .equalTo(_._1)
      .window(TumblingProcessingTimeWindows.of(Time.seconds(10)))
      .apply((people, tuple) => new People(people.id, people.name, tuple._2, people.address, people.price))




    //注册一个table
    streamTableEnv.registerDataStream("workPeople",joinStream)

    streamTableEnv.sqlQuery("select * from workPeople")
    //将DataStream\DataSet转化成一个Table
    val table: api.Table = streamTableEnv.fromDataStream(joinStream)

    //将Table转化成一个DataStream\DataSet





    streamEnv.


    streamTableEnv.execute("table-api")
  }
}
