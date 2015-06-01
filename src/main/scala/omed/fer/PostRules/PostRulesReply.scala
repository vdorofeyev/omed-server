package omed.fer.PostRules

import omed.fer.FERReply

/**
 *
 */
class PostRulesReply(val body: scala.xml.Node) extends FERReply {
  val id = extractValue("id")
}

object PostRulesReply {
  def apply(body: scala.xml.Node) = new PostRulesReply(body)
}