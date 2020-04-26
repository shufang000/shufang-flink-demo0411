package com.shufang

import java.util.{Collections, Properties}

import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.serialization._
import org.apache.flink.api.common.typeinfo.BasicTypeInfo
import org.apache.flink.api.common.typeutils.TypeSerializer
import org.apache.flink.api.java.typeutils.runtime.ValueSerializer
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.streaming.util.serialization.{KeyedDeserializationSchema, KeyedDeserializationSchemaWrapper, TypeInformationKeyValueSerializationSchema}
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import scala.collection.JavaConversions._

/**
 * NOTE:重要是是DeSerializationSchema指定,
 * 这里DeSerializationSchema分为2种：
 * interface DeSerializationSchema[T] extends Serializable, ResultTypeQueryable :-> 用来将kafka中的二进制数据的【value】反序列化成Java/Scala中的Object对象
 * KeyedDeSerializationSchema[T] -> 用来将kafka中的二进制数据的【key/value】反序列化成Java/Scala中的Object对象
 *
 *
 */
object KafkaConsumerUtil {


  /**
   * 构造器一，通过测试，可用
   *
   * @param topic
   * @param deserier
   * @param props
   * @return
   */
  def getKafkaConsumer1(topic: String, deserier: DeserializationSchema[String], props: Properties): FlinkKafkaConsumer[String] = {
    new FlinkKafkaConsumer[String](topic, deserier, props)
  }

  /**
   * 构造器二
   *
   * @return
   */
  def getKafkaConsumer2(topic: String, props: Properties): FlinkKafkaConsumer[String] = {
    new FlinkKafkaConsumer[String](
      topic,
      new KeyedDeserializationSchemaWrapper[String](new SimpleStringSchema()),
      props
    )
  }

  /**
   * 构造器三
   *
   * @param
   * @return
   */
  def getKafkaConsumer3(topic:String): FlinkKafkaConsumer[String] = {
    new FlinkKafkaConsumer[String](
      Collections.singletonList(topic),
            new TypeInformationSerializationSchema[String](BasicTypeInfo.STRING_TYPE_INFO,new ExecutionConfig),
      new Properties()
    )
  }

  /**
   * 构造器四
   */
    def getKafkaConsumer4(topic:String):FlinkKafkaConsumer[String]={
      new FlinkKafkaConsumer[String](
        Collections.singletonList(topic),
        new KeyedDeserializationSchemaWrapper[String](new SimpleStringSchema()),
        new Properties()
      )
    }


//  def getKafkaConsumer5(topic:String):FlinkKafkaConsumer[String]={
//    val config: ExecutionConfig = new ExecutionConfig
//
//    new FlinkKafkaConsumer[String, String](
//      Collections.singletonList(topic),
//      new TypeInformationKeyValueSerializationSchema[String,String](
//        BasicTypeInfo.STRING_TYPE_INFO,
//        BasicTypeInfo.STRING_TYPE_INFO,
//        config
//      ),
//      new Properties()
//    )
//  }




//  new FlinkKafkaConsumer[String](
//    //通过正则表达式来消费指定0-9主题的数据
//    java.util.regex.Pattern.compile("flink_topic-[0-9]"),
//    new SimpleStringSchema,
//    new Properties()
//  )


}
