package com.shufang.broadcast

import org.apache.flink.api.common.state.MapStateDescriptor
import org.apache.flink.api.common.typeinfo.{BasicTypeInfo, TypeInfo}
import org.apache.flink.streaming.api.datastream.BroadcastStream
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.util.Collector


/**
 * 主要是实现一个无界流的广播刘变量的使用，实现mapjoin
 */
object UnboundedBroadCastDemo {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment


    val ds1: DataStream[(Int, Char)] = env.fromElements((1, '男'), (2, '女'))

    val ds2: DataStream[(Int, String, Int, String, Double)] = env.socketTextStream("localhost", 9999)
      .map(
        line => {
          val strings: Array[String] = line.split(",")
          val id: Int = strings(0).trim.toInt
          val name: String = strings(1).trim
          val gendercode: Int = strings(2).trim.toInt
          val address: String = strings(3).trim
          val price: Double = strings(4).trim.toDouble
          (id, name, gendercode, address, price)
        }
      )

    //自定义状态描述器
    val genderInfo: MapStateDescriptor[Integer, Character] = new MapStateDescriptor(
      "genderInfo",
      BasicTypeInfo.INT_TYPE_INFO,
      BasicTypeInfo.CHAR_TYPE_INFO
    )

    //通过将ds1将自己进行广播
    val bcStream: BroadcastStream[(Int, Char)] = ds1.broadcast(genderInfo)

    //这里可以尝试用mapWithState来代替process的状态管理
    ds2.connect(bcStream).process(
      new BroadcastProcessFunction[(Int, String, Int, String, Double), (Int, Char), (Int, String, Char, String, Double)]() {


        //一、首先将广播变量的信息放进taskManager的内存中，通过State.put()方法来实现
        override def processBroadcastElement(value: (Int, Char),
                                             ctx: BroadcastProcessFunction[(Int, String, Int, String, Double), (Int, Char), (Int, String, Char, String, Double)]#Context,
                                             out: Collector[(Int, String, Char, String, Double)]): Unit = {
          //put：将genderinfo状态中的k，v放进taskManager的内存中
          ctx.getBroadcastState(genderInfo).put(value._1, value._2)
        }

        //二、用来处理主datastream的方法，进行字段之间的拼接
        override def processElement(value: (Int, String, Int, String, Double),
                                    ctx: BroadcastProcessFunction[(Int, String, Int, String, Double), (Int, Char), (Int, String, Char, String, Double)]#ReadOnlyContext,
                                    out: Collector[(Int, String, Char, String, Double)]): Unit = {
          // 获取主stream需要被转换的k作为k，去状态中查询
          //1.从value中获取gender信息
          val gendercode: Int = value._3

          val gender: Char = ctx.getBroadcastState(genderInfo).get(gendercode).charValue()

          //2.封装OUT的类型
          val PeopleInfo: (Int, String, Char, String, Double) = (value._1, value._2, gender, value._4, value._5)

          //将数据传给下一个task
          out.collect(PeopleInfo)
        }
      }
    ).print(s"${Thread.currentThread().getName}:")



    //进行connect操作
    env.execute("stream-broadcast-var")

    /*********************************************
     * main::2> (1001,小红,女,北京市望京西里,1888.0)*
     * main::4> (1003,小丽,女,广州市珠江CDB,1888.0)*
     * main::3> (1002,小博,男,上海市人民广场,1888.0)*
     * main::5> (1004,小花,女,深圳市市民中心,1888.0)*
     * main::7> (1003,小丽,女,广州市珠江CDB,1888.0)*
     * main::6> (1002,小博,男,上海市人民广场,1888.0)*
     * main::8> (1004,小花,女,深圳市市民中心,1888.0)*
     * main::2> (1003,小丽,女,广州市珠江CDB,1888.0)*
     * main::1> (1002,小博,男,上海市人民广场,1888.0)*
     * main::3> (1004,小花,女,深圳市市民中心,1888.0)*
     *********************************************/

    //TODO怎么使用spark中的广播变量实现这样的效果å
  }
}
