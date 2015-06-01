package omed.model

import omed.model._
import ru.atmed.omed.beans.model.meta._
import omed.data.{ColorRule, ColorRuleSeq}
import omed.forms.{ConditionViewFieldSeq, ConditionViewFieldCardSeq, ConditionViewFieldGridSeq}


trait MetaClassProvider {

  def getClassByRecord(objectId: String): MetaObject

  /**
   * Получить метаописание классов
   */
  def getAllClassesMetadata(): Seq[MetaObject]
  
  def getAllClasses(): Map[String, MetaObject]

  def getAllParents(classId:String):Set[String]
  /**
   * Получить метаописание класса
   */
  def getClassMetadata(classId: String): MetaObject
  
  def getClassByCode(code: String): MetaObject

  def getClassAndProperty(arrayName:String):(String,String)
  /**
   * Получить правила окрашивания
   */
  def getColorRules(classId:String):Seq[ColorRule]

  def getStatusMenu(recordId: String): List[StatusMenu]

  def getClassStatusDiagramm(classId: String, diagrammId: String): Seq[MetaObjectStatus]

  def getStatusDescription(statusId:String):Option[MetaObjectStatus]

  def getClassStatusTransitions(classId: String): Seq[ObjectStatusTransition]

  // переопределения метаданных по данным
  def getConditionViewField(): Map[String, ConditionViewFieldSeq]
  
  // ------ validation -------------
  def getClassValidationRules(classId: String): Seq[ClassValidationRule]


  def getFieldValidationRules: Map[String, List[FieldValidationRule]]

  /**
   * Получение коллекции валидаторов для статуса
   */
  def getStatusValidationRules: Map[String, StatusValidatorSeq]

  /**
   * Получение коллекции валидаторов для входа в статус
   */
  def getStatusInputValidationRules: Map[String, StatusInputValidatorSeq]

  /**
   * Получение коллекции валидаторов для перехода
   */
  def getTransitionValidationRules: Map[String, TransitionValidatorSeq]

  def getBFValidationRules: Map[String, BFValidatorSeq]

  def dropCache(domains:Seq[Int])

  def getModuleInDomain(domain:Int):Seq[String]

  def getFilterModuleInDomain(domain :Int,alias:String =null):String
}