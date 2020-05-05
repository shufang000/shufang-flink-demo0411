package com.shufang.connectors

import java.util.Properties

import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer

object KafkaSourceFromConfigFile {
  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    //将所有的配置信息都写进文件
    /**
     * bootstrap.servers=localhost:9092
     * group.id=linkConsumer
     * key.deserializer=org.apache.kafka.common.serialization.StringDeserializer
     * value.deserializer=org.apache.kafka.common.serialization.StringDeserializer
     * topic=flink_topic
     */
    val kafkaConfig = new Properties()
    kafkaConfig.load(this.getClass.getClassLoader.getResourceAsStream("kafka_consumer.properties"))

    //通过Properties来获取配置文件中的属性
    val topic: String = kafkaConfig.getProperty("topic")
    println(topic)

    val kafkaConsumer: FlinkKafkaConsumer[String] = new FlinkKafkaConsumer[String](
      topic,
      new SimpleStringSchema(),
      kafkaConfig
    )
    //从每个分区的最开始开始读取
    kafkaConsumer.setStartFromEarliest()

    //保证高可用
    kafkaConsumer.setCommitOffsetsOnCheckpoints(true)

    //创建sourceStream并打印
    env.addSource(kafkaConsumer).print()


    //执行
    env.execute("config-kafka")
  }
}
