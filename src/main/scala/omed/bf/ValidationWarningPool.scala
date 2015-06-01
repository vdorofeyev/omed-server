package omed.bf

import ru.atmed.omed.beans.model.meta.CompiledValidationRule
import scala.collection.mutable
/**
 * Пулл предупреждений при работе БФ и редактировании объекта
 */
class ValidationWarningPool {
  val failedValidators = mutable.Set[CompiledValidationRule]()//new ThreadLocal[Set[CompiledValidationRule]]
  def getWarnings : Set[CompiledValidationRule]={
    failedValidators.toSet
  }
  def addWarnings(warnings:Seq[CompiledValidationRule]){
    failedValidators ++= warnings
  }
  def clearPool{
    failedValidators.clear()
  }
}
