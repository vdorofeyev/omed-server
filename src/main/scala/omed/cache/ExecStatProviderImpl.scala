package omed.cache

import scala.collection.mutable.{ Map => MutableMap }
import org.joda.time.DateTime
import java.util.concurrent.ConcurrentHashMap
import java.util
import scala.xml.{TopScope, Elem}

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 09.09.13
 * Time: 12:55
 * To change this template use File | Settings | File Templates.
 */
class ExecStatProviderImpl extends ExecStatProvider {


   val execedBlocks = MutableMap[String,Stat ]()
   val execedProcedures =MutableMap[String,Stat ] ()
   var dbTotalTime:Long =0
   val blockDBTimeMap =  MutableMap[String,Long ]()

//  def startBlock(name:String) {
//     execedBlocks.put(name,new ExecedBlock())
//  }
//  def stopBlock(name:String){
//     val block = execedBlocks.get(name)
//     if (block.isDefined) {
//        block.get.end = System.currentTimeMillis()
//        execedBlocks.put(name,block.get)
//     }
//  }
  def addExecedSP(name:String,time:Long) {
     dbTotalTime+=time
     val oldValue = execedProcedures.get(name)
     if(oldValue.isDefined){
         execedProcedures.put(name,Stat(oldValue.get.calls+1,oldValue.get.totalTime+time))
     }
    else execedProcedures.put(name,Stat(1,time))
    blockDBTimeMap.foreach(f => blockDBTimeMap.update(f._1,f._2 + time))
  }

  def addExecedBlock(name:String, totalTime:Long,startTime:Long){
    val oldValue = execedBlocks.get(name)
    val time =if(blockDBTimeMap.contains(name+startTime.toString)){
      val tmp = totalTime  -  blockDBTimeMap(name+startTime.toString)
      blockDBTimeMap.remove(name+startTime.toString)
      tmp
    } else 0

    if(oldValue.isDefined){
      execedBlocks.put(name,Stat(oldValue.get.calls+1,oldValue.get.totalTime+time))
    }
    else execedBlocks.put(name,Stat(1,time))
  }
  def addPredExecedBlock(name:String){
     blockDBTimeMap.put(name,0)
  }
  def toXml:String={
    <execStat>
      <total>{execedBlocks.get("total").map(f=>f.totalTime + dbTotalTime).getOrElse("not defined")}</total>
      <db>
        <total>{dbTotalTime}</total>
        {statToXML(execedProcedures,false)}
      </db>
      <appServer>
        <total>{execedBlocks.get("total").map(f=>(f.totalTime)).getOrElse("not defined")}</total>
        {statToXML(execedBlocks.filter(p=>p._1!="total"),true)}
      </appServer>
    </execStat>.toString()
  }
  private def statToXML(stats:MutableMap[String,Stat ],isAppServer:Boolean):Elem={
      val internal =stats.map(f=> {
        val block =  <name>{f._1}</name>
          <count>{f._2.calls}</count>
          <totalTime>{f._2.totalTime}</totalTime>;

        Elem(null,if(isAppServer)"block" else "procedure",null,TopScope,block :_*)
      }
      );

      Elem(null,if(isAppServer)"appBlocks" else "procedures",null,TopScope,internal.toSeq :_*)


  }
}

case class Stat(val calls: Int,
                val totalTime: Long)
