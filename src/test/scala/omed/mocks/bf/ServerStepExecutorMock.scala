package omed.mocks.bf

import sun.reflect.generics.reflectiveObjects.NotImplementedException

import omed.bf.{ ServerStepExecutor, ProcessTask }
import omed.model.Value
import omed.bf.tasks.SetValue
import omed.bf.handlers.SetValueHandler
import omed.model.services.ExpressionEvaluator
import omed.lang.eval.Configuration
import omed.mocks.ContextProviderMock

class ServerStepExecutorMock extends ServerStepExecutor {

  // don't evaluate expressions with references and queries
  val contextProvider = new ContextProviderMock
  val evaluator = new ExpressionEvaluator
  val handler = new SetValueHandler

  evaluator.contextProvider = contextProvider
  /**
   * @param step Process step description with context
   * @return Updated process step description with altered context and result message
   */
  def execute(step: ProcessTask, context: Map[String, Value],processId:String): Map[String, Value] = {
    if (step.stepType == "_Meta_BFSSetValue") {
      val targetTask = step.asInstanceOf[SetValue]

      val destinationVar = targetTask.destination.replaceFirst("\\@", "")
      val fieldValue = evaluator.evaluate(
        targetTask.sourceExp, Configuration.standard, context)

      Map(destinationVar -> fieldValue)
    } else throw new NotImplementedException
  }

  def canHandle(step: ProcessTask): Boolean = true

}