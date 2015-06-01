package omed.data

import omed.model.{MetaObject, EntityInstance,Value}

/**
 * Created by andrejnaryskin on 18.02.14.
 */
trait EntityFactory {
   def createEntity(id:String):EntityInstance
   def createEntityWithValue(value:Value):EntityInstance
   def createEntityWithCode(id:String,code:String):EntityInstance
   def createEntityWithClassId(id:String,classId:String):EntityInstance
   def createEntityWithData(data:Map[String,Any]):EntityInstance
   def createEntityWithDataAndObject(obj:MetaObject, data:Map[String,Any]):EntityInstance

}
