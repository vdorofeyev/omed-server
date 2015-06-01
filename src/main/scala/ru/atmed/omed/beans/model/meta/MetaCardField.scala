package ru.atmed.omed.beans.model.meta

/**
 * Базовый интерфейс для полей формы-карточи и формы-гриды
 */
trait Metafield {
  /**
   * Получить идентификатор поля
   *
   * @return Идентификатор (Guid)
   */
  def getViewFieldId: String
  /**
   * Получить служебное имя поля
   *
   * @return Служебное имя поля
   */
  def getCodeName: String
  /**
   * Получить тип элемента управления поля
   *
   * @return Тип поля
   */
  def getEditorType: String
  /**
   * Получить тип значения поля
   */
  def getTypeCode: String
  /**
   * Получить дополнительную информацию о поле
   */
  def getTypeExtInfo: String
}

/**
 *
 * @param viewFieldId Идентификатор поля
 * @param codeName Служебное имя поля
 * @param editorType Тип элемента управления
 * @param typeCode Тип поля
 * @param typeExtInfo Дополнительная информация о типе поля
 */
case class MetafieldImpl(
    val viewFieldId: String,
    val codeName: String,
    val editorType: String,
    val typeCode: String,
    val typeExtInfo: String) extends Metafield {

  def getViewFieldId = this.viewFieldId
  def getCodeName = this.codeName
  def getEditorType = this.editorType
  def getTypeCode = this.typeCode
  def getTypeExtInfo = this.typeExtInfo
}

/**
 * Метаописание поля формы-карточки
 *
 * @param metafield
 * @param caption Видимое название поля
 * @param sortOrder Сортировка
 * @param isReadOnly Признак "Только чтение"
 * @param format Формат поля
 * @param isDropDownNotAllowed Признак "Выпадения списка"
 * @param isMasked Признак "Поле экранируется звёздочками"
 * @param isVisible Признак "Доступность клиенту"
 * @param height Высота поля
 * @param width Ширина полня
 * @param groupId Идентификатор группы
 * @param defaultFormGridId Идентификатор грида при нажатии "F2"
 * @param extInfo Дополнительная информация
 * @param isRequired Признак "Обязательности"
 * @param refParams Параметры отобора из справочника
 * @param mask Маска ввода
 * @param isJoinMask Признак сохранения маски вместе с данными
 * @param captionStyle Стиль заголовка
 * @param isJoined Признак "Объединять с предыдущим полем"
 */
case class MetaCardField(
  val metafield: Metafield,
  val caption: String,
  val sortOrder: Int,
  val isReadOnly: Boolean,
  val format: String,
  val isDropDownNotAllowed: Boolean,
  val isMasked: Boolean,
  val isVisible: Boolean,
  val height: Int,
  val width: Int,
  val groupId: String,
  val sectionId:String,
  val defaultFormGridId: String,
  val extInfo: String,
  val isRequired: Boolean,
  val refParams: String,
  val mask: String,
  val isJoinMask: Boolean,
  val captionStyle: String,
  val isJoined: Boolean,
  val isRefreshOnChange: Boolean) extends Metafield {

  def getCaption = this.caption
  def getSortOrder = this.sortOrder
  def getIsReadOnly = this.isReadOnly
  def getFormat = this.format
  def getIsDropDownNotAllowed = this.isDropDownNotAllowed
  def getIsMasked = this.isMasked
  def getIsVisible = this.isVisible
  def getHeight = this.height
  def getWidth = this.width
  def getGroupId = this.groupId
  def getDefaultFormGridId = this.defaultFormGridId
  def getExtInfo = this.extInfo
  def getIsRequired = this.isRequired

  def getViewFieldId = metafield.getViewFieldId
  def getCodeName = metafield.getCodeName
  def getEditorType = metafield.getEditorType
  def getTypeCode = metafield.getTypeCode
  def getTypeExtInfo = metafield.getTypeExtInfo
  def getSectionId = this.sectionId
}

/**
 * Группа полей.
 *
 * @param id Идентификатор группы
 * @param caption Название группы
 * @param sortOrder Порядок сортировки
 */
case class FieldGroup(id: String, caption: String, sortOrder: Int) {
  def getId = this.id
  def getCaption = this.caption
  def getSortOrder = this.sortOrder
}

/**
 * Секции полей на вкладке
 *
 * @param id Идентификатор группы
 * @param caption Название группы
 * @param sortOrder Порядок сортировки
 */
case class FieldSection(id: String, caption: String, sortOrder: Int,groupId:String, sectionParentId:String) {
  def getId = this.id
  def getCaption = this.caption
  def getSortOrder = this.sortOrder
  def getGroupId = this.groupId
}


/**
 * Вспомогательный класс для икапсуляции полей и групп полей.
 *
 * @param fields Поля
 * @param groups Группы полей
 */
case class MetaCardFieldsAndGroups(fields: List[MetaCardField], groups: List[FieldGroup],sections:List[FieldSection])
