package ru.atmed.omed.beans.model.meta

/**
 * Базовый интерфейс для форм карточки и списка
 */
trait Metaform {
  /**
   * Получить идентификатор класса формы
   * 
   * @return Идентификатор класса
   */
  def getClassId: String
  /**
   * Получить заголово формы
   * 
   * @return Заголовок
   */
  def getCaption: String
}

/**
 * Метаописание формы-карточки
 *
 * @param classId Идентификатор класса
 * @param viewCardId
 * @param caption Заголовок
 * @param glyph Пиктограмма формы
 * @param width Ширина
 * @param height Высота
 * @param fieldsPanelHeight Высота панели с полями
 * @param isReadOnly Признак "только для чтения"
 * @param contextMenu Контекстное меню формы
 * @param fields Поля формы
 * @param groups Группы полей
 * @param refGrids Связанные с карточкой гриды
 */
case class MetaCard (
  classId: String,
  viewCardId: String,
  caption: String = "",
  glyph: Array[Byte] = null,
  width: Int = 800,
  height: Int = 600,
  fieldsPanelHeight: Int = 300,
  isReadOnly: Boolean = false,
  isVisibleAlias:Boolean = false,
  contextMenu: Seq[ContextMenu] = Seq[ContextMenu](),
  fields: Seq[MetaCardField] = Seq[MetaCardField](),
  groups: Seq[FieldGroup] = Seq[FieldGroup](),
  sections:Seq[FieldSection] = Seq[FieldSection](),
  refGrids: Seq[MetaGrid] = Seq[MetaGrid](),
  cardsInCard :Seq[CardInCardItem] = Seq[CardInCardItem]() ,
  gridsInCard:Seq[GridInCardItem] = Seq[GridInCardItem]()
) extends Metaform {
  /**
   * Получить идентификатор класса формы-карточки
   * 
   * @return Идентификатор класса
   */
  def getClassId = classId
  /**
   * Получить заголовок формы-карточки
   * 
   * @return Заголовок
   */
  def getCaption = caption
}
