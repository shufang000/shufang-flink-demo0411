package com.shufang.broadcast

import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.api.scala._
import org.apache.flink.configuration.Configuration

import scala.collection.mutable

object DateSetBroadCastDemo {
  def main(args: Array[String]): Unit = {

    val env: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment

    val ds1: DataSet[(Int, Char)] = env.fromElements((1, '男'), (2, '女'))

    val ds2: DataSet[(Int, String, Int, String, Int)] = env.fromElements((1001, "小红", 2, "北京市望京西里", 8888), (1002, "Mike", 1, "上海市爱民广场", 1000), (1003, "小小", 2, "深圳市财富港", 1888))


    import scala.collection.JavaConversions._
    ds2.map(
      new RichMapFunction[(Int, String, Int, String, Int), (Int, String, Char, String, Int)] {
        //声明容器用来装广播变量
        var container: mutable.Map[Int, Char] = _

        override def open(parameters: Configuration): Unit = {
          container = mutable.Map()
          val list: java.util.List[(Int, Char)] = getRuntimeContext.getBroadcastVariable("genderinfo")

          for (elem <- list) {
            container.put(elem._1, elem._2)
          }
        }

        override def map(value: (Int, String, Int, String, Int)): (Int, String, Char, String, Int) = {

          val genderCode = value._3
          val gender: Char = container.getOrElse(genderCode, 'N')
          (value._1, value._2, gender, value._4, value._5)
        }
      }
    ).withBroadcastSet(ds1, "genderinfo")
      .print()


    ds2.join(ds1)
      .where(2)
      .equalTo(0)
      .map(
        a =>
          (a._1._1, a._1._2, a._2._2, a._1._4, a._1._5)
      ).print()


    /**
     * (1001,小红,女,北京市望京西里,8888)
     * (1002,Mike,男,上海市爱民广场,1000)
     * (1003,小小,女,深圳市财富港,1888)
     * (1002,Mike,男,上海市爱民广场,1000)
     * (1001,小红,女,北京市望京西里,8888)
     * (1003,小小,女,深圳市财富港,1888)
     */
  }
}
