package omed.bf

import omed.model.Value
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 30.08.13
 * Time: 15:31
 * To change this template use File | Settings | File Templates.
 */
case class BusinessFunctionStepLog (descr:String,val context: Map[String, Value] = Map(),val params:Map[String,Value] = Map() ){

}

case class BusinessFunctionLog (info: String,instanceId:String,steps:ListBuffer[BusinessFunctionStepLog]=new ListBuffer() ){

  def addStep(step:BusinessFunctionStepLog){
    steps += step
  }
}
