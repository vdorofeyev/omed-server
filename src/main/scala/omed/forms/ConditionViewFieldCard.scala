package omed.forms

/**
 * Переопределения поля карточки в статусах
 */
case class ConditionViewFieldCard (
  /** ИД переопределения */
  id: String,
  /** Условие переопределения */
  condition: String,
  /** Выражение для значения «грид для открытия по F2» */
  defaultFormGridIDSourceExp: String,
  /** Выражение для значения «выпадение запрещено» */
  dropDownNotAllowedSourceExp: String,
  /** Выражение для значения «тип элемента управления» */
  editorTypeSourceExp: String,
  /** Выражение для значения «дополнительная информация» */
  extInfoSourceExp: String,
  /** Выражение для значения «Формат» */
  formatSourceExp: String,
  /** Выражение для значения «Видимость» */
  visibleSourceExp: String,
  /** Выражение для значения «присоедиенение маски к значению» */
  joinMaskSourceExp: String,
  /** Выражение для значения «Маска» */
  maskSourceExp: String,
  /** Наименование переопределения */
  name: String,
  /** Приоритет */
  priority: Int,
  /** Выражение для значения «Только для чтения» */
  readOnlySourceExp: String,
  /** ИД переопределяемого поля */
  viewFieldID: String,
  /** Выражение для значения «Заголовок» */
  captionSourceExp: String,
  /** Выражение для значения «Сортировка» */
  sortOrderSourceExp: String,
  /** Выражение для значения «Вкладка» */
  tabSourceExp: String
)

case class ConditionViewFieldCardSeq(data: Seq[ConditionViewFieldCard])