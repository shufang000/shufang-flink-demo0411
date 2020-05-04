package com.shufang.table

import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.{Table, TableEnvironment}
import org.apache.flink.table.api.scala.StreamTableEnvironment
import org.apache.flink.table.sinks.CsvTableSink
import org.apache.flink.table.sources.CsvTableSource

/** TableEnv的作用：
 * Registering a Table in the internal catalog  ：注册内部目录中的一个表
 * Registering an external catalog      ：注册一个外部目录
 * Executing SQL queries    ：执行SQL查询
 * Registering a user-defined (scalar, table, or aggregation) function ：注册用户自定义函数
 * Converting a DataStream or DataSet into a Table  ：将DataStream\DataSet转成 => Table
 * Holding a reference to an ExecutionEnvironment or StreamExecutionEnvironment  ：维持一个环境引用
 */
object RegisterTableInTheCatalog {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment


    //1 、创建一个Flink的表环境TableEnv
    val tableEnv: StreamTableEnvironment = TableEnvironment.getTableEnvironment(env)

    //TODO 1.1 在内部目录上注册个table，注册的table就相当于关系型数据库中查询出来的一个VIEW视图
    //Table在这里是没有经过优化的，但是在其他查询引用该表时是串联的，如果多个查询引用同一个registered Table
    //那么它会为每个引用的查询串行的执行多次，table的查询结果不是共享的
    val table: Table = tableEnv.scan("").select("")
    tableEnv.registerTable("testTable",table)


    //TODO 1.2 创建一个table-source
    //Flink为了给其他数据源读取的数据提供通用的数据格式和数据存储，给用户提供了TableSource（指定格式数据、消息队列、其他数据库）
    //如何注册一个TableSource？？？
    //tableEnv.registerTableSource("sourcename",new CsvTableSource())


    //TODO tranform a source table

    //TODO 1.3 创建一个table-sink
    //为了将tableAPI、SQLquery查询出的数据发送到其他外部存储系统，Flink提供了TableSink接口
    //外部系统如：DB、KVStore、Files、MassageQueue、FileSystem......
    //tableEnv.registerTableSink("sinkname",new CsvTableSink())











    //注册一个表源
    //    tableEnv.registerTableSource("tablesource",new CsvTableSource(".../path",.....))
    //注册一个表输出
    //    tableEnv.registerTableSink("tablesink",new KafkaTableSink())




  }
}
