package omed.bf

import omed.model.Value
import ru.atmed.omed.beans.model.meta.CompiledValidationRule

trait BusinessFunctionExecutor {
  
  /**
   * Инициализировать бизнес-функцию
   *
   * @param functionId Идентификатор функции в базе данных
   * @param params Параметры для триггеров
   */
  def initFunctionInstance(
    functionId: String,
    params: Map[String, Value] = Map()): String
  /**
   * Получить метаданные для следующего клиентского шага
   *
   * @param processId Идентификатор экземпляра бизнес функции
   */
  def getNextClientStep(processId: String): (Option[ClientTask],Option[Value])

  def getContext(processId: String): Map[String, Value]

  def getFalseValidations(processId: String):  Set[CompiledValidationRule]
  def setFalseValidations(processId: String, validators:Set[CompiledValidationRule])
  /**
   * Установить результат клиентской функции
   *
   */
  def setClientResult(processId: String, clientMessage: String): Unit

}
