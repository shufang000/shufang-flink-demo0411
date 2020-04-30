package com.shufang.window

import java.lang

import com.shufang.entities._
import com.shufang.source.{MyUDFPeopleSource, MyUDFSensorReadingSource}
import org.apache.flink.api.common.functions.AggregateFunction
import org.apache.flink.api.java.tuple.Tuple
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.functions.windowing._
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.{EventTimeSessionWindows, GlobalWindows, SlidingEventTimeWindows, TumblingEventTimeWindows}
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.extensions._
import scala.collection.JavaConversions

/**
 * @Trigger
 * A triggering policy might be something like “when the number of elements in the window is more than 4”,
 * or “when the watermark passes the end of the window”.
 * A trigger can also decide to purge a window’s contents any time between its creation and removal.
 * Purging in this case only refers to the elements in the window, and not the window metadata.
 * This means that new data can still be added to that window.
 * @：TRANSLATE
 * 一个触发器策略就像 "当一个窗口内的元素个数达到4"或者"当watermark超过窗口的endTime" 就触发WindowFunctions内的操作；
 * @：NOTE
 * 触发器还能决定在 窗口的创建和移除时间的任意时间"清除"窗口中的内容，这里的"清除"仅仅是清除窗口中的元素，而不是窗口的元数据
 * 这就意味着=>新的数据荏苒可以被添加进当前的window（窗口）
 * @Evictor
 * Apart from the above, you can specify an Evictor (see Evictors)
 * which will be able to remove elements from the window after the trigger fires
 * and before and/or after the function is applied.
 * 你能指定淘汰器,这个淘汰器能够 在trigger触发之后 & 在窗口函数执行前后移除窗口中的元素。
 */
object KeyedWindowDemo {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val sourceStream: DataStream[SensorReading] = env.addSource(new MyUDFSensorReadingSource)

    val primaryStream: DataStream[SensorReading] = sourceStream.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor[SensorReading](Time.seconds(1)) {
      override def extractTimestamp(element: SensorReading): Long = {
        element.timestamp
      }
    })

    //import org.apache.flink.streaming.api.scala.extensions._  开启flink-scala的拓展语法支持
    val value1: DataStream[String] = primaryStream.mapWith (
       sensor => sensor + "hello"
    )

    value1.print("normal")

    val keyStream: KeyedStream[SensorReading, String] = primaryStream.keyBy(_.name)


    /** 时间窗口
     * Event_time windows
     * SlidingEventTimeWindows\TumblingEventTimeWindows\EventTimeSessionWindows
     * Processing_time windows
     */

    //滑动窗口，窗口之间可能有重叠，也可能没有重叠
    //keyedStream.window(SlidingEventTimeWindows.of(Time.minutes(5),Time.minutes(1)))
    //滚动窗口，窗口之间没有overlap重叠
    //keyedStream.window(TumblingEventTimeWindows.of())
    //这里涉及到一个session-gap的概念，假如一个窗口开启后session-gap时间内没有进入数据，
    //那么就开启下一个窗口，evaluate上一个窗口，
    //keyedStream.window(EventTimeSessionWindows.of())
    //全局窗口，全局就一个窗口
    //keyedStream.window(GlobalWindows)

    val winStream: WindowedStream[SensorReading, String, TimeWindow] = keyStream.window(SlidingEventTimeWindows.of(Time.seconds(10), Time.seconds(5)))

    //ReduceFunction  求最大温度
    /*winStream.reduce(
      (s1, s2) =>
        if (s1.temperture > s2.temperture) s1
        else s2
    ).print("resuce-function:====>").setParallelism(1)*/

    //AggregatingFunction 求最大温度
    /*winStream.aggregate(new AggregateFunction[SensorReading, SensorReading, (SensorReading, Double)] {
      override def createAccumulator(): SensorReading = {
        new SensorReading(0,"",0,0)
      }

      override def add(value: SensorReading, accumulator: SensorReading): SensorReading = {
        if (value.temperture > accumulator.temperture) value
        else accumulator
      }

      override def getResult(accumulator: SensorReading): (SensorReading, Double) = {
        (accumulator, accumulator.temperture)
      }

      override def merge(a: SensorReading, b: SensorReading): SensorReading = {
        if (a.temperture > b.temperture) a
        else b
      }
    }).print()*/


    //ProcessWindowFunction
    //如果还是简单的聚合运算，官方不建议使用processWindowFunction，建议用incremenal ReduceFuntion&AggregateFunction来代替
    //是在复杂到reduce、AggregateFunctions解决不了的场景可以选择ProcessWindowFunction，
    //只要是原生的process方法，是没有`预-聚合`的，但是功能强大，像reduce、sum都是逐步计算，并保存状态的
    //winStream.process[SensorReading]( new MyProcessWindowFunction())

//    winStream.process[String](new MyProcessWindowFunction[SensorReading,String,String,TimeWindow]())
    //TODO =====>  ??????????为什么用ProcessWindowFunction还是报错说类型mismatch


    //增量聚合的方式incremental-aggregate
    //ReduceFunction & ProcessWindowFunction



    env.execute("keyedWindowStream")
  }
}

class MyProcessWindowFunction extends ProcessWindowFunction[SensorReading, String, String, TimeWindow] {
  override def process(key: String, context: ProcessWindowFunction[SensorReading, String, String, TimeWindow]#Context, elements: lang.Iterable[SensorReading], out: Collector[String]): Unit = {

    var initReading =  SensorReading(0,"",0,0)
    import scala.collection.JavaConversions._
    for (elem <- elements){
      if(elem.temperture > initReading.temperture){
        initReading = elem
      }
    }

    out.collect(s"${initReading.name} 的温度为：${initReading.temperture}")
  }
}
