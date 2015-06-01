package omed.mocks

import omed.model._
import ru.atmed.omed.beans.model.meta._
import sun.reflect.generics.reflectiveObjects.NotImplementedException
import scala.collection.mutable.HashMap
import omed.data.{ColorRule, ColorRuleSeq}
import omed.forms.{ConditionViewFieldSeq, ConditionViewFieldGridSeq, ConditionViewFieldCardSeq}

class MetaClassProviderMock extends MetaClassProvider {

  // Контекст
  var metaObjects = new HashMap[String, omed.model.MetaObject]
  var records2classes = new HashMap[String, String]
  // HashMap[(<classId>, <diagrammId>), List[MetaObjectStatus]
  var objectStatuses = new HashMap[(String, String), List[MetaObjectStatus]]
  // HashMap[<classId>, Seq[ClassValidationRule]
  var classValidtionRules = new HashMap[String, Seq[ClassValidationRule]]
  var fieldValidationRules = Map.empty[String, List[FieldValidationRule]]

  def getClassByRecord(objectId: String): MetaObject =
    if (this.records2classes.contains(objectId))
      if (metaObjects.contains(this.records2classes(objectId)))
        metaObjects(this.records2classes(objectId))
      else
        throw new NotImplementedException
    else
      throw new NotImplementedException

  /**
   * Получить метаописание классов
   */
  def getAllClassesMetadata(): Seq[MetaObject] =
    this.metaObjects.values.toSeq

  def getAllClasses(): Map[String, MetaObject] =
    throw new NotImplementedException
  def getStatusDescription(statusId:String):Option[MetaObjectStatus] =
    Option(null)
  def getModuleInDomain(domain:Int)  =
    throw new NotImplementedException
  /**
   * Получить метаописание класса
   */
  def getClassMetadata(classId: String): MetaObject =
    if (this.metaObjects.contains(classId))
      this.metaObjects(classId)
    else
      throw new NotImplementedException
   def getAllParents(classId:String):Set[String]={
     Set(classId)
   }
  def getClassByCode(code: String): MetaObject = {
    val mayBeFind = this.metaObjects.values.find(_.code == code)
    if (mayBeFind != None)
      mayBeFind.get
    else null
  }
  def getClassAndProperty(arrayName:String):(String,String) =
    throw new NotImplementedException

  /**
   * Получить правила окрашивания
   */
  def getColorRules(classId:String): Seq[ColorRule] =
    Seq()

  def getStatusMenu(recordId: String): List[StatusMenu] =
    throw new NotImplementedException

  def getClassStatusDiagramm(classId: String, diagrammId: String): List[MetaObjectStatus] =
    if (this.objectStatuses.contains(classId -> diagrammId))
      this.objectStatuses(classId -> diagrammId)
    else
      List.empty[MetaObjectStatus]

  def getClassStatusTransitions(classId: String): Seq[ObjectStatusTransition] =
    throw new NotImplementedException


  // переопределения метаданных по данным


  def getConditionViewField(): Map[String, ConditionViewFieldSeq] = Map()

  // ------ validation -------------
  def getClassValidationRules(classId: String): Seq[ClassValidationRule] =
    if (this.classValidtionRules.contains(classId))
      this.classValidtionRules(classId)
    else
      Seq()

  def getFieldValidationRules: Map[String, List[FieldValidationRule]] =
    this.fieldValidationRules

  /**
   * Получение коллекции валидаторов для статуса
   */
  def getStatusValidationRules: Map[String, StatusValidatorSeq] = Map()

  def getBFValidationRules:Map[String,BFValidatorSeq] = Map()
  /**
   * Получение коллекции валидаторов для входа в статус
   */
  def getStatusInputValidationRules: Map[String, StatusInputValidatorSeq] = Map()

  /**
   * Получение коллекции валидаторов для перехода
   */
  def getTransitionValidationRules: Map[String, TransitionValidatorSeq] = Map()

  def dropCache(domain:Seq[Int]) {}

  def getFilterModuleInDomain(domain: Int, alias: String) = null
}