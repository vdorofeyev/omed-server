package omed.data

import scala.xml.Elem
import ru.atmed.omed.beans.model.meta.StatusMenuRedefinition
import omed.rest.model2xml.Model2Xml

/**
 * Описание таблицы со значениями ячеек
 */
class DataTable(val columns: Seq[String], val binaryItems: Seq[Int], val data: Seq[Array[Any]], val perm:Map[String,Boolean])

/**
 * Описание таблицы со значениями ячеек и дополнительными атрибутами
 * @param columns
 * @param binaryItems
 * @param data
 * @param defaultTabId =null вкладка по умолчанию при открытии ФК
 */
class DataViewTable(val columns: Seq[String], val binaryItems: Seq[Int], val data: Seq[DataViewRow],val defaultTabId:String =null,val relations:Seq[DataRelation] = null,val detailGrids:Seq[DataViewTable] = null,val windowGridId:String = null)

/**
 * Описание строки таблицы, значения полей и дополнительные переопределения
 * @param data Значения полей строки
 * @param fieldOverrides Описание переопределений
 * @param rowColor Цвет строки
 * @param cellColors Массив значений цвета для полей
 */
class DataViewRow(
  val data: Array[Any],
  val fieldOverrides: Map[String, Map[String, Any]],
  val rowColor: ColorRule,
  val cellColors: Array[String],
  val isDeleteNotAllowed: Option[Boolean] = None ,
  val position:ObjectPosition = null,
  val menuOverrides:Seq[StatusMenuRedefinition] = Seq()
)


case class DataRelation( relationId:String, objectId:String, caption:String, from:String, to:String, fromReadOnly :Option[Boolean], toReadOnly:Option[Boolean], isDeletedAllowed :Option[Boolean]){
  def toXml :Elem={
      <relation relationId={relationId}  objectId = {Option(objectId) orNull} caption={caption} from={from} to ={to} fromReadOnly = {fromReadOnly.map(f => f.toString) orNull} toReadOnly = {toReadOnly.map(f => f.toString) orNull} isDeletedAllowed = {isDeletedAllowed.map(f => f.toString) orNull} />
          //<relation relationId={relationId}  objectId = {Option(objectId) orNull} caption={caption} from={from} to ={to}  fromReadOnly = {fromReadOnly orNull} toReadOnly ={toReadOnly orNull} isDeletedAllowed = {isDeletedAllowed orNull}  />
  }
}