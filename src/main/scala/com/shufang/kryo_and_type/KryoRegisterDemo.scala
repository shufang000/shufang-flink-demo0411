package com.shufang.kryo_and_type

import com.esotericsoftware.kryo.serializers.DefaultSerializers.KryoSerializableSerializer
import com.shufang.entities.SensorReading
import com.shufang.source.MyUDFSensorReadingSource
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.api.common.typeinfo.{BasicTypeInfo, TypeInformation}
import org.apache.flink.api.common.typeutils.TypeSerializer
import org.apache.flink.api.scala.DataSet
import org.apache.flink.streaming.api.scala._
//import org.apache.flink.api.scala._


/**
 * @REMEMBER 如果报typeInformation相关的异常，那么请选择性导入相关的包（二选一）：
 *           import org.apache.flink.streaming.api.scala._
 *           import org.apache.flink.api.scala._ （No Implicit Value for Evidence Parameter Error）
 *           在Flink程序中，
 * @KEYWORDS
 * hint 	  暗示，指示
 * frequent  频繁的，经常的
 * interact  交互
 * fall_back 后退
 * manually  手动的
 * Composite 复合的
 * particular 特别、尤其
 * elaborate 精细的
 * manifests 明显的、显而易见的
 */

object KryoRegisterDemo {

  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    //提高性能，注册子类型,可用于常用继承类的注册以及自定义类的注册，默认按照Flink的默认序列化方式进行序列化
    env.registerType(classOf[SensorReading])

    //为指定类型添加默认的序列化方式：给SensorReading在传输的过程中以Kryo序列化的方式进行序列化
    env.addDefaultKryoSerializer(classOf[SensorReading], classOf[KryoSerializableSerializer])


    /**
     * @1 创建一个TypeInformation：
     *    TypeInformation所有类型描述符的基类，它揭示了所有类的基本属性，并可以生成序列化器！～
     *    1）TypeInformation.of(classOf[SensorReading])
     *    2）createTypeInformation[SensorReading]
     *    3）BasicTypeInfo.STRING_TYPE_INFO
     */
    val stringtypeinfo: BasicTypeInfo[String] = BasicTypeInfo.STRING_TYPE_INFO
    val sensorReadingInfo: TypeInformation[SensorReading] = TypeInformation.of(classOf[SensorReading])
    //TODO 这里必须导入import org.apache.flink.streaming.api.scala._
    //createTypeInformation，must import org.apache.flink.streaming.api.scala._
    val srInfo: TypeInformation[SensorReading] = createTypeInformation[SensorReading]
    val tupleInfo: TypeInformation[(String, SensorReading)] = createTypeInformation[(String, SensorReading)]
    val uniontupleInfo: TypeInformation[(SensorReading, (String, SensorReading))] = createTuple2TypeInformation(srInfo, tupleInfo)


    /**
     * @2 通过typeInfo实例创建对应的序列化器
     *    1)  env.getConfig
     *    2)  getRuntimeContext().getExecutionConfig
     *    3)  ds.getExecutionConfig
     */
    val ds: DataStream[SensorReading] = env.addSource(new MyUDFSensorReadingSource)
    sensorReadingInfo.createSerializer(env.getConfig)
    sensorReadingInfo.createSerializer(ds.getExecutionConfig)

    ds.map(new RichMapFunction[SensorReading, String] {
      override def map(value: SensorReading): String = {
        val config: ExecutionConfig = getRuntimeContext.getExecutionConfig
        val ts: TypeSerializer[SensorReading] = sensorReadingInfo.createSerializer(config)
        value.name + ts.toString
      }
    }).print()

    /**
     * 这里的_相当于不关心的类型、_还可以给默认值、case _、只用一次的当前对象
     * 1） 声明类型 input:DataSet[(String,_)]
     * 2)  给默认值 var name:String = _
     * 3)  case _ =>  do something
     * 4)  map((_,x))
     * ........ `_`还有很多用法 ，有兴趣的可以参考scala官网
     */
    def selectFirst[T](input: DataSet[(T, _)]): DataSet[T] = {
      input.map { v => v._1 }
    }
    //    val data : DataSet[(String, Long) = env
    //    val result = selectFirst(data)


    //Flink自动识别为POJO的要求
    /** ***********************************************************************
     * 1、必须是public的独立类（没有非静态内部类）
     * 2、必须有公共的无参构造器
     * 3、类中的所有非静态，非瞬态成员都是公共的，不能是最终的final，必须有对应的getter\setter方法
     * ************************************************************************/


    //Flink默认使用Flink自带的序列化方式，可以通过下面
    env.getConfig.disableGenericTypes()
    env.getConfig.disableForceKryo()
    env.execute("type and serializer")
  }
}
