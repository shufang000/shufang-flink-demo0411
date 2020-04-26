package com.shufang.broadcast

import java.util.Properties

import com.shufang.KafkaConsumerUtil
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.common.state.MapStateDescriptor
import org.apache.flink.api.common.typeinfo.BasicTypeInfo
import org.apache.flink.streaming.api.datastream.BroadcastStream
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.util.Collector
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

object UnBoundedStreamBroadCastDemo02 {
  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    val kafkaPropertiesConsumer = new Properties()
    kafkaPropertiesConsumer.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    //指定消费者组
    kafkaPropertiesConsumer.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "flinkConsumer")
    //指定key的反序列化类型
    kafkaPropertiesConsumer.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    //指定value的反序列化类型
    kafkaPropertiesConsumer.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    //
    kafkaPropertiesConsumer.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"lastest")

    val ds1: DataStream[(Int, Char)] = env.fromElements((1, '男'), (2, '女'))

    val consumer: FlinkKafkaConsumer[String] = KafkaConsumerUtil.getKafkaConsumer1("flink_topic", new SimpleStringSchema(), kafkaPropertiesConsumer)

    val ds2: DataStream[(Int, String, Int, String, Double)] = env.addSource(consumer).map(line => {
      val pros: Array[String] = line.split(",")
      val id: Int = pros(0).trim.toInt
      val name: String = pros(1).trim
      val gendercode: Int = pros(2).trim.toInt
      val address: String = pros(3).trim
      val price: Double = pros(4).trim.toDouble
      (id, name, gendercode, address, price)
    })

    val genderInfo = new MapStateDescriptor[Int, Char](
      "genderinfo",
      classOf[Int],
      classOf[Char]
    )


    //获取广播流变量
    val bcStream: BroadcastStream[(Int, Char)] = ds1.broadcast(genderInfo)

    //
    val ds3: DataStream[(Int, String, Char, String, Double)] = ds2.connect(bcStream).process(
      new BroadcastProcessFunction[(Int, String, Int, String, Double), (Int, Char), (Int, String, Char, String, Double)] {

        override def processElement(value: (Int, String, Int, String, Double),
                                    ctx: BroadcastProcessFunction[(Int, String, Int, String, Double), (Int, Char), (Int, String, Char, String, Double)]#ReadOnlyContext,
                                    out: Collector[(Int, String, Char, String, Double)]): Unit = {
          val key: Int = value._3

          val gender: Char = ctx.getBroadcastState(genderInfo).get(key)

          out.collect((value._1, value._2, gender, value._4, value._5))
        }

        override def processBroadcastElement(value: (Int, Char),
                                             ctx: BroadcastProcessFunction[(Int, String, Int, String, Double), (Int, Char), (Int, String, Char, String, Double)]#Context,
                                             out: Collector[(Int, String, Char, String, Double)]): Unit =
          ctx.getBroadcastState(genderInfo).put(value._1, value._2)

      }
    )


    ds3.print()


    env.execute("stream")
  }
}
