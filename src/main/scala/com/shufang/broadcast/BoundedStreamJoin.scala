package com.shufang.broadcast


import org.apache.flink.api.scala._


/**
 * 需要通过一个join操作来join2个相关的dataset
 */
object BoundedStreamJoin {
  def main(args: Array[String]): Unit = {

    val env: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment

    val ds1: DataSet[(Int, Char)] = env.fromElements((1, '男'), (2, '女'))

    val ds2: DataSet[(Int, String, Int, String, Int)] = env.fromElements((1001, "小红", 2, "北京市望京西里", 8888), (1002, "Mike", 1, "上海市爱民广场", 1000), (1003, "小小", 2, "深圳市财富港", 1888))

    ds1.join(ds2)
      .where(a => a._1)
      .equalTo(a => a._3)
      .map(a => (a._2._1, a._2._2, a._1._2, a._2._4, a._2._5))
      .print()

  }
}
