package omed.validation

import ru.atmed.omed.beans.model.meta._
import omed.model._
import omed.lang.eval.{ExpressionEvaluator, ValidatorEvaluator, ValidatorContext}
import omed.lang.xml.ValidatorExpressionXmlReader
import omed.model.StatusValidatorSeq
import omed.model.StatusInputValidatorSeq
import omed.model.TransitionValidatorSeq
import omed.errors.MetaModelError
import ru.atmed.omed.beans.model.meta.ClassValidationRule
import omed.model.StatusValidatorSeq
import omed.model.StatusInputValidatorSeq
import ru.atmed.omed.beans.model.meta.ObjectStatusTransition
import omed.model.TransitionValidatorSeq
import ru.atmed.omed.beans.model.meta.ValidationRule
import com.google.inject.Inject
import omed.bf.{ValidationWarningPool, ConfigurationProvider}
import omed.system.ContextProvider
import omed.db.DBProfiler
import omed.cache.ExecStatProvider
import omed.data.EntityFactory
import omed.lang.struct.Validator
import java.util.logging.Logger

object ValidationProviderType extends Enumeration {
  val Edit = Value("Edit")
  val BF = Value("BF")
  val Transition = Value("Transition")
}


/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 15.08.13
 * Time: 12:56
 * To change this template use File | Settings | File Templates.
 */
class ValidationProviderImpl extends ValidationProvider{
  val logger = Logger.getLogger(this.getClass.getName())
  @Inject
  var metaClassProvider:MetaClassProvider = null

  @Inject
  var configProvider: ConfigurationProvider = null

  @Inject
  var contextprovider:ContextProvider =null

  @Inject
  var execStatProvider:ExecStatProvider = null

  @Inject
  var model:MetaModel = null

  @Inject
  var expressionEvaluator: omed.model.services.ExpressionEvaluator = null
  @Inject
  var entityFactory:EntityFactory = null
  @Inject
  var validationWarningPool:ValidationWarningPool = null

  def getComlpexEditValidators(classId:String,statusId:String,fields:Map[String,Any],context: Map[String,omed.model.Value]):(Seq[CompiledValidationRule],Seq[CompiledValidationRule]) = {
    getFailedValidators(
      compileValidators(getClassValidationRules(classId)++getStatusValidationRules(classId,statusId),context).filter( compiledValidator=>{
      val usedFields = compiledValidator.compiled.condition.getUsedVariableFields("this")
    // если не warning, то проверяем, что валидатор проверяет хотя бы одно из сохраняемых полей;
    // для warning будем выполнять все валидаторы всегда
      (compiledValidator.validationRule.isInstanceOf[ClassValidationRule] &&
      compiledValidator.validationRule.asInstanceOf[ClassValidationRule].validationResultType ==
        ValidationResultType.Warning) || fields.find(el => usedFields.contains(el._1)) != None
    }),context)
  }
  def getComlpexBFValidators(bfId:String,context: Map[String,omed.model.Value]):(Seq[CompiledValidationRule],Seq[CompiledValidationRule])=  {
    getFailedValidators(compileValidators(getBFValidationRules(bfId),context),context)
  }
  def getComlpexTransitionValidators(classId:String, transition:ObjectStatusTransition,context: Map[String,omed.model.Value]):(Seq[CompiledValidationRule],Seq[CompiledValidationRule])=   {
    getFailedValidators(
      compileValidators(getStatusValidationRules(classId, transition.beginStatusID) ++
      getInputStatusValidationRules(classId, transition.endStatusID) ++
      getTransitionValidationRules(classId, transition.id) ++
      getClassValidationRules(classId),context),
      context)
  }

  def compileValidators(validators:Seq[ClassValidationRule],context: Map[String,omed.model.Value]):Seq[CompiledValidationRule] ={
      validators.map(rule => {
        val validator = try {
          expressionEvaluator.compile(rule.condition,currentConfig,context)
        } catch {
          case e@_ => throw new MetaModelError(String.format("Некорректно задан валидатор [%s]", rule.name), e)
        }
        new CompiledValidationRule(rule, Validator(null,validator))
      })

  }

  def getFailedValidators(validators: Seq[CompiledValidationRule], runningContext: Map[String,omed.model.Value]):(Seq[CompiledValidationRule],Seq[CompiledValidationRule])={
    DBProfiler.profile("Validation",execStatProvider, true){
      val failedValidators = validators.filterNot(compiledValidator => {
        DataType.boolValueFromValue(expressionEvaluator.evaluate(compiledValidator.compiled.condition,currentConfig,runningContext))
        //ValidatorEvaluator.evaluate(runningContext, currentConfig, compiledValidator.compiled,contextprovider.getContext.timeZone)
      })
      val ce = failedValidators.filter(_.validationRule.asInstanceOf[ClassValidationRule]
        .validationResultType == ValidationResultType.Error)

      val cw = failedValidators.filter(_.validationRule.asInstanceOf[ClassValidationRule]
        .validationResultType == ValidationResultType.Warning)
      validationWarningPool.addWarnings(ce++cw)
      (ce, cw)
    }
  }
  lazy val currentConfig = configProvider.create()
  /**
   * Получить правила валидации для текущего статуса
   */
  def getStatusValidationRules(classId:String, statusId: String) = {
    if (statusId == null) Seq() else {
      val cvmap = getClassValidationRulesMap(classId)

      // получаем идентификаторы валидаторов
      val svr = metaClassProvider.getStatusValidationRules.getOrElse(statusId, StatusValidatorSeq(Seq()))
      // ищем из коллекции валидаторов класса нужные по идентификаторам
      val statusRules =svr.data.map(_.validatorId).map(cvmap)

      statusRules
    }
  }
  /**
   * Получить правила валидации для входа в следующий статус
   */
  def getInputStatusValidationRules(classId: String, statusId: String) = {
    if (statusId == null) Seq() else {
      val cvmap = getClassValidationRulesMap(classId)

      // получаем идентификаторы валидаторов
      val svr = metaClassProvider.getStatusInputValidationRules.getOrElse(statusId, StatusInputValidatorSeq(Seq()))
      // ищем из коллекции валидаторов класса нужные по идентификаторам
      val statusRules = svr.data.map(_.validatorId).map(cvmap)

      statusRules
    }
  }
  def getBFValidationRules(bfId:String)={
    if (bfId == null) Seq() else {
      val cvmap = getClassValidationRulesMap(null)

      // получаем идентификаторы валидаторов
      val svr = metaClassProvider.getBFValidationRules.getOrElse(bfId, BFValidatorSeq(Seq()))
      // ищем из коллекции валидаторов класса нужные по идентификаторам
      val bfRules = svr.data.map(_.validatorId).map(cvmap)

      bfRules
    }
  }
  def getClassValidationRules(classId:String)={
      val cvr = metaClassProvider.getClassValidationRules(classId)
     // отсечь правила, которые не входят в текущий домен и включены в статусы
     cvr.filter(_.isEnabledInDomain).filterNot(_.isUsedInStatus)
  }
  //валидаторы класса использующиеся в переходах, статусах или БФ
  def getClassValidationRulesMap(classId: String) = {
    val cvr = metaClassProvider.getClassValidationRules(classId)
    // отсечь правила, которые не входят в текущий домен и не включены в статусы
    val cvmap = cvr.filter(_.isEnabledInDomain).filter(_.isUsedInStatus).map(p => p.id -> p).toMap
    cvmap
  }
  def getTransitionValidationRules(classId :String,transitionId:String)= {
    if (transitionId == null) Seq() else {
      val cvmap = getClassValidationRulesMap(classId)

      // получаем идентификаторы валидаторов
      val tvr = metaClassProvider.getTransitionValidationRules.getOrElse(transitionId, TransitionValidatorSeq(Seq()))
      // ищем из коллекции валидаторов класса нужные по идентификаторам
      val transitionRules = tvr.data.map(_.validatorId).map(cvmap)

      transitionRules
    }
  }

}
