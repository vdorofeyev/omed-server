package omed.fer.GetReserve

import omed.fer.FERReply

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 01.10.13
 * Time: 17:21
 * To change this template use File | Settings | File Templates.
 */
class GetReserveReply(val body: scala.xml.Node) extends FERReply{

}
object GetReserveReply {
  def apply(body: scala.xml.Node) = new GetReserveReply(body)
}

