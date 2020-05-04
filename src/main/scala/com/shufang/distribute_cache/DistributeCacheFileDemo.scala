package com.shufang.distribute_cache

import java.io.File

import com.shufang.broadcast.People
import com.shufang.entities.WorkPeople
import com.shufang.source.MyUDFPeopleSource
import org.apache.commons.io.FileUtils
import org.apache.flink.api.common.JobExecutionResult
import org.apache.flink.api.common.functions.{RichMapFunction, RuntimeContext}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala._

import scala.collection.mutable
import scala.io.{BufferedSource, Source}

/**
 * 1⃣ 首先将gender.txt文件上传到hdfs上的指定目录/flink/cache
 * 2⃣ 然后通过获取环境 -> 创建流 -> 使用RichFunction or ProcessFunction，env.registerCachedFile（ , ）获取需要被广播的File
 * 3⃣ 然后获取File中的内容，将内容放在指定的容器里面，等待被调用，这里的容器是一个container: mutable.HashMap[Int,Char]()
 */
object DistributeCacheFileDemo {

  def main(args: Array[String]): Unit = {

    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    /** gender.txt中的文件格式如下
     * 1,女
     * 2,男
     */
    env.registerCachedFile("hdfs:///flink/cache", "gender.txt", true)

    //怎么访问?通过RichFunction or ProcessFunction进行访问
    val ds: DataStream[WorkPeople] = env.addSource(new MyUDFPeopleSource)

    ds.map(new RichMapFunction[WorkPeople, People] {

      //初始化容器
      var container: mutable.Map[Int, Char] = _
      var file: File = _
      var bufferedSource: BufferedSource = _


      override def open(parameters: Configuration): Unit = {

        //初始化，执行一次
        container = new mutable.HashMap[Int, Char]()
        //读取缓存数据，将数据加载在内容容器中
        val rc: RuntimeContext = getRuntimeContext
        file = rc.getDistributedCache.getFile("gender.txt")

        //FileUtils.readFileToString()
        bufferedSource = Source.fromFile(file)
        val list: List[String] = bufferedSource.getLines().toList

        for (line <- list){
          val id: Int = line.split(",")(0).trim.toInt
          val gender: Char = line.split(",")(1).trim.toCharArray()(0)
          container.put(id,gender)
        }
      }

      //实现逻辑，进行转换
      override def map(value: WorkPeople): People = {
        val key: Int = value.genderCode
        val gender: Char = container.getOrElse(key, 'x')
        People(value.id, value.name, gender, value.address, value.price)
      }

      //关闭文件 资源
      override def close(): Unit = {
        if (bufferedSource != null) bufferedSource.close()
        file.deleteOnExit()
      }
    })

    //TODO WITH RESULT
    val result: JobExecutionResult = env.execute()

  }
}
