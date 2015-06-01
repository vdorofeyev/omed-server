package omed.mocks.bf

import omed.bf.{BusinessFunctionLog, BusinessFunctionStepLog, BusinessFunctionLogger}
import omed.system.Context

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 04.09.13
 * Time: 12:37
 * To change this template use File | Settings | File Templates.
 */
class BusinessFunctionLoggerMock extends BusinessFunctionLogger {
  def getBFInstanceLog(bfInstanceId:String):Seq[BusinessFunctionStepLog]     = {
    Seq()
  }
  def addLogStep(bfInstanceId:String,step:BusinessFunctionStepLog, context:Context = null)
  {

  }

  def getAllLogs:Seq[BusinessFunctionLog] = {
     Seq()
  }
  def dropLog{

  }
}
