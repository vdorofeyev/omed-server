package omed.bf.handlers

import com.google.inject.Inject

import omed.bf._
import omed.model.{SimpleValue, Value}
import omed.model.services.ExpressionEvaluator
import omed.data.{DataAwareConfiguration, DataWriterService}
import omed.bf.tasks.{SetValue, SetAttributeValue}
import omed.lang.eval.Configuration

class SetValueHandler extends ProcessStepHandler {

  @Inject
  var expressionEvaluator: ExpressionEvaluator = null
  @Inject
  var configProvider: ConfigurationProvider = null
  @Inject
  var businessFunctionLogger:BusinessFunctionLogger = null
  override val name = "_Meta_BFSSetValue"


//  def canHandle(step: ProcessTask) = {
//    step.stepType == name
//  }

  def handle(task: ProcessTask, context: Map[String, Value],processId:String): Map[String, Value] = {
    val targetTask = task.asInstanceOf[SetValue]

    val destinationVar = targetTask.destination.replaceFirst("\\@", "")
    val fieldValue = expressionEvaluator.evaluate(targetTask.sourceExp, configProvider.create(), context)
    businessFunctionLogger.addLogStep(processId,new BusinessFunctionStepLog("Шаг Присвоить значение",context,Map("value"->fieldValue,"var"->SimpleValue(destinationVar))))
    Map(destinationVar -> fieldValue)
  }

}