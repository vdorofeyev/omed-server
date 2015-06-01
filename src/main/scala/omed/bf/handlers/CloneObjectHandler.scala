package omed.bf.handlers

import omed.bf._
import omed.model.{MetaClassProvider, SimpleValue, EntityInstance, Value}
import omed.bf.tasks.CloneObject
import com.google.inject.Inject
import omed.model.services.ExpressionEvaluator
import omed.data.{EntityFactory, DataWriterService, DataReaderService}

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 16.09.13
 * Time: 12:03
 * To change this template use File | Settings | File Templates.
 */
class CloneObjectHandler extends  ProcessStepHandler{
  @Inject
  var expressionEvaluator:ExpressionEvaluator = null
  @Inject
  var configProvider: ConfigurationProvider = null
  @Inject
  var dataWriter: DataWriterService = null
  @Inject
  var cloneObjectProvider:CloneObjectProvider = null
  @Inject
  var metaClassProvider : MetaClassProvider = null
  @Inject
  var businessFunctionLogger:BusinessFunctionLogger = null
  @Inject
  var entityFactory:EntityFactory = null
  override val name = "_Meta_BFSCloneObject"

  def handle(step: ProcessTask, context: Map[String, Value],processId:String): Map[String, Value]={
     val targetTask = step.asInstanceOf[CloneObject]
    if(targetTask.source==null) throw new RuntimeException("При копировании объекта не задан копируемый объект")
    val source = entityFactory.createEntityWithValue(expressionEvaluator.evaluate(targetTask.source, configProvider.create(), context))
    val destination = if (targetTask.destination!=null) entityFactory.createEntityWithValue(expressionEvaluator.evaluate(targetTask.destination, configProvider.create(), context))
    else dataWriter.addRecord(metaClassProvider.getClassByRecord(source.getId).id)

//    val entitySource =  dataReader.getObjectData(objectId = source)
//    val classId = entitySource("_ClassID").asInstanceOf[String]
//    //аттрибуты которые не должны быть скопированы
//    val noCopyFields = Set("ID","_ClassID","_ChangeDate","_CreateDate","_ChangeUserID","_CreateUserID","_StatusID","_Name","LockTime","_Deleted","_Domain")
//    entitySource.filter(p=> !(noCopyFields contains p._1)).foreach(f=>dataWriter.directSaveField(classId,destination,f._1,f._2))
    businessFunctionLogger.addLogStep(processId,new BusinessFunctionStepLog("Шаг копирования объекта до" ,context,Map("source"->source,"destination"->destination)))
    cloneObjectProvider.cloneObject(destination,source.data)
    if(targetTask.resultVar!=null) Map(targetTask.resultVar->destination)
    else Map()
  }
}
