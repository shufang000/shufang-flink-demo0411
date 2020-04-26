package com.shufang.connectors

import java.util.{Collections, Properties}

import com.shufang.KafkaConsumerUtil
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.serialization.{AbstractDeserializationSchema, SimpleStringSchema, TypeInformationSerializationSchema}
import org.apache.flink.api.common.typeinfo.BasicTypeInfo
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.streaming.util.serialization.KeyedDeserializationSchemaWrapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

object TestKafkaConsumerUtilDemo {
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

    //    val consumer: FlinkKafkaConsumer[String] = KafkaConsumerUtil.getKafkaConsumer1("flink_topic", new SimpleStringSchema(), kafkaPropertiesConsumer)

    //    可用
    //    val consumer = new FlinkKafkaConsumer[String](
    //      "flink_topic",
    //      new KeyedDeserializationSchemaWrapper[String](new SimpleStringSchema()),
    //      kafkaPropertiesConsumer
    //    )


    //自定义反序列化器
//    val consumer = new FlinkKafkaConsumer[String](
//      Collections.singletonList("flink_topic"),
//      new AbstractDeserializationSchema[String]() {
//        override def deserialize(message: Array[Byte]): String = {
//          val str = new String(message)
//          str
//        }
//      },
//      kafkaPropertiesConsumer
//    )





//    val ds: DataStream[String] = env.addSource(consumer)

//    ds.print()

    env.execute("streamutil")

  }
}
