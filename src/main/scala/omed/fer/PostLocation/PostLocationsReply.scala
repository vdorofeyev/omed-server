package omed.fer.PostLocation

import omed.fer.FERReply

/**
 *
 */
class PostLocationsReply(val body: scala.xml.Node) extends FERReply {
  val id = extractValue("id")
  val name = extractValue("prefix")
}

object PostLocationsReply {
  def apply(body: scala.xml.Node) = new PostLocationsReply(body)
}
