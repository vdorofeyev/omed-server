package omed.bf

import omed.model.Value

/**
 * Интерфейс для обработчиков шагов процесса.
 */
trait ProcessStepHandler {

  /**
   * Имя шага.
   */
  val name: String = "Step"

  /**
   * Проверка возможности выполнения данного шага.
   * 
   * @param step Задача на обработку
   * @return `Правда` | `Ложь`
   */
  def canHandle(step: ProcessTask) = {
    step.stepType == name
  }
  /**
   * Обработать шаг.
   * 
   * @param step Описание шага процесса
   * @param context Текущий контекст
   * @return Контекст процесса
   */
  def handle(step: ProcessTask, context: Map[String, Value],processId:String): Map[String, Value]

}