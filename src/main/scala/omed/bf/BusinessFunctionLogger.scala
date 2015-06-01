package omed.bf

import omed.system.Context
import scala.collection.mutable.ListBuffer

/**
 * Лог выполнения БФ
 */
trait BusinessFunctionLogger {
      def getBFInstanceLog(bfInstanceId:String):Seq[BusinessFunctionStepLog]
      def addLogStep(bfInstanceId:String,step:BusinessFunctionStepLog, context:Context = null)
      def getAllLogs:Seq[BusinessFunctionLog]
      def dropLog

}
