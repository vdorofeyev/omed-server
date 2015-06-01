package ru.atmed.omed.beans.model.meta

import omed.lang.struct.Validator

/**
 * Тип результата валидатора.
 */
object ValidationResultType extends Enumeration {
  val Error = Value("Error")
  val Warning = Value("Warning")

  val aliases = Map(
    "error" -> Error,
    "warning" -> Warning)
}

/**
 * Правило валидации.
 *
 * @param classId Идентификатор мета класса
 * @param name Название правила
 * @param condition Условие
 * @param falseMessage Сообщение пользователю в случае непрохождения валидации
 * @param isEnabledInDomain Признак "Включен в домен"
 */
case class ValidationRule(
  val classId: String,
  val name: String,
  val condition: String,
  val falseMessage: String,
  val isEnabledInDomain: Boolean,
  val validationResultType: ValidationResultType.Value
)

/**
 * Правило валидации для класса.
 * 
 * @param classId Идентификатор мета класса
 * @param name Название правила
 * @param condition Условие
 * @param falseMessage Сообщение пользователю в случае непрохождения валидации
 * @param isEnabledInDomain Признак "Включен в домен"
 * @param validationResultType Тип результата: Ошибка, Предупреждение
 */
case class ClassValidationRule(
  val id: String,
  override val classId: String,
  override val name: String,
  override val condition: String,
  override val falseMessage: String,
  override val isEnabledInDomain: Boolean,
  val isUsedInStatus: Boolean,
  override val validationResultType: ValidationResultType.Value)
  extends ValidationRule(classId, name, condition, falseMessage, isEnabledInDomain,validationResultType)

/**
 * Списокв валидатров для класса
 * 
 * @param data Списков валидаторов
 */
case class ClassValidationRuleSeq(data: Seq[ClassValidationRule])

/**
 * Правило валидации для поля.
 * 
 * @param classId Идентификатор мета класса
 * @param name Названия
 * @param condition Условие
 * @param falseMessage Сообщение пользователю в случае непрохождения валидации
 * @param isEnabledInDomain Признак "Включен в домен"
 * @param propertyId Идентификатор поля
 * @param propertyCode Код поля
 */
case class FieldValidationRule(
  override val classId: String,
  override val name: String,
  override val condition: String,
  override val falseMessage: String,
  override val isEnabledInDomain: Boolean,
  override val validationResultType: ValidationResultType.Value,
  propertyId: String,
  propertyCode: String)
  extends ValidationRule(classId, name, condition, falseMessage, isEnabledInDomain, validationResultType)

/**
 * Скомилированное правило
 * 
 * @param validationRule Исходное правило
 * @param compiled Скомпилированное правило
 */
class CompiledValidationRule(
  val validationRule: ValidationRule,
  val compiled: Validator)
{
  override  def equals(o:Any)= o match {
    case that: CompiledValidationRule => (that.validationRule.classId+that.validationRule.condition).equals(validationRule.classId+validationRule.condition)
    case _ => false
  }
  override  def hashCode = (validationRule.classId+validationRule.condition).hashCode
}