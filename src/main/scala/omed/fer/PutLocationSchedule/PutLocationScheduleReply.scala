package omed.fer.PutLocationSchedule

import omed.fer.FERReply

/**
 *
 */
class PutLocationScheduleReply(val body: scala.xml.Node) extends FERReply {
  val locationId = extractValue("location-id")
  val ruleId = extractValue("rule-id")
}

object PutLocationScheduleReply {
  def apply(body: scala.xml.Node) = new PutLocationScheduleReply(body)
}

