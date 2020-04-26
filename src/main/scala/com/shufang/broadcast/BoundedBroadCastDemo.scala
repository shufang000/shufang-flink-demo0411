package com.shufang.broadcast

import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.api.scala._
import org.apache.flink.configuration.Configuration


import scala.collection.mutable


object BoundedBroadCastDemo {
  def main(args: Array[String]): Unit = {


    val env: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment

    val ds1: DataSet[(Int, Char)] = env.fromElements((1, '男'), (2, '女'))

    val ds2: DataSet[(Int, String, Int, String, Int)] = env.fromElements((1001, "小红", 2, "北京市望京西里", 8888), (1002, "Mike", 1, "上海市爱民广场", 1000), (1003, "小小", 2, "深圳市财富港", 1888))



    //ds2是主set，一般将比较小的ds1放进一个Map中
    ds2.map(new RichMapFunction[(Int, String, Int, String, Int), (Int, String, Char, String, Int)] {

      //用来存储从广播变量中读取的数据到TaskManager的内存中
      //这里的map存储的是性别信息，也就是ds1中的类型（Int，Char），接下来我们需要在open方法中对map进行初始化
      //NOTE:这里的container默认声明的是一个scala中的不可变map：immutable.Map[A, B],我们需要手动声明mutable
      var container: mutable.Map[Int, Char] = _

      /**
       * 用来进行初始化操作，这个方法只会执行一次
       *
       * @param parameters
       */
      override def open(parameters: Configuration): Unit = {

        //1.首先对容器进行初始化,这里不能用new mutable.Map(),而应该使用apply之后的mutable.Map()
        container = mutable.Map()

        //2.获取广播变量中的广播数据,这里返回的是一个java的list，我们需要手动声明，因为scala的底层没有这么智能
        val list: java.util.List[(Int, Char)] = getRuntimeContext.getBroadcastVariable("genderInfo")

        //需要导入这个类，通过将java中的list转化成scala中的list
        import scala.collection.JavaConversions._
        //3.将获取到的数据一次遍历添加到container中，不能用.+,因为+会形成一个新的container
        for (item <- list) {
          container.put(item._1, item._2)
        }

      }

      /**
       * 用来处理map方法中的逻辑与转换,每次处理dataset中的一个元素，这个方法都会触发一次
       *
       * @param value
       * @return
       */
      override def map(value: (Int, String, Int, String, Int)): (Int, String, Char, String, Int) = {

        println(Thread.currentThread().getName)
        val key: Int = value._3
        val gender: Char = container.getOrElse(key, 'x')
        (value._1, value._2, gender, value._4, value._5)

      }

      /**
       * 用来关闭开启的资源,如果没有 ，可以不用管，这个方法也只会被调用一次
       */
      override def close(): Unit = super.close()
    }
    ).withBroadcastSet(ds1, "genderInfo")
      .print()

    /**
     * 最终的结果与join操作的一致：
     * (1001,小红,女,北京市望京西里,8888)
     * (1002,Mike,男,上海市爱民广场,1000)
     * (1003,小小,女,深圳市财富港,1888)
     */

  }
}


