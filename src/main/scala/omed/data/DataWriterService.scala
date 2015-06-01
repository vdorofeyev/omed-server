package omed.data

import ru.atmed.omed.beans.model.meta._
import omed.model.{EntityInstance, MetaObject}

trait DataWriterService {

  /**
   * Создание новой записи. Возможно заполнение начальными значениями полей объекта
   */
  def addRecord(classId: String,relationField:Map[String,String]=Map()): EntityInstance

  /**
   * Создание новой записи для связанного грида
   */
  def addRelRecord(classId: String, viewCardId: String, cardRecordId: String, windowGridId: String):EntityInstance
    //(Boolean, String, Seq[CompiledValidationRule])

  def addRelation(relationId:String,fromObjectId:String):String

  def editPosition(nodeId:String,treeParams:String,objectId:String,windowGridId:String,newValues:Map[String,String])

  def editRelation(relationId:String,objectId:String,fromObjectId:String,toObjectId:String)

  def deleteRelation(relationId:String,objectId:String)

  def directSaveField(classId: String, recordId: String,
    fieldCode: String, fieldValue: Any)

  /**
   * Редактирование объекта
   *
   * @return (Код_возврата, Сообщение)
   * @throws DataDisptcherError Ошибка работы диспетчера данных или ошибка при обращении к базе данных
   */
  def editRecord(recordId: EntityInstance, fields: Map[String, String]):
    (Boolean, Seq[CompiledValidationRule])
  def editRecord(recordId: String, fields: Map[String, String]):
  (Boolean, Seq[CompiledValidationRule])

  /**
   * Удаление объекта
   *
   * @throws DataDisptcherError Ошибка работы диспетчера данных или ошибка при обращении к базе данных
   */
  def deleteRecord(recordId: String)

  def lockObject(objectId: EntityInstance): MetaObject

  def unlockObject(objectId: EntityInstance)
}