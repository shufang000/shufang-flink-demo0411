package com.shufang.broadcast

import java.lang
import java.nio.charset.Charset
import java.util.{Collections, Properties}

import com.shufang.KafkaConsumerUtil
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.serialization.{AbstractDeserializationSchema, DeserializationSchema, TypeInformationSerializationSchema}
import org.apache.flink.api.common.state.MapStateDescriptor
import org.apache.flink.api.common.typeinfo.BasicTypeInfo
import org.apache.flink.api.common.typeutils.TypeSerializer
import org.apache.flink.api.common.typeutils.base.{CharSerializer, StringSerializer}
import org.apache.flink.api.java.typeutils.runtime.ValueSerializer
import org.apache.flink.streaming.api.datastream.BroadcastStream
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.streaming.connectors.kafka.internals.KafkaTopicPartition
import org.apache.flink.streaming.util.serialization.{KeyedDeserializationSchemaWrapper, SimpleStringSchema, TypeInformationKeyValueSerializationSchema}
import org.apache.flink.table.sources.wmstrategies.BoundedOutOfOrderTimestamps
import org.apache.flink.util.Collector
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{IntegerSerializer, StringDeserializer}

import scala.collection.mutable

object UnboundedBroadCastDemo02 {

  def main(args: Array[String]): Unit = {

    //一、获取环境、获取2个DataStream
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.getCheckpointConfig.setMaxConcurrentCheckpoints(5)
    env.getCheckpointConfig.setCheckpointInterval(1000)
    env.enableCheckpointing(6000)

    val ds1: DataStream[(Int, Char)] = env.fromElements((1, '男'), (2, '女'))


    val properties: Properties = new Properties()

    //指定kafka的启动集群
    properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    //指定消费者组
    properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "flinkConsumer")
    //指定key的反序列化类型
    properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    //指定value的反序列化类型
    properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    //指定自动消费的策略
    //properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    //    println(properties)




    //一
    //    val kafkaConsumer: FlinkKafkaConsumer[String] = new FlinkKafkaConsumer[String](
    //      "flink_topic",
    //      new KeyedDeserializationSchemaWrapper[String](new SimpleStringSchema(Charset.defaultCharset())),
    //      properties
    //    )
    //二
    //      val kafkaConsumer: FlinkKafkaConsumer[String] = new FlinkKafkaConsumer[String](
    //        "flink_topic",
    //        new AbstractDeserializationSchema[String]() {
    //          override def deserialize(message: Array[Byte]): String = {
    //            val msg = new String(message)
    //            msg
    //          }
    //        },
    //        properties
    //      )

    //三
    val kafkaConsumer = new FlinkKafkaConsumer[String](
      "flink_topic",
      new KeyedDeserializationSchemaWrapper[String](new AbstractDeserializationSchema[String]() {
        override def deserialize(message: Array[Byte]): String = {
          new String(message)
        }
      }),
      properties
    )


    //保证exactly-once
    kafkaConsumer.setCommitOffsetsOnCheckpoints(true)



    //指定时间戳字段，发射watermark到下一级
    //    kafkaConsumer.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[String](Time.seconds(1)) {
    //      override def extractTimestamp(element: String): Long = {
    //        element.split(",")(0).trim.toLong
    //      }
    //    })


    /** *************************************
     * import scala.collection.JavaConversions._ //用来将java中的类型转化成scala中能编译通过的类型
     * kafkaConsumer.setStartFromEarliest() //从最先开始消费
     * kafkaConsumer.setStartFromLatest() //从leo开始消费
     * val topPartitionAndOffset: Map[KafkaTopicPartition, lang.Long] =  Map[KafkaTopicPartition, lang.Long]()
     * topPartitionAndOffset.put(new KafkaTopicPartition("flink_topic", 0), 23L)
     * topPartitionAndOffset.put(new KafkaTopicPartition("flink_topic", 1), 24L)
     * topPartitionAndOffset.put(new KafkaTopicPartition("flink_topic", 2), 25L)
     * kafkaConsumer.setStartFromSpecificOffsets(topPartitionAndOffset) //不同的分区指定不同的offset开始消费
     * auto.offset.reset //假如以上的设置的不生效，就使用该配置的默认配置进行消费
     */

    //kafkaconsumer也支持指定不同的水印发射器
    // kafkaConsumer.assignTimestampsAndWatermarks()

    //保证offset，默认是true，如果checkpoint关闭情况下，offset的提交会依赖于
    // enable.auto.commit = true ,auto.commit.interval.ms = 适当的值
    // kafkaConsumer.setCommitOffsetsOnCheckpoints(true)


    val genderInfo: MapStateDescriptor[Integer, Character] = new MapStateDescriptor(
      "genderInfo",
      //      new IntegerSerializer,
      //      new CharSerializer
      //      classOf[Integer],
      //      classOf[Character]

      BasicTypeInfo.INT_TYPE_INFO, //指定广播的k的类型
      BasicTypeInfo.CHAR_TYPE_INFO //指定广播的v的类型
    )


    val bcStream: BroadcastStream[(Int, Char)] = ds1.broadcast(genderInfo)

    val ds2: DataStream[String] = env.addSource(kafkaConsumer)

    val ds3: DataStream[(Int, String, Int, String, Double)] = ds2.map(line => {
      val pros: Array[String] = line.split(",")
      val id: Int = pros(0).trim.toInt
      val name: String = pros(1).trim
      val gendercode: Int = pros(2).trim.toInt
      val address: String = pros(3).trim
      val price: Double = pros(4).trim.toDouble
      (id, name, gendercode, address, price)
    }
    )

    /**
     * 通过connect来连接广播stream，然后调用原生的process方法进行处理
     */
    ds3.connect(bcStream).process(
      new BroadcastProcessFunction[(Int, String, Int, String, Double), (Int, Char), People] {

        println(s"${Thread.currentThread().getName}")

        override def processElement(value: (Int, String, Int, String, Double), ctx: BroadcastProcessFunction[(Int, String, Int, String, Double), (Int, Char), People]#ReadOnlyContext, out: Collector[People]): Unit = {

          val gendercode: Int = value._3

          val gender: Char = ctx.getBroadcastState(genderInfo).get(gendercode).charValue()

          val people: People = People(value._1, value._2, gender, value._4, value._5)
          out.collect(people)
        }

        override def processBroadcastElement(value: (Int, Char),
                                             ctx: BroadcastProcessFunction[(Int, String, Int, String, Double), (Int, Char), People]#Context,
                                             out: Collector[People]): Unit = {

          ctx.getBroadcastState(genderInfo).put(value._1, value._2)
        }
      }
    ).print("Stream")


    //开始执行
    env.execute("stream-broadcast")

  }
}

case class People(id: Int, name: String, gender: Char, address: String, price: Double)
