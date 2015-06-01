package omed.fer.PostReserve

import omed.fer.FERReply

/**
 *
 */
class PostReserveReply(val body: scala.xml.Node) extends FERReply {
  val uniqueKey = extractValue("unique-key")
  val reserved = extractValue("status") == "reserved"
}

object PostReserveReply {
  def apply(body: scala.xml.Node) = new PostReserveReply(body)
}

