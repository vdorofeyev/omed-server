package omed.bf

import scala.collection.mutable.{ Map => MutableMap }

class ProcessStateProviderImpl extends ProcessStateProvider {

  private val procMap = MutableMap[String, ProcessState]()
  
  def getProcess(processId: String): Option[ProcessState] = {
    procMap.get(processId)
  }
  
  def putProcess(processId: String, info: ProcessState): Unit = {
    procMap.put(processId, info)
  }
  
  def dropProcess(processId: String): Unit = {
    procMap.remove(processId)
  }

}