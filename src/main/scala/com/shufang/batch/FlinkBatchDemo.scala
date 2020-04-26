package com.shufang.batch

import org.apache.flink.api.scala.{DataSet, ExecutionEnvironment}

/**
 * 在scala1.9+以后flink融合blink的源码
 * 同时统一了DatastreamAPI和dataSetAPI的runtime执行过程，都属于留式处理
 */
object FlinkBatchDemo {

  def main(args: Array[String]): Unit = {
    //1.获取环境
    val env: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment

    //2.获取数据源
    val dataset: DataSet[String] = env.readTextFile("src/main/data", "UTF-8")
    //3.转换
    import org.apache.flink.api.scala._ //这个依赖是必须要导入的，因为涉及到许多的implicit方法的调用
    dataset.flatMap(_.split("\\s+")) //正则匹配
      .filter(_.nonEmpty)
      .map((_, 1))
      .groupBy(0) //boundedflow的时候才可以groupby，stream的时候是keyby
      .sum(1)
      .print()

      //.writeAsCsv(s"$hdfspath",FileSystem.WriteMode.OVERWRITE)
      //如果需要写入或者读取HDFS的文件，我们需要引入hadoop-common依赖和hadoop-client的依赖

    //4.关闭资源，
    // 在DataSet的时候，不需要调用env.execute(a:String)
    // 但是在DataStream的时候就需要显示的调用

    }
}
