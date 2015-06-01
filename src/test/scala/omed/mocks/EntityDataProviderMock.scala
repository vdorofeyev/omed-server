package omed.mocks

import omed.model.{Value, EntityDataProvider}
import omed.lang.struct.Expression

/**
 * Created with IntelliJ IDEA.
 * User: naryshkinaa
 * Date: 25.04.14
 * Time: 16:17
 * To change this template use File | Settings | File Templates.
 */
class EntityDataProviderMock extends EntityDataProvider{
  def getDataForInstance(entityCode: String, entityId: String) = null

  def getArrayForInstance(arrayName: String, entityId: String, filters: Seq[Expression], context: Map[String, Value]) = null

  //  def getEntityInstance(entityCode: String, entityId: String) :EntityInstance
  def dropCache(entityId: String) = ()

  def getClassData(classId: String, filters: Seq[Expression], context: Map[String, Value]) = null
}
