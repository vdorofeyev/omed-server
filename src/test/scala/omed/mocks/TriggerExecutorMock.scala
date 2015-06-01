package omed.mocks

import omed.triggers._
import omed.model.EntityInstance
import omed.triggers.Trigger

class TriggerExecutorMock extends TriggerExecutor {

  var firedTriggers = new scala.collection.mutable.ArrayBuffer[Trigger]

  def fireTriggers(
    inst: EntityInstance,
    fields: Seq[String],
    event: TriggerEvent.Value,
    period: TriggerPeriod.Value) {

    firedTriggers += Trigger(
      period, events = Set(event), watchList = null, functionID = null,
      inst.obj.id,excludedList = null,isNotInhereted = false, triggerType = TriggerType.BF)
  }
}