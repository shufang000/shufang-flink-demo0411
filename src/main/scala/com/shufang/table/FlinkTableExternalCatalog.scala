package com.shufang.table

import java.util.Collections

import com.shufang.entities.SensorReading
import com.shufang.source.MyUDFSensorReadingSource
import org.apache.flink.streaming.api.scala._
import org.apache.flink.table.api.{Table, TableEnvironment}
import org.apache.flink.table.api.scala.StreamTableEnvironment
import org.apache.flink.table.catalog.InMemoryExternalCatalog


/**
 * 一旦注册了外部目录，所有定义在外部目录中的表都能通过TableAPI｜SQL query用全路径进行访问
 * 如： catalog.database.table
 */
object RegisterExternalCatalog {
  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    val tableEnv: StreamTableEnvironment = TableEnvironment.getTableEnvironment(env)

    val sensorStream: DataStream[SensorReading] = env.addSource(new MyUDFSensorReadingSource)
    //注册外部的内存目录
    //tableEnv.registerExternalCatalog("external-catalog",new InMemoryExternalCatalog("external catalog"))


    //创建table
    val table: Table = tableEnv.fromDataStream(sensorStream)

    //注册table
    tableEnv.registerTable("sensor",table)

    //注册完之后就能用了
//    table.select('id,'name,'timestamp,'temperture)
    val strings: Array[String] = tableEnv.listTables()



  }
}
