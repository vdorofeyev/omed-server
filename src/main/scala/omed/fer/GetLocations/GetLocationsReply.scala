package omed.fer.GetLocations

import omed.fer.FERReply

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 02.10.13
 * Time: 11:34
 * To change this template use File | Settings | File Templates.
 */
class GetLocationsReply (val body: scala.xml.Node) extends FERReply{

     val locations = (body \\ "location").map(f=>(f \ "id").text).toSeq

}
object GetLocationsReply {
  def apply(body: scala.xml.Node) = new GetLocationsReply(body)
}