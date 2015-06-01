package omed.bf

import omed.model.Value

trait ServerStepExecutor {
  /**
   * @param step Process step description with context
   * @return Updated process step description with altered context and result message
   */
  def execute(step: ProcessTask, context: Map[String, Value], processId:String): Map[String, Value]
  
  def canHandle(step: ProcessTask): Boolean
}