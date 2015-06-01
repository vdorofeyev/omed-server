package omed.forms

/**
 * Created by andrejnaryskin on 05.03.14.
 */
case class ConditionViewField(
      id: String,
      /** Условие переопределения */
      condition: String,
      /** Наименование переопределения */
      name: String,
      /** Приоритет */
      priority: Int,
      /** ИД переопределяемого поля */
      viewFieldId: String,
      redefinitions:Map[String,String]
      /** ИД переопределяемого поля */
//      viewFieldID: String,
//      /** Выражение для значения «грид для открытия по F2» */
//      defaultFormGridIDSourceExp: String,
//      /** Выражение для значения «выпадение запрещено» */
//      dropDownNotAllowedSourceExp: String,
//      /** Выражение для значения «тип элемента управления» */
//      editorTypeSourceExp: String,
//      /** Выражение для значения «дополнительная информация» */
//      extInfoSourceExp: String,
//      /** Выражение для значения «Формат» */
//      formatSourceExp: String,
//      /** Выражение для значения «Видимость» */
//      visibleSourceExp: String,
//      /** Выражение для значения «присоедиенение маски к значению» */
//      joinMaskSourceExp: String,
//      /** Выражение для значения «Маска» */
//      maskSourceExp: String,
//
//    /** Выражение для значения «Только для чтения» */
//      readOnlySourceExp: String
     
      )
{}

case class ConditionViewFieldSeq(data: Seq[ConditionViewField])
