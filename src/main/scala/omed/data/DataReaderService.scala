package omed.data

import ru.atmed.omed.beans.model.meta._
import omed.lang.struct.Expression
import omed.model.Value

/**
 * Интерфейс для получения данных для форм.
 * Реализация: [[omed.data.DataReaderServiceImpl]]
 */
trait DataReaderService {

  def getDomains(): Seq[Int]
  /**
   * Данные справочника
   */
  def getDictionaryData(viewFieldId: String, variablesXml: String, objectId: String = null): DataDictionary

  /**
   * Дерево фильтров
   *
   * @param rootNodeId - идентификатор ноды с которого начинается дерево
   */
  def getTreeFilter(rootNodeId: String): Seq[FilterNode]

  /**
   * Данные сущности для формы-списка
   */
  def getGridData(
    gridId: String,
    nodeId: String,
    refId: String,
    nodeData: String,
    recordId: String,
    viewCardId: String,
    fieldId: String,
    variablesXml: String,
    treeVariablesXml: String,
    filters:Seq[Expression]=Seq(),
    context:Map[String,Value]=Map(),
    isFull:Boolean = false): DataTable

  def getGridDataView(
    gridId: String,
    nodeId: String,
    refId: String,
    nodeData: String,
    recordId: String,
    viewCardId: String,
    fieldId: String,
    variablesXml: String,
    treeVariablesXml: String,
    isDiagram : Boolean = false): DataViewTable

  /**
   * получить данные ИСФ
   * @param recordId
   * @param propertyCode
   * @return
   */
  def getISFDataView(
    recordId:String,
    propertyCode:String): DataViewTable

  /**
   * Данные сущности для формы-карточки
   */
  def getCardData(recordId: String,isFull:Boolean = false): DataTable

  def getCardDataView(recordId: String,viewCardId:String = null): (DataViewTable, Seq[StatusWindowGrid],Seq[StatusSection])

  def getObjectData(classCode: String = null, objectId: String): Map[String, Any]

  def getDisplayName(objectId:String) :Option[String]

  def getBFID(bfCode:String):String

  def getObjectClass(objectId:String):String

  def getCollectionByArrayName(arrayName:String,fieldValue:String,filters:Seq[Expression]=Seq(),context:Map[String,Value]=Map()):DataTable

  def getCollectionData(classCode: String, fieldCode: String, fieldValue: String,filters:Seq[Expression]=Seq(),context:Map[String,Value]=Map()): DataTable

   def getClassData(classId:String,filters:Seq[Expression],context:Map[String,Value]):DataTable
   def getClassDataFromAllDomain(classCode:String):Seq[Map[String,Any]]
  //def getClientSettings:Map[String,]
}