package omed.bf.handlers

import scala.xml.XML
import java.util.logging.Logger
import com.google.inject.Inject

import omed.bf._
import omed.model._
import omed.data.{EntityFactory, DataWriterService}
import omed.bf.tasks.StateTransition
import ru.atmed.omed.beans.model.meta.{ValidationResultType, ClassValidationRule, CompiledValidationRule, ValidationRule}
import omed.lang.eval.{ValidatorEvaluator, ValidatorContext}
import omed.lang.xml.ValidatorExpressionXmlReader
import omed.errors.{ValidationException, MetaModelError}
import ru.atmed.omed.beans.model.meta.ValidationRule
import omed.lang.eval.ValidatorContext
import omed.validation.ValidationProvider
import omed.lang.eval.ValidatorContext
import omed.db.DBProfiler
import omed.cache.ExecStatProvider
import omed.predNotification.PredNotificationProvider

class StateTransitionHandler extends ProcessStepHandler {
  val logger = Logger.getLogger(classOf[StateTransitionHandler].getName())
  @Inject
  var metaClassProvider: MetaClassProvider = null
  @Inject
  var dataWriterService: DataWriterService = null
  @Inject
  var configProvider: ConfigurationProvider = null
  @Inject
  var validationProvider:ValidationProvider = null
  @Inject
  var businessFunctionLogger:BusinessFunctionLogger = null
  @Inject
  var execStatProvider:ExecStatProvider = null
  @Inject
  var predNotificationProvider:PredNotificationProvider = null
  @Inject
  var entityFactory:EntityFactory = null
  @Inject
  var validationWarningPool:ValidationWarningPool = null
  override val name = "_Meta_BFSTransition"


  def handle(task: ProcessTask, context: Map[String, Value],processId:String): Map[String, Value] = {
    val targetTask = task.asInstanceOf[StateTransition]

    val instance = context("this").asInstanceOf[EntityInstance]
    val classId = instance.obj.id
    val recordId = instance.data("ID").asInstanceOf[String] // or id or may be Id

   val transition = metaClassProvider.getClassStatusTransitions(classId)
      .find(_.id == targetTask.transitionId).get
    val (errors,warnings) = validationProvider.getComlpexTransitionValidators(classId,transition,context)
    businessFunctionLogger.addLogStep(processId,new BusinessFunctionStepLog("Шаг Изменение Статуса",context,Map("recordId"->SimpleValue(recordId),"transition"->SimpleValue(transition.id))))
    logger.info(String.format(
      "Executing server BF step %s. Change state of %s(id='%s') from '%s' to '%s'",
      name, instance.obj.code, recordId, transition.beginStatusID, transition.endStatusID))

    if (!errors.isEmpty)
      throw new ValidationException(false, validationWarningPool.getWarnings.toSeq)

    dataWriterService.directSaveField(classId, recordId, "_StatusID", transition.endStatusID)
    predNotificationProvider.updatePredNotificationsForObject(instance)
    instance.drop
    predNotificationProvider.createPredNotificationsForObject(instance)
    Map()
  }
}
