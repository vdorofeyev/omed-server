package omed.bf

import scala.collection.mutable.{Map => MutableMap, ListBuffer}
import scala.collection.mutable
import com.google.inject.Inject
import omed.system.ContextProvider
import java.util.Calendar
import omed.data.DataReaderService
import omed.system.Context
/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 30.08.13
 * Time: 15:54
 * To change this template use File | Settings | File Templates.
 */
class BusinessFunctionLoggerImpl  extends  BusinessFunctionLogger{

  private val bfLogSeq = new ListBuffer[BusinessFunctionLog]()

 // @Inject
  //var dataReaderService:DataReaderService = null

  def getBFInstanceLog(bfInstanceId:String):Seq[BusinessFunctionStepLog]= {
       bfLogSeq.find(p=>p.instanceId==bfInstanceId).map(f=>f.steps.toSeq).getOrElse(Seq())
  }
  def addLogStep(bfInstanceId:String,step:BusinessFunctionStepLog,context:Context = null){
     val previouslog =  bfLogSeq.find(p=>p.instanceId==bfInstanceId)
     if(previouslog.isDefined){
       previouslog.get.addStep(step)
     }
     else {
        if(bfLogSeq.length>=50){
          bfLogSeq.remove(0)
        }

       val user =if(context==null) null
       else context.userId//dataReaderService.getObjectData("UserAccount",context.userId)
//       val str  =if(user==null)"[Пользователь не задан]"
//       else user.get("UserLogin").get
        bfLogSeq+=new BusinessFunctionLog("user: " + user+ " " + currentTime+" instanceID = " + bfInstanceId,bfInstanceId, ListBuffer(step))
     }
  }
  def currentTime:String={
    val today = Calendar.getInstance().getTime
    today.toString()
  }
  def getAllLogs:Seq[BusinessFunctionLog]={
    bfLogSeq.reverse.toSeq
  }

  def dropLog{
    bfLogSeq.clear()
  }
}
