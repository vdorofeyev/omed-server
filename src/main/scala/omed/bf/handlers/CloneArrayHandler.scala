package omed.bf.handlers

import omed.bf._
import omed.model._
import omed.bf.tasks.CloneArray
import com.google.inject.Inject
import omed.model.services.ExpressionEvaluator
import omed.data.{EntityFactory, DataWriterService, DataReaderService}
import omed.bf.BusinessFunctionStepLog

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 17.09.13
 * Time: 15:41
 * To change this template use File | Settings | File Templates.
 */
class CloneArrayHandler extends ProcessStepHandler{
  @Inject
  var expressionEvaluator:ExpressionEvaluator = null
  @Inject
  var configProvider: ConfigurationProvider = null
  @Inject
  var dataReader: DataReaderService = null
  @Inject
  var dataWriter: DataWriterService = null
  @Inject
  var metaClassProvider:MetaClassProvider = null
  @Inject
  var cloneObjectProvider:CloneObjectProvider = null
  @Inject
  var businessFunctionLogger:BusinessFunctionLogger = null
  @Inject
  var entityFactory:EntityFactory = null
  override val name = "_Meta_BFSCloneArray"
  def handle(step: ProcessTask, context: Map[String, Value],processId:String): Map[String, Value] = {
    val targetTask = step.asInstanceOf[CloneArray]
    if(targetTask.source==null) throw new  RuntimeException("При копировании коллекции не задан объект для копирования")
    if(targetTask.destination==null) throw new  RuntimeException("При копировании коллекции не задан объект назначения")
    def evaluateInstance(expresion:String):EntityInstance={
       expressionEvaluator.evaluate(expresion, configProvider.create(), context) match{
         case e:EntityInstance => e
         case s:SimpleValue => entityFactory.createEntity(s.toString)
         case _ => throw new  RuntimeException("При копировании коллекции выражение не задает объект")
       }
    }
    val destination =entityFactory.createEntityWithValue(expressionEvaluator.evaluate(targetTask.destination, configProvider.create(), context))
    val source =entityFactory.createEntityWithValue(expressionEvaluator.evaluate(targetTask.source, configProvider.create(), context))

    val (classCode,fieldCode) = metaClassProvider.getClassAndProperty(targetTask.arrayName)
    businessFunctionLogger.addLogStep(processId,new BusinessFunctionStepLog("Шаг копирования коллекции до" ,context,Map("source"->source,"destination"->destination,"classCode"->SimpleValue(classCode),"arrayName"->SimpleValue(targetTask.arrayName))))
    val data = source.getProperty(targetTask.arrayName).asInstanceOf[EntityArray]//dataReader.getCollectionData(targetTask.arrayName,source.getId)
    data.data.foreach(f=> {
      val newRecord= dataWriter.addRecord(f.asInstanceOf[EntityInstance].getClassId)
      cloneObjectProvider.cloneObject(newRecord,f.asInstanceOf[EntityInstance].data++Map(fieldCode->destination.getId ) )
    })
    Map()
  }
}
