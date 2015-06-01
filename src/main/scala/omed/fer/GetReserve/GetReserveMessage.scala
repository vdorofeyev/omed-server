package omed.fer.GetReserve

import omed.fer.FERMessage

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 01.10.13
 * Time: 16:21
 * To change this template use File | Settings | File Templates.
 */
class GetReserveMessage ( placeId: String,
                         authToken: String) extends FERMessage{
  val code = "GetReserve"

  val body = {
  <params>
    <param name=":place_id">{placeId}</param>
    <param name="auth_token">{authToken}</param>
    <param name="start_date">2013-09-01</param>
    <param name="end_date">2013-10-30</param>
  </params>.toString()
  }
}

object GetReserveMessage {
  def apply (placeId:String,authToken:String): GetReserveMessage ={
    new GetReserveMessage(placeId,authToken)
  }
}