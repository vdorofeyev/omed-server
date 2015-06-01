package ru.atmed.omed.beans.model.meta

import scala.xml.Elem
import net.iharder.Base64
import omed.rest.model2xml.Model2Xml

/**
 * Элемент контекстного меню
 *
 * @param id Идентификатор элемента меню
 * @param name Имя элемента меню видимое пользователю
 * @param parentId Родительский элемент меню
 * @param method Выполняемый метод с указанием класса (например, «Patient.GetSomeData»)
 * @param methodTypeCharCode Тип метода. Значения: U – выгрузка в Эксель, R – отчет, P – процедура
 * @param isPublicOnFormGrid Признак "Доступен на форме грида"
 * @param isPublicOnFormCard Признак "Доступен на форме карточки"
 * @param isConfirmation Признак «Требует подтверждения перед выполнением» 
 *        (если true, то после нажатия на пункт меню выводится сообщение из Message)
 * @param isRefresh Признак "Обновить форму после выполнения"
 * @param message Сообщение
 * @param shortcut Сочетание клавиш, вызывающее метод
 * @param glyph Изображение элемента меню
 * @param businessFuncId Идентификатор бизнес функции, которая вызывается при выборе элемента
 */
case class ContextMenu(
   id: String,
   name: String,
   parentId: String,
   method: String,
   methodTypeCharCode: String,
   isPublicOnFormGrid: Boolean,
   isPublicOnFormCard: Boolean,
   isConfirmation: Boolean,
   isRefresh: Boolean,
   message: String,
   shortcut: String,
   glyph: Array[Any],
   businessFuncId: String,
   alignment:String,
   buttonPosition :String,
   sectionId:String,
   row :Int,
   sortOrder: Int,
   buttonGroupId:String) {

  def xmlString :String={
    new StringBuilder().append(Model2Xml.tag("menuItem",new StringBuilder()
        .append(Model2Xml.tag("id", id))
          .append(Model2Xml.tag("name", name))
          .append(Model2Xml.tag("parentMenuId",parentId))
          .append(Model2Xml.tag("glyph", {
            if (glyph != null)
              Base64.encodeBytes(glyph.asInstanceOf[Array[Byte]])
            else
              null
          }))
          .append(Model2Xml.tag("methodTypeCharCode",methodTypeCharCode))
          .append(Model2Xml.tag("methodCode",method))
          .append(Model2Xml.tag("isConfirmation",isConfirmation.toString()))
          .append(Model2Xml.tag("isRefresh", isRefresh.toString()))
          .append(Model2Xml.tag("msg", message))
          .append(Model2Xml.tag("shortcut", shortcut))
          .append(Model2Xml.tag("businessFunctionId", businessFuncId))
          .append(Model2Xml.tag("alignment",alignment))
          .append(Model2Xml.tag("buttonPosition",buttonPosition))
          .append(Model2Xml.tag("sectionId", sectionId))
          .append(Model2Xml.tag("row",row.toString))
          .append(Model2Xml.tag("sortOrder", sortOrder.toString))
          .append(Model2Xml.tag("buttonGroupId",buttonGroupId)))).toString
  }
}

/**
 * Контекстное меню
 *
 * @param data Список элементов контекстного меню
 */
case class ContextMenuSeq(data: Seq[ContextMenu])