package com.shufang.source

import com.shufang.entities._
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.source.{RichSourceFunction, SourceFunction}

import scala.collection.mutable.ArrayBuffer

class MyUDFPeopleSource extends RichSourceFunction[WorkPeople] {

  import scala.util.Random

  //自定义一个随机变量
  var random: Random = _
  var flag: Boolean = _
  var names: ArrayBuffer[String] = _
  var addresses: ArrayBuffer[String] = _
  var gendercodes: Array[Int] = _

  override def open(parameters: Configuration): Unit = {
    //进行初始化
    random = new Random()
    flag = true
    names = ArrayBuffer("小红", "Mike", "小小")
    gendercodes = Array(1, 2)
    addresses = ArrayBuffer("北京市望京西里", "上海市爱民广场", "深圳市财富港")
  }


  override def run(ctx: SourceFunction.SourceContext[WorkPeople]): Unit = {
    //封装
    val id: Int = 1000 + random.nextInt(3).abs

    while (flag) {
      //1001,sensorreading01,1587778791011,34.1
      val name: String = names(math.abs(random.nextInt(names.size)))
      val code: Int = gendercodes(random.nextInt(gendercodes.length).abs)
      val address: String = addresses(random.nextInt(addresses.length).abs)
      val price: Int = random.nextInt(10000).abs
      //传输
      ctx.collect(WorkPeople(id, name, code, address, price))
      Thread.sleep(2000)
    }

  }

  override def cancel(): Unit = {
    flag = false
  }
}
