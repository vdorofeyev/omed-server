package ru.atmed.omed.beans.model.meta

import scala.xml.Elem
import omed.rest.model2xml.Model2Xml

/**
 * Метаописание колонок для грида.
 *
 * @param metafield
 * @param caption Видимое название поля
 * @param sortOrder Сортировка
 * @param isReadOnly Признак "Только чтение"
 * @param format Формат поля
 * @param isDropDownNotAllowed Признак "Выпадения списка"
 * @param isMasked Признак "Поле экранируется звёздочками"
 * @param isVisible Признак "Доступность клиенту"
 * @param isHidden Признак "Видимость поля"
 * @param aggregateFunction Тип агрегатора. Возможные значения: sum, avg, count, min, max
 * @param width Ширина
 * @param defaultFormGridId Идентификатор грида при нажатии F2
 * @param extInfo Дополнительная информация
 * @param refParams Параметры отобора из справочника
 * @param isShowOnChart Признак "Использовать при построения графика"
 * @param normaMin Минимальное значение нормы. Используется в графиках
 * @param normaMax Максимальное значение нормы. Используется в графиках
 */
case class MetaGridColumn(
   metafield: Metafield,
   caption: String,
   sortOrder: Int,
   isReadOnly: Boolean,
   format: String,
   isDropDownNotAllowed: Boolean,
   isMasked: Boolean,
   isVisible: Boolean,
   isHidden: Boolean,
   aggregateFunction: String,
   width: Int,
   defaultFormGridId: String,
   extInfo: String,
   refParams: String,
   mask: String,
   isJoinMask: Boolean,
   isShowOnChart: Boolean,
   normaMin: Double,
   normaMax: Double,
   isRefreshOnChange: Boolean,
   windowGridId:String) extends Metafield {
  def getViewFieldId = metafield.getViewFieldId
  def getCodeName = metafield.getCodeName
  def getEditorType = metafield.getEditorType
  def getTypeCode = metafield.getTypeCode
  def getTypeExtInfo = metafield.getTypeExtInfo
  def toXml:Elem={
    <field>
      <id>{metafield.getViewFieldId}</id>
      <codeName>{metafield.getCodeName}</codeName>
      <caption>{caption}</caption>
      <sortOrder>{sortOrder}</sortOrder>
      <isReadOnly>{isReadOnly.toString}</isReadOnly>
      <editorType>{metafield.getEditorType}</editorType>
      <format>{format}</format>
      <isDropDownNotAllowed>{isDropDownNotAllowed.toString}</isDropDownNotAllowed>
      <isMasked>{isMasked.toString}</isMasked>
      <isVisible>{isVisible.toString}</isVisible>
      <isHidden>{isHidden.toString}</isHidden>
      <defaultFormGridId>{defaultFormGridId}</defaultFormGridId>
      <aggregateFunction>{aggregateFunction}</aggregateFunction>
      <typeCode>{metafield.getTypeCode}</typeCode>
      <typeExtInfo>{metafield.getTypeExtInfo}</typeExtInfo>
      <width>{width}</width>
      <extInfo>{extInfo}</extInfo>
      <refParams>{refParams}</refParams>
      <mask>{mask}</mask>
      <normaMin>{normaMin}</normaMin>
      <normaMax>{normaMax}</normaMax>
      <isJoinMask>{isJoinMask.toString}</isJoinMask>
      <isShowOnChart>{isShowOnChart.toString}</isShowOnChart>
      <isRefreshOnChange>{isRefreshOnChange.toString}</isRefreshOnChange>
    </field>
  }
  def xmlString:String = {
    new StringBuilder().append(Model2Xml.tag("field", new StringBuilder()
                  .append(Model2Xml.tag("id", metafield.getViewFieldId))
                  .append(Model2Xml.tag("codeName", metafield.getCodeName))
                  .append(Model2Xml.tag("caption", caption))
                  .append(Model2Xml.tag("sortOrder", sortOrder.toString))
                  .append(Model2Xml.tag("isReadOnly",isReadOnly.toString))
                  .append(Model2Xml.tag("editorType", metafield.getEditorType))
                  .append(Model2Xml.tag("format", format))
                  .append(Model2Xml.tag("isDropDownNotAllowed", isDropDownNotAllowed.toString))
                  .append(Model2Xml.tag("isMasked", isMasked.toString))
                  .append(Model2Xml.tag("isVisible", isVisible.toString))
                  .append(Model2Xml.tag("defaultFormGridId",defaultFormGridId))
                  .append(Model2Xml.tag("isHidden", isHidden.toString))
                  .append(Model2Xml.tag("aggregateFunction", aggregateFunction))
                  .append(Model2Xml.tag("typeCode", metafield.getTypeCode))
                  .append(Model2Xml.tag("typeExtInfo", metafield.getTypeExtInfo))
                  .append(Model2Xml.tag("width", width.toString))
                  .append(Model2Xml.tag("extInfo",extInfo))
                  .append(Model2Xml.tag("refParams", refParams))
                  .append(Model2Xml.tag("mask", mask))
                  .append(Model2Xml.tag("isJoinMask", isJoinMask.toString))
                  .append(Model2Xml.tag("isShowOnChart",isShowOnChart.toString))
                  .append(Model2Xml.tag("normaMin", normaMin.toString))
                  .append(Model2Xml.tag("normaMax", normaMax.toString))
                  .append(Model2Xml.tag("isRefreshOnChange", isRefreshOnChange.toString)))).toString
  }

}

/**
 * Колонки формы-списка.
 * 
 * @param data Список колонок формы-списка
 */
case class MetaGridColumnSeq(data: Seq[MetaGridColumn])
