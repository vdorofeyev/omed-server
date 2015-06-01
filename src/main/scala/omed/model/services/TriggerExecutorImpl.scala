/**
 *
 */
package omed.model.services

import omed.triggers._
import omed.model.{SimpleValue, EntityInstance, MetaClassProvider}
import com.google.inject.Inject
import omed.system.ContextProvider
import omed.bf.{ProcessStateProvider, BusinessFunctionExecutor}
import omed.data.DataReaderService
import java.util.logging.Logger

class TriggerExecutorImpl extends TriggerExecutor {
  val logger = Logger.getLogger(this.getClass.getName())
  @Inject
  var triggerService: TriggerService = null
  @Inject
  var metaClassProvider: MetaClassProvider = null
  @Inject
  var businessFunctionExecutor: BusinessFunctionExecutor = null
  @Inject
  var contextProvider: ContextProvider = null
  @Inject
  var dataReader: DataReaderService = null
  @Inject
  var processStateProvider: ProcessStateProvider = null
  @Inject
  var systemTriggerProvider: SystemTriggerProvider = null

  def fireTriggers(inst: EntityInstance, updatedFields: Seq[String],
    event: TriggerEvent.Value, period: TriggerPeriod.Value) = {

    val triggers = triggerService.getTriggersByClass(inst.obj.id)
    //отобрать тригеры совпадающие по событию для Insert and Delete а также добавить триггеры на Update у которых существует в WatchList обновляемое поле или WatchList пуст, но существует обновлямое поле вне списка исключений
    val actualTriggers = triggers.filter(t =>
      (t.period == period) &&
      (t.events.contains(event) && event!=TriggerEvent.OnUpdate ||
        t.events.contains(TriggerEvent.OnUpdate) && (t.watchList.isEmpty && !updatedFields.forall(t.excludedList.contains) || updatedFields.exists(t.watchList.contains)) )
        )

    logger.info(String.format("Fire %d of %d triggers for class %s (%s)",
      actualTriggers.length.asInstanceOf[AnyRef],
      triggers.length.asInstanceOf[AnyRef],
      inst.obj.code,
      inst.obj.id))

    val fire = fireTrigger(_: Trigger, inst,updatedFields)

    actualTriggers foreach fire
  }

  private def fireTrigger(t: Trigger, inst: EntityInstance,updatedFields:Seq[String]) {
    // помещаем в контекст переданный объект
    t.triggerType match {
      case TriggerType.BF => {
        val triggerField = if(updatedFields.length == 1) Map("__triggerFieldCode"->SimpleValue(updatedFields.last)) else Map()
        val params = Map("this" -> inst)  ++ triggerField
        // запускаем выполнение бизнес-функции
        processStateProvider.dropProcess(businessFunctionExecutor.initFunctionInstance(t.functionID, params))
      }
        //обновление _Name объекта
      case TriggerType.Name => {
         systemTriggerProvider.updateName(inst)
      }
    }

  }

}