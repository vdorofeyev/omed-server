package ru.atmed.omed.beans.model.meta

/**
 * Элемент главное меню приложения
 * 
 * @param id Идентификатор элемента меню
 * @param businessFunctionId Индентификатор вызываемой БФ
 * @param parentId Идентификатор родительского элемента меню
 * @param menuLevel Уровень вложенности меню
 * @param name Наименование элемента на форма-карточке или форме-списке
 * @param openViewId Индентификатор формы, которая открывается по при выборе данного элемента меню
 * @param glyph Иконка элемента меню
 */
case class AppMenu(
  id: String,
  parentId: String,
  menuLevel: Int,
  name: String,
  openViewId: String,
  businessFunctionId: String,
  glyph: Array[Byte])

/**
 * Главное меню приложения
 * 
 * @param data Список элементов меню
 */
case class AppMenuSeq(data: Seq[AppMenu])