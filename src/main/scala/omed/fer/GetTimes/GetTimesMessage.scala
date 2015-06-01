package omed.fer.GetTimes

import omed.fer.FERMessage

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 01.10.13
 * Time: 18:16
 * To change this template use File | Settings | File Templates.
 */
class GetTimesMessage ( locationId: String,
                        authToken: String,
                        date:String) extends FERMessage{
  val code = "GetTimes"

  val body = {
    <params>
      <param name=":location_id">{locationId}</param>
      <param name="auth_token">{authToken}</param>
      <param name="date">{date}</param>
    </params>.toString()
  }
}

object GetTimesMessage {
  def apply (locationId:String,authToken:String,date:String): GetTimesMessage ={
    new GetTimesMessage(locationId,authToken,date)
  }
}
