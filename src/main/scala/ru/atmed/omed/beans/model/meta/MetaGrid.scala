package ru.atmed.omed.beans.model.meta

import scala.xml.{TopScope, Text, NodeBuffer, Elem}
import net.iharder.Base64
import omed.rest.model2xml.Model2Xml

/**
 * Метаописание формы-списка.
 *
 * @param classId Идентификатор класса
 * @param viewGridId Идентификатор формы карточки
 * @param caption Заголовок
 * @param glyph Пиктограмма
 * @param isDeleteAllowed Признак "Удаление разрешено"
 * @param isInsertAllowed Признак "Добавление разрешено"
 * @param isEditAllowed Признак "Редактирование разрешено"
 * @param isInformPanelVisible Признак "Видмость информационнй панели"
 * @param isTreeVisible Признак "Видимость дерева фильтров"
 * @param isVisibleAuxiliaryGrid Признак "Видимость вспомогательного грида"
  * @param treeId Идентификатор запроса дерева фильтров
* @param isGoOnCardAfterInsert Признак
* @param contextMenu Контекстное меню
* @param menu Меню формы-списка
* @param fields Поля формы-списка (колонки)
* @param subClassList Производные классы
* @param windowGridId Идентификатор формы-списка
* @param diagramType Тип диаграммы
* @param abscissaCode Код поля для оси абсцисс (X). Используется для построения графиков
* @param arrayName Имя таблицы, при использовании в качестве источника данных в отчёте.
*/
case class MetaFormGrid (
                          var viewGridId: String = null,
                          isTreeVisible: Boolean = false,
                          isVisibleAuxiliaryGrid: Boolean = false,
                          treeId: String = null,
                          isSearchView:Boolean = false ,
                          diagramMeta:DiagramMeta = DiagramMeta(),
                          menu: Seq[ContextMenu] = null,
                          caption:String,
  var mainGrid :MetaGrid = null) extends Metaform {

  def getClassId = this.mainGrid.classId
  def getCaption = this.caption
  def xmlString:String={
    new StringBuilder().append(Model2Xml.tag("gridForm",
      new StringBuilder().append(Model2Xml.tag("viewGridId", viewGridId))
            .append(Model2Xml.tag("caption",caption))
            .append(Model2Xml.tag("isTreeVisible", isTreeVisible.toString))
            .append(Model2Xml.tag("treeId", treeId))
            .append(Model2Xml.tag("isSearchView", isSearchView.toString))
            .append(Model2Xml.tag("menu",if(menu!=null)menu.map(f => f.xmlString).mkString("\n") else null))
            .append(mainGrid.xmlString)
            .append(if(diagramMeta.captionPropertyCode!=null) diagramMeta.xmlString else "")
    )).toString
  }
}

case class MetaCardGrid(
  caption:String = null,
  arrayName: String = null,
  glyph: Array[Byte] = null,
  viewCardGridId:String = null,
  windowGridId:String = null,
  contextMenuId:String = null,
  sortOrder:Option[Int] = None
)
{
  def xmlString:String={
    new StringBuilder()
          .append(Model2Xml.tag("caption",caption))
          .append(Model2Xml.tag("glyph", {
            if (glyph != null)
              Base64.encodeBytes(glyph)
            else
              null
          }))
          .append(Model2Xml.tag("arrayName", arrayName))
          .append(Model2Xml.tag("sortOrder",sortOrder.map(f=> f.toString).getOrElse(null)))
          .append(Model2Xml.tag("viewCardGridId", viewCardGridId)).toString
  }

}

case class MetaGrid(
  classId: String = null,
  windowGridId: String = null,
  isDeleteAllowed: Boolean = false,
  isInsertAllowed: Boolean = false,
  isEditAllowed: Boolean = false,
  isGoOnCardAfterInsert: Boolean = false,
  diagramType: String = null,
  abscissaCode: String = null,
  isSearchVisible: Boolean = false,
  var relationPropertyCode :String = null,
  var metaCardGrid:MetaCardGrid = null,
  var contextMenu: Seq[ContextMenu] = null,
  fields: Seq[MetaGridColumn]= null,
  subClassList: Seq[Subclass] = null,
  var schedulerGroups:Seq[SchedulerGroup] = null,
  isSchedulerView:Boolean = false
)
{
  def xmlString:String={
    new StringBuilder().append(Model2Xml.tag("classId", classId))
          .append(if(metaCardGrid!=null)metaCardGrid.xmlString else "")
          .append(Model2Xml.tag("windowGridId", windowGridId))
          .append(Model2Xml.tag("relationPropertyCode",relationPropertyCode))
          .append(Model2Xml.tag("isDeleteAllowed", isDeleteAllowed.toString))
          .append(Model2Xml.tag("isInsertAllowed", isInsertAllowed.toString))
          .append(Model2Xml.tag("isEditAllowed", isEditAllowed.toString))
          .append(Model2Xml.tag("isSearchVisible", isSearchVisible.toString))
          .append(Model2Xml.tag("isSchedulerView", isSchedulerView.toString))
          .append(Model2Xml.tag("isGoOnCardAfterInsert", isGoOnCardAfterInsert.toString))
          .append(Model2Xml.tag("diagramType",diagramType))
          .append(Model2Xml.tag("abscissaCode",abscissaCode))
          .append(Model2Xml.tag("contextMenu",if(contextMenu!=null)contextMenu.map(f => f.xmlString).mkString("\n") else null))
          .append(Model2Xml.tag("fields", if(fields!=null)fields.map(f => f.xmlString).mkString("\n") else null))
          .append(Model2Xml.tag("groups", if(schedulerGroups.length == 0) null else schedulerGroups.map(f => f.xmlString).mkString("\n")))
          .append(Model2Xml.tag("subClasses",  if(subClassList!=null)subClassList.map(f => f.xmlString).mkString("\n") else null)).toString

  }
}
/**
 * Список метаописаний форм-списков
 *
 * @param data Список метаописаний форм-списков
 */
case class MetaGridSeq(data: Seq[MetaGrid])

/**
 * Производный класс.
 *
 * @param subClassId Идентификатор производного класса
 * @param name Наименование производного класса
 * @param parentId Идентификатор родительского класса
 */
case class Subclass(
  subClassId: String,
  name: String,
  parentId: String,
  rootClassId:String)
{

  def xmlString:String={
    new StringBuilder().append(Model2Xml.tag("subClass", new StringBuilder()
                 .append(Model2Xml.tag("id", subClassId))
                 .append(Model2Xml.tag("name", name))
                 .append(Model2Xml.tag("parentId", parentId)))).toString
  }
}

/**
* Производные классы
*
* @param data Список производных классов
*/
//case class SubclassSeq(data: Seq[Subclass])
