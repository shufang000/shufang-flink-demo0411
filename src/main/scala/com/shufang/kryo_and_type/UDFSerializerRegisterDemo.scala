package com.shufang.kryo_and_type

import com.esotericsoftware.kryo.serializers.DefaultSerializers.KryoSerializableSerializer
import com.shufang.entities.SensorReading
import com.twitter.chill.protobuf.ProtobufSerializer
import com.twitter.chill.thrift.TBaseSerializer
import org.apache.flink.api.common.typeinfo.{BasicTypeInfo, TypeHint, TypeInformation}
import org.apache.flink.api.java.typeutils.runtime.kryo.KryoSerializer
import org.apache.flink.streaming.api.scala._

object UDFSerializerRegisterDemo {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    import org.apache.flink.api.scala._

    val sensorInformation: TypeInformation[SensorReading] = createTypeInformation[SensorReading]
    sensorInformation.createSerializer(env.getConfig)


    val info2: TypeInformation[SensorReading] = TypeInformation.of(classOf[SensorReading])
    // BasicTypeInfo<T> extends TypeInformation<T> implements AtomicType<T>
    // 所以BasicTypeInfo也能反正一个类的基本信息，同时也可以用来生来序列化器
    val info3: BasicTypeInfo[SensorReading] = new BasicTypeInfo[SensorReading]
    val info4: BasicTypeInfo[java.math.BigDecimal] = BasicTypeInfo.BIG_DEC_TYPE_INFO


    //注册一个类，
    //env.registerType()
    //env.addDefaultKryoSerializer()


    //给一个类型添加默认的Kryo序列化方式
    env.getConfig.registerTypeWithKryoSerializer(classOf[SensorReading],classOf[KryoSerializableSerializer])

    //TODO 添加外部序列化系统
    //Apache Thrift
    env.getConfig.registerTypeWithKryoSerializer(classOf[SensorReading],classOf[TBaseSerializer])
    //Google ProtoBuf
    env.getConfig.registerTypeWithKryoSerializer(classOf[SensorReading],classOf[ProtobufSerializer])


    //常规优化：用自定义类POJO代替TupleX



  }
}
