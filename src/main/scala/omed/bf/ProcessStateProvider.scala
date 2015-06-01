package omed.bf

trait ProcessStateProvider {
  
  def getProcess(processId: String): Option[ProcessState]
  
  def putProcess(processId: String, info: ProcessState): Unit
  
  def dropProcess(processId: String): Unit

}