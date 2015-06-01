package omed.bf.handlers

import omed.bf._
import omed.db.DataAccessSupport
import omed.model._
import com.google.inject.Inject
import omed.data.{EntityFactory, DataWriterService}
import omed.model.services.ExpressionEvaluator
import omed.bf.tasks.CreateByClassTemplate
import omed.forms.MetaFormProvider
import omed.lang.eval.ExpressionEvaluator
import omed.model.services.ExpressionEvaluator
import omed.bf.BusinessFunctionStepLog

/**
 * Created with IntelliJ IDEA.
 * User: naryshkinaa
 * Date: 04.04.14
 * Time: 14:17
 * To change this template use File | Settings | File Templates.
 */
class CreateByTemplateHandler  extends ProcessStepHandler with DataAccessSupport {
  @Inject
  var dataWriterService: DataWriterService = null
  @Inject
  var metaFormProvider:MetaFormProvider = null

  @Inject
  var metaClassProvider: MetaClassProvider = null
  @Inject
  var expressionEvaluator: ExpressionEvaluator = null
  @Inject
  var configProvider: ConfigurationProvider = null
  @Inject
  var entityFactory:EntityFactory = null
  @Inject
  var businessFunctionLogger:BusinessFunctionLogger = null
  @Inject
  var model:MetaModel = null
  override val name = "_Meta_BFSCreateByClassTemplate"
  def handle(task: ProcessTask, context: Map[String, Value],processId:String): Map[String, Value] = {
    val targetTask = task.asInstanceOf[CreateByClassTemplate]

    //объект одного из классов шаблонов
    val obj = entityFactory.createEntityWithValue(expressionEvaluator.evaluate(targetTask.templateObjectExpr,configProvider.create(),context))
    val templateClass = metaFormProvider.getTemplateClass(obj.getClassId,targetTask.templateClassTypeId)
    if(templateClass == null) throw new RuntimeException("Не найден шаблон для класса: " + obj.getClassId + " и типа: " +targetTask.templateClassTypeId)
    businessFunctionLogger.addLogStep(processId,new BusinessFunctionStepLog("Шаг создания по шаблону",context,Map("class"->SimpleValue(model.getObjectById(templateClass.newObjectClassId).code))))
    val newObj = dataWriterService.addRecord(templateClass.newObjectClassId)

    templateClass.templateProperties.foreach(f => dataWriterService.editRecord(newObj,Map(f.toPropertyCode->obj.data(f.fromPropertyCode).toString)))
    Map(targetTask.resultVariable -> newObj)
  }
}