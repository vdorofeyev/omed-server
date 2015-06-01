package omed.fer.PutSlot

import omed.fer.FERReply

/**
 *
 */
class PutSlotReply(val body: scala.xml.Node) extends FERReply {
  val uniqueKey = extractValue("unique-key")
  val approved = extractValue("status") == "approved"
}

object PutSlotReply {
  def apply(body: scala.xml.Node) = new PutSlotReply(body)
}


