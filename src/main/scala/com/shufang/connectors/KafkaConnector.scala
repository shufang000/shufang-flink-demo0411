package com.shufang.connectors

import java.util.Properties

import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.common.time.Time
import org.apache.flink.streaming.api.{CheckpointingMode, TimeCharacteristic}
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer, FlinkKafkaProducer}
import org.apache.flink.streaming.util.serialization.KeyedSerializationSchemaWrapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer

object KafkaConnector {
  def main(args: Array[String]): Unit = {

    // 设置runtime config
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.enableCheckpointing(5000)
    env.getCheckpointConfig.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE)
    env.getCheckpointConfig.setCheckpointTimeout(6000)


    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)


    import org.apache.flink.streaming.api.scala._
    //获取kafka数据流
    val kafkaPropertiesConsumer = new Properties()


    kafkaPropertiesConsumer.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    //指定消费者组
    kafkaPropertiesConsumer.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "flinkConsumer")
    //指定key的反序列化类型
    kafkaPropertiesConsumer.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    //指定value的反序列化类型
    kafkaPropertiesConsumer.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)

    val kafkaConsumer = new FlinkKafkaConsumer[String](
      "testflink",
      new SimpleStringSchema(),
      kafkaPropertiesConsumer
    )
    //开启offset，在checkpoint的时候可提交，可重新消费，保证source阶段的容错性，数据不丢失
    kafkaConsumer.setCommitOffsetsOnCheckpoints(true)

    val kafkaStream: DataStream[String] = env.addSource[String](kafkaConsumer)

    kafkaStream.print()

    //    TODO SOMETHING TO TRANSFER THE DATASTREAM

    //写入到kafka sink
    //    testflinkdwi
    //    val producerConfig = new Properties()
    //    producerConfig.put(ProducerConfig.ACKS_CONFIG, new Integer(1)) // 设置producer的ack传输配置
    //    producerConfig.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, Time.hours(2)) //设置超市时长，默认1小时，建议1个小时以上
    //    producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092") //设置超市时长，默认1小时，建议1个小时以上

    val kafkaProducer: FlinkKafkaProducer[String] = new FlinkKafkaProducer[String](
      "localhost:9092",
      "testflinkdwi",
      new SimpleStringSchema()
    )

    /**
     * kafka 0.10及以前的版本的，必须设置这个
     *     kafkaProducer.setLogFailuresOnly(false)
     *     kafkaProducer.setFlushOnCheckpoint(true)
     *     同时在配置文件中添加 reries = ？？？，设置适当的重试次数
     *     -----------------
     * kafka 0.11+以后只需要在Producer的构造其中指定 FlinkKafkaProducer.Semantic.EXACTLY_ONCE就行了
     */
    kafkaProducer.setLogFailuresOnly(false)
//    kafkaProducer.setFlushOnCheckpoint(true)
    kafkaProducer.setWriteTimestampToKafka(true) //在sink到下个系统时，往消息中添加时间戳

    //这个就是往这里面写
    kafkaStream.addSink(kafkaProducer)

    //TODO SOMETHING TO CONFIG THE SINK
    env.execute("kafka-connector")
  }
}
