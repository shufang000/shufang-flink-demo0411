package com.shufang.accumulator

import org.apache.flink.api.common.JobExecutionResult
import org.apache.flink.api.common.accumulators.{DoubleCounter, IntCounter, LongCounter}
import org.apache.flink.api.common.functions.{RichMapFunction, RuntimeContext}
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala._

object AccumulatorDemo {
  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment


    val tool: ParameterTool = ParameterTool.fromPropertiesFile("src/main/resources/netcat.properties")
    val ds: DataStream[String] = env.socketTextStream(tool.get("hostname"), tool.getInt("port"))


    //
    ds.map(
      new RichMapFunction[String, String] {

        //1、声明累加器，要在富函数中声明全局的
        val normal = new LongCounter()
        val exception = new LongCounter()
        val all = new LongCounter()


        //2、在open函数中添加累加器
        override def open(parameters: Configuration): Unit ={
          val rc: RuntimeContext = getRuntimeContext
          rc.addAccumulator("normal_temperture",normal)
          rc.addAccumulator("exception_temperture",exception)
          rc.addAccumulator("all_temperture",all)
        }

        override def map(value: String): String = {

          //进行累加
          all.add(1)
          val msg = "体温过高，需要隔离进行排查！～"
          val temper: Double = value.split(",")(3).toDouble
          if (temper > 36.4){
            //进行累加
            exception.add(1)
            s"${value.split(',')(1).trim} 的体温为 $temper, $msg"
          }else{
            //进行累加
            normal.add(1)
            s"${value.split(',')(1).trim} 的体温为 $temper, 体温正常，请进入"
          }
        }
      }
    ).print("体温结果为： ")

    val result: JobExecutionResult = env.execute("accumulator")

    //3、执行完了之后获取累加器的值
    val normal: Long = result.getAccumulatorResult[Long]("normal_temperture")
    val exception: Long = result.getAccumulatorResult[Long]("exception_temperture")
    val all: Long = result.getAccumulatorResult[Long]("all_temperture")


    println(s"normal $normal")
    println(s"exception $exception")
    println(s"all $all")

    /**
     * 体温结果为： :2> sensorreading03 的体温为 33.8, 体温正常，请进入
     * 体温结果为： :6> sensorreading03 的体温为 35.4, 体温正常，请进入
     * 体温结果为： :2> sensorreading01 的体温为 34.1, 体温正常，请进入
     * 体温结果为： :8> sensorreading01 的体温为 34.1, 体温正常，请进入
     * normal 16
     * exception 7
     * all 23
     */
  }
}
