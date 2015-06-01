package omed.data

import com.google.inject.Inject
import omed.model._
import omed.model.services.ExpressionEvaluator

/**
 * Created by andrejnaryskin on 18.02.14.
 */
class EntityFactoryImpl extends EntityFactory{
  @Inject
  var dataReader: DataReaderService = null
  @Inject
  var entityDataProvider:EntityDataProvider = null
  @Inject
  var model:MetaModel = null
  @Inject
  var expressionEvaluator: ExpressionEvaluator = null
  def createEntity(id:String):EntityInstance={
    if(id == null) return null
    val classCode =dataReader.getObjectClass(id)
    if(classCode!=null) createEntityWithCode(id,classCode)
    else null
  }
  def createEntityWithValue(value:Value):EntityInstance={
    value match {
      case e:EntityInstance => e
      case s:SimpleValue =>createEntity(s.toString)
      case _ => throw new  RuntimeException("Не возможно создать объект из типа: "+ value.dataType.toString)
    }
  }
  def createEntityWithCode(id:String,code:String):EntityInstance ={
     if(id == null) return null
     new EntityInstance(model,model(code),id,entityDataProvider)
  }
  def createEntityWithClassId(id:String,classId:String):EntityInstance={
    if(id == null) return null
    new EntityInstance(model,model.getObjectById(classId),id,entityDataProvider)
  }
  def createEntityWithData(data:Map[String,Any]):EntityInstance ={
    createEntityWithDataAndObject(model.getObjectById(data("_ClassID").asInstanceOf[String]),data)
  }
  def createEntityWithDataAndObject(obj:MetaObject, data:Map[String,Any]):EntityInstance={
    //если в данных представлены не все значения то значит что надо загрузить объект заново
    val newData =  if (obj.fields.map(_.code).forall(data.contains))  data else null
    new EntityInstance(model,obj,data("ID").asInstanceOf[String],entityDataProvider,newData)
  }


}
