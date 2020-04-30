package com.shufang.source

import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.source.{RichSourceFunction, SourceFunction}

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class GenderInfoSource extends RichSourceFunction[(Int, Char)] {
  var random: Random = _
  var ids: ArrayBuffer[Int] = _
  var isrunning = true

  override def open(parameters: Configuration): Unit = {
    random = new Random()
    ids = ArrayBuffer(1, 2)
  }

  override def run(ctx: SourceFunction.SourceContext[(Int, Char)]): Unit = {

    while (isrunning) {
      val id: Int = ids(random.nextInt(ids.size))
      val gender: Char = id match {
        case 1 => '男'
        case _ => '女'
      }
      ctx.collect((id, gender))

      Thread.sleep(4000)
    }
  }

  override def cancel(): Unit = {
    isrunning = false
  }

}
