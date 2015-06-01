package omed.model.services

import omed.model.{EntityInstance, EntityDataProvider,Value}
import com.google.inject.Inject
import omed.data.{EntityFactory, DataReaderService}
import omed.lang.struct.Expression
/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 14.02.14
 * Time: 13:55
 * To change this template use File | Settings | File Templates.
 */

class EntityDataProviderImpl extends EntityDataProvider {
  @Inject
  var dataReader: DataReaderService = null

  @Inject
  var entityFactory :EntityFactory = null

  val cache : scala.collection.mutable.Map[String,Map[String,Any]] =  scala.collection.mutable.Map[String,Map[String,Any]]()
  def getDataForInstance(entityCode: String, entityId: String) :Map[String,Any]={
    if(cache.contains(entityId))cache(entityId)
    else  dataReader.getObjectData(entityCode, entityId)
  }
  def getArrayForInstance(arrayName:String,entityId:String,filters:Seq[Expression],context:Map[String,Value]):Seq[EntityInstance]={
    val data = dataReader.getCollectionByArrayName(arrayName,entityId,filters,context)
    data.data.map(f => entityFactory.createEntityWithData(( data.columns zip f).toMap))
  }
  def getClassData(classId:String,filters:Seq[Expression],context:Map[String,Value]):Seq[EntityInstance]={
    val data =  dataReader.getClassData(classId,filters,context)
    data.data.map(f => entityFactory.createEntityWithData(( data.columns zip f).toMap))
  }
  def dropCache( entityId: String)={
    if(entityId==null) cache.clear()
    else cache.remove(entityId)
  }
}