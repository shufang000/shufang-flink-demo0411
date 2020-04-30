package scala_extension

import com.shufang.entities.WorkPeople
import com.shufang.source.MyUDFPeopleSource

object FlinkScalaExtension {

  import org.apache.flink.streaming.api.scala._


  //导入scala的拓展包，可以使用很多额外的技能
  import org.apache.flink.streaming.api.scala.extensions._

  private val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

  env.addSource(new MyUDFPeopleSource)
    .mapWith {
      a: WorkPeople => a.name + "mapWith"
    } //这个只有导入了拓展包才能使用
    .map(_ + "map").print()


  env.execute("scala extension")

}
