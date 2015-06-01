package omed.bf.handlers

import com.google.inject.Inject

import omed.bf._
import omed.model._
import omed.model.services.ExpressionEvaluator
import omed.data.{EntityFactory, DataReaderService, DataWriterService}
import omed.bf.tasks.SetAttributeValue
import omed.db.{DBProfiler}
import omed.errors.MetaModelError
import omed.model.ReferenceField
import omed.cache.ExecStatProvider
import omed.lang.eval.DBUtils


class SetAttrValueHandler extends ProcessStepHandler {

  @Inject
  var dataWriter: DataWriterService = null
  @Inject
  var dataReader: DataReaderService = null
  @Inject
  var expressionEvaluator: ExpressionEvaluator = null
  @Inject
  var configProvider: ConfigurationProvider = null
  @Inject
  var metaClassProvider: MetaClassProvider = null
  @Inject
  var execStatPrivider:ExecStatProvider = null
  @Inject
  var businessFunctionLogger:BusinessFunctionLogger = null
  @Inject
  var entityFactory:EntityFactory = null
  override val name = "_Meta_BFSSetAttributeValue"


//  def canHandle(step: ProcessTask) = {
//    step.stepType == name
//  }

  def handle(task: ProcessTask, context: Map[String, Value],processId:String): Map[String, Value] = {
    val targetTask = task.asInstanceOf[SetAttributeValue]

 //   val model =new LazyMetaModel(metaClassProvider)
    // if value of reference field doesn't present in entity data map,
    // retrieve entity from storage
    def refreshInstance(instance: EntityInstance, field: String): EntityInstance = {
      if (instance.data.contains(field)) instance
      else {
        val data = dataReader.getObjectData(instance.obj.code, instance.getId)
        entityFactory.createEntityWithData(data)
      }
    }
    val refreshedContext=  expressionEvaluator.convertGuidsToObject(context)
    val destParts = targetTask.destination.split("\\.")
    if (destParts.length < 2) throw new MetaModelError("Step destination has inappropriate format")
    val varName = if (destParts.length > 1) destParts.head.replaceFirst("\\@", "") else "this"
    if(!refreshedContext.contains(varName)) throw new MetaModelError("Не найдена переменная @"+ varName)
    val instance = refreshedContext(varName).asInstanceOf[EntityInstance]
    val path = destParts.tail.init
    val fieldCode = destParts.last
    // hop over dot-separated parts of path, determine value object for each field

    val targetInstance =  try {
      path.foldLeft(instance) {
        (instance: EntityInstance, field: String) =>
          instance.getProperty(field).asInstanceOf[EntityInstance]
//          val inst = refreshInstance(instance, field)
//          inst.obj(field) match {
//            case refField: ReferenceField => {
//              entityFactory.createEntity(inst.data(field).toString)
//            }
//          }
      }
    } catch {
      case _ =>
        businessFunctionLogger.addLogStep(processId,new BusinessFunctionStepLog("ERROR: Шаг Изменение аттрибута в БД" ,context,Map("Source"->SimpleValue(targetTask.source),"Destination"->SimpleValue(targetTask.destination))))
        throw new MetaModelError("Could not resolve step destination")
    }

    //вычисление значения выражения и обновления контекста переменных из String (Guid) в EntityInstance
    val fieldValue = try {
      DBProfiler.profile("calc expression",execStatPrivider,true) {expressionEvaluator.evaluate(targetTask.source, configProvider.create(), refreshedContext) }
    } catch {
    case e =>
      businessFunctionLogger.addLogStep(processId,new BusinessFunctionStepLog("ERROR: Шаг Изменение аттрибута в БД " + e.getMessage ,context,Map("Source"->SimpleValue(targetTask.source),"Destination"->SimpleValue(targetTask.destination))))
      throw e
  }
    val value = DBUtils.platformValueToDb(fieldValue)
    val strValue = if(value==null) null else value.toString


    businessFunctionLogger.addLogStep(processId,new BusinessFunctionStepLog("Шаг Изменение аттрибута в БД" ,context,Map("recordID"->SimpleValue(targetInstance.getId),"Value"->SimpleValue(strValue),"Source"->SimpleValue(targetTask.source),"Destination"->SimpleValue(targetTask.destination))))
    val (error,warning) =  dataWriter.editRecord(targetInstance,Map(fieldCode-> strValue))
    // обновляем данные в самой EntityInstance
    Map()
//    if (path.isEmpty) {
//      // Если происходило изменение поля переменной varName, необходимо обновить контекст:
//      // возвращаем подмененный объект с новым значением поля.
//      // fieldValue будет типа Value. Это допустимо, поскольку это значение поля переменной-сущности,
//      // т.е. фактически VariableField, а ExpressionEvaluator обрабатывает ситуацию, когда
//      // значение VariableField имеет тип Value.
//      targetInstance.drop
//      val newInstance = targetInstance//new EntityInstance(model, targetInstance.obj,ta targetInstance.data.updated(fieldCode, value))
//      Map(varName -> newInstance)
//    } else {
//      // Иначе контекст не обновляем
//      Map.empty[String, EntityInstance]
//    }
  }

}