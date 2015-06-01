package omed.bf.handlers

import omed.bf.{ProcessTask, ProcessStepHandler}
import omed.model.Value
import omed.bf.tasks.UpdateNameTask
import omed.model.services.{ExpressionEvaluator, SystemTriggerProvider}
import com.google.inject.Inject
import omed.data.EntityFactory

/**
 * Created with IntelliJ IDEA.
 * User: naryshkinaa
 * Date: 23.04.14
 * Time: 15:55
 * To change this template use File | Settings | File Templates.
 */
class UpdateNameHandler extends ProcessStepHandler {
  override val name = "_Meta_BFSUpdateName"
  @Inject
  var systemTriggerProvider:SystemTriggerProvider = null
  @Inject
  var expressionEvaluator:ExpressionEvaluator = null
  @Inject
  var entityFactory:EntityFactory = null
  def handle(task: ProcessTask, context: Map[String, Value],processId:String): Map[String, Value] = {
    val targetTask = task.asInstanceOf[UpdateNameTask]
    val entity = entityFactory.createEntityWithValue(expressionEvaluator.evaluate(targetTask.expression,variables = context))
    systemTriggerProvider.updateName(entity)
    Map()
  }
}
