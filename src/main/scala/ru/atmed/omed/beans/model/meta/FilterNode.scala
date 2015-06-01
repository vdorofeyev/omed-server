package ru.atmed.omed.beans.model.meta

/**
 * Элемент дерева фильтров для сущности.
 *
 * @param id Идентификатор узла
 * @param name Наименование узла
 * @param parentId Идентификатор родительской записи
 * @param data Дополнительная информация для фильтрации
 */
case class FilterNode(
  val id: String,
  val name: String,
  val parentId: String,
  val data: List[NodeParameter])

/**
 * Дерево фильтров.
 *
 * @param data Список элементов дерева фильтров
 */
case class FilterNodeSeq(data: Seq[FilterNode])

/**
 * Параметр узла для элемента дерева фильтра.
 *
 * @param id Идентификатор параметра
 * @param nodeId Идентификатор узла
 * @param caption Заголовок
 * @param varName Имя переменной
 * @param editorType Тип редактора
 * @param defaultValue Значения по-умолчанию
 * @param referenceParams Параметр для фильтрации
 * @param parameterSqlFilter Фильтр поля
 * @param viewFieldId ИД поля
 * @param typeTypeCode Тип значения
 * @param typeExtInfo Дополнительная информация о типе значения
 * @param isNoTerminalSelection возможность выбора не терминальных узлов
 */
case class NodeParameter(
  id: String,
  nodeId: String,
  caption: String,
  varName: String,
  editorType: String,
  defaultValue: String,
  referenceParams: String,
  parameterSqlFilter: String,
  viewFieldId: String,
  typeTypeCode: String,
  typeExtInfo: String,
  isNoTerminalSelection: Boolean,
  isMultiSelect:Boolean)