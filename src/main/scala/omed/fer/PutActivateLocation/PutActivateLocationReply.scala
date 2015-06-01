package omed.fer.PutActivateLocation

import omed.fer.FERReply

/**
 *
 */
class PutActivateLocationReply(val body: scala.xml.Node) extends FERReply {
  val locationId = extractValue("location-id")
  val active = extractValue("active")
}

object PutActivateLocationReply {
  def apply(body: scala.xml.Node) = new PutActivateLocationReply(body)
}



