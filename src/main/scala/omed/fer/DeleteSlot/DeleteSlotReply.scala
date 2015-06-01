package omed.fer.DeleteSlot

import omed.fer.FERReply

/**
 *
 */
class DeleteSlotReply(val body: scala.xml.Node) extends FERReply

object DeleteSlotReply {
  def apply(body: scala.xml.Node) = new DeleteSlotReply(body)
}