package omed.bf

import omed.model.Value
import com.google.inject.{Injector, Inject}

class ClientResultParser {
  @Inject var injector: Injector = null

  def parse(task: ProcessTask, clientMessage: String): Map[String, Value] = {

    val clientTask = task.asInstanceOf[ClientTask]

    val step = task.stepType match {
      case "_Meta_BFSSetValue" =>
        val s = new SetValueStep(clientTask.xml)
        injector.injectMembers(s)
        s
      case _ => new ClientStep()
    }

    step.parseResults(clientMessage)
  }
}




