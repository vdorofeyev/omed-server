package omed.bf

import omed.model.Value
import ru.atmed.omed.beans.model.meta.CompiledValidationRule


class ProcessState(
  val id: String,
  val sessionId: String,
  val functionId: String,
  var context: Map[String, Value] = Map(),
  var tasks: Seq[ProcessTask] = Seq(),
  var state: ProcessStateType.Value = ProcessStateType.Running,
  var falseValidators:Set[CompiledValidationRule] = Set())