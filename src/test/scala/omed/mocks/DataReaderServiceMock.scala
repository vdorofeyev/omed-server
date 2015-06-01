package omed.mocks

import sun.reflect.generics.reflectiveObjects.NotImplementedException

import ru.atmed.omed.beans.model.meta._
import scala.collection.mutable.HashMap
import omed.data.{Timeslot, DataViewTable, DataTable, DataDictionary}
import omed.lang.struct.Expression
import omed.model.Value

class DataReaderServiceMock extends omed.data.DataReaderService {

  val cardData = new HashMap[String, DataTable]
  val objectClass =   new HashMap[String,String]

  def getDomains() = Seq(0)

  /**
   * Данные справочника
   */
  def getDictionaryData(viewFieldId: String, variablesXml: String, objectId: String = null): DataDictionary =
    throw new NotImplementedException

  /**
   * Дерево фильтров
   *
   * @param rootNodeId - идентификатор ноды с которого начинается дерево
   */
  def getTreeFilter(rootNodeId: String): Seq[FilterNode] =
    throw new NotImplementedException

  def getClassData(classId:String,filters:Seq[Expression],context:Map[String,Value]):DataTable={
    throw new NotImplementedException
  }
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
    variablesXml: String, treeVariablesXml: String,
    filters :Seq[Expression],
    context:Map[String,Value],
    isFull:Boolean): DataTable =
    throw new NotImplementedException

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
                isDiagram:Boolean): DataViewTable =
    throw new NotImplementedException

  /**
   * Данные сущности для формы-карточки
   */
  def getCardData(recordId: String,isFull:Boolean = false): DataTable =
    if (this.cardData.contains(recordId))
      this.cardData(recordId)
    else
      throw new NotImplementedException

  /**
   * Данные сущности для формы-карточки
   */
  def getCardDataView(recordId: String,viewCardId:String = null): (DataViewTable, Seq[StatusWindowGrid],Seq[StatusSection]) =
    throw new NotImplementedException

  def getObjectClass(objectId:String):String=
    objectClass.get(objectId).orNull

  def getBFID(bfCode:String):String=
    throw new NotImplementedException

  def getDisplayName(objectId:String) :Option[String]=
    throw new NotImplementedException

  def getObjectData(classCode: String = null, objectId: String): Map[String, Any] =
    if (classCode == "Episode")
      Map("ID" -> objectId, "PatientID" -> "recordId-9012312312", "_StatusID" -> "5234")
    else if (classCode == "Patient" || classCode == "Patient2" || classCode == "Patient3")
      Map(
                "ID" -> "recordId-9012312312",
                "Name" -> "Пётр",
                "_StatusID" -> "5234")
    else
      throw new NotImplementedException

  def getISFDataView(recordId: String,  propertyCode: String): DataViewTable = null

  def getCollectionByArrayName(arrayName:String,fieldValue:String,filters:Seq[Expression]=Seq(),context:Map[String,Value]=Map()):DataTable =null

  def getCollectionData(classCode: String, fieldCode: String, fieldValue: String,filters:Seq[Expression]=Seq(),context:Map[String,Value]=Map()): DataTable = null

  def getClassDataFromAllDomain(classCode: String) = null
}
