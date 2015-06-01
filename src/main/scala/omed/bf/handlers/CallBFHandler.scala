package omed.bf.handlers

import omed.bf._
import omed.model.{EntityArray, EntityInstance, SimpleValue, Value}
import omed.bf.tasks.CallBF
import com.google.inject.Inject
import omed.lang.eval.ExpressionEvaluator
import omed.model.services.ExpressionEvaluator
import omed.errors.MetaModelError

/**
 * Created by andrejnaryskin on 17.03.14.
 */
class CallBFHandler  extends  ProcessStepHandler {
  @Inject
  var expressionEvaluator:ExpressionEvaluator = null

  @Inject
  var businessFunctionExecutor: BusinessFunctionExecutor = null

  @Inject
  var processStateProvider: ProcessStateProvider = null

  @Inject
  var businessFunctionLogger:BusinessFunctionLogger = null

  override val name = "_Meta_BFSCallBF"

  def handle(step: ProcessTask, context: Map[String, Value],processId:String): Map[String, Value]={
    val targetTask = step.asInstanceOf[CallBF]
    if(targetTask.bf ==null) throw  new MetaModelError("Не задана вызываемая БФ")
    val source =  if(targetTask.expression ==null || targetTask.expression =="") null else  expressionEvaluator.evaluate(targetTask.expression, variables = context)
    def call(obj:Value):Map[String,Value]={
      val objMap = if(obj==null) Map() else Map("this"->obj)
      val bfInstanceId = businessFunctionExecutor.initFunctionInstance(targetTask.bf,targetTask.inParams.map(f => f._2 -> context(f._1)).toMap ++ objMap )
      val localContext = businessFunctionExecutor.getContext(bfInstanceId)
      processStateProvider.dropProcess(bfInstanceId)
      targetTask.outParams.map(f => f._1 -> localContext(f._2)).toMap
    }

    source match {
      case a:EntityArray => {
        var result :Map[String,Value] = Map()
        businessFunctionLogger.addLogStep(processId,new BusinessFunctionStepLog("Шаг вызова вложенной БФ" ,context,Map("source"->SimpleValue(targetTask.expression),"BF"->SimpleValue(targetTask.bf),"times"->SimpleValue(a.data.length))))
        a.data.foreach( e => result = call(e))
        result
      }
      case e:EntityInstance => {
        businessFunctionLogger.addLogStep(processId,new BusinessFunctionStepLog("Шаг вызова вложенной БФ" ,context,Map("source"->SimpleValue(targetTask.expression),"BF"->SimpleValue(targetTask.bf),"times"->SimpleValue(1))))
        call(e)
      }
      case _ =>  Map()
    }
  }
}
