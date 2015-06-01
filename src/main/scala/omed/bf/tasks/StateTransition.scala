package omed.bf.tasks

import omed.bf.ProcessTask


class StateTransition(
  val transitionId: String
) extends ProcessTask("_Meta_BFSTransition")

object StateTransition {
  def apply(xml: scala.xml.Node): StateTransition = {
    val transitionId = xml.attribute("TransitionID")
      .map(_.head).map(_.text).orNull
    new StateTransition(transitionId)
  }
}