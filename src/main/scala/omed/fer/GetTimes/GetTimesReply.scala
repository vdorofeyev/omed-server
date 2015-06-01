package omed.fer.GetTimes

import omed.fer.FERReply

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 01.10.13
 * Time: 18:19
 * To change this template use File | Settings | File Templates.
 */
class GetTimesReply (val body: scala.xml.Node) extends FERReply{
   val  slots = (body \\ "slot").map(f=>(f \ "time").text->(f \ "reserved").text.toBoolean).toMap
}
object GetTimesReply {
  def apply(body: scala.xml.Node) = new GetTimesReply(body)
}
