package com.shufang.source

import com.shufang.entities.SensorReading
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.source.{RichSourceFunction, SourceFunction}

import scala.collection.immutable
import scala.util.Random

class MyUDFSensorReadingSource extends RichSourceFunction[SensorReading] {

  //自定义一个随机变量
  var random: Random = _
  var flag: Boolean = _
  var sendorReading: String = _

  override def open(parameters: Configuration): Unit = {
    //进行初始化
    random = new Random()
    flag = true
    sendorReading = "sendorReading100"

  }


  override def run(ctx: SourceFunction.SourceContext[SensorReading]): Unit = {
    //封装
    val names: Array[String] = (1 to 9).map(
      num =>
        sendorReading + num
    ).toArray

    while (flag) {
      //1001,sensorreading01,1587778791011,34.1
      val name: String = names(math.abs(random.nextInt(names.size)))
      val id: Int = 1000+math.abs(random.nextInt(1000))
      val timestamp: Long = System.currentTimeMillis()
      val temperture: Double = 36.0 + random.nextGaussian()

      val reading: SensorReading = SensorReading(id, name, timestamp, temperture)
      //传输
      ctx.collect(reading)

      Thread.sleep(2000)
    }


  }

  override def cancel(): Unit = {
    flag = false
  }
}
