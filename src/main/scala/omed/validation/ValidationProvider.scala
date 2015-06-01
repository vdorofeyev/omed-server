package omed.validation

import ru.atmed.omed.beans.model.meta.{CompiledValidationRule, ValidationRule, ObjectStatusTransition, ClassValidationRule}
import omed.lang.eval.ValidatorContext

/**
 * Протокол получения валидаторов.
 */
trait ValidationProvider {
   def getComlpexEditValidators(classId:String,statusId:String,fields:Map[String,Any],context: Map[String,omed.model.Value]): (Seq[CompiledValidationRule],Seq[CompiledValidationRule])
   def getComlpexBFValidators(bfId:String,context: Map[String,omed.model.Value]):(Seq[CompiledValidationRule],Seq[CompiledValidationRule])
   def getComlpexTransitionValidators(classId:String, transition:ObjectStatusTransition,context: Map[String,omed.model.Value]):(Seq[CompiledValidationRule],Seq[CompiledValidationRule])
}
