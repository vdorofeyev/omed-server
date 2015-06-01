package omed.fer.GetLocations

import omed.fer.FERMessage

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 02.10.13
 * Time: 11:25
 * To change this template use File | Settings | File Templates.
 */
class GetLocationsMessage( placeId: String,
                           authToken: String) extends FERMessage{
  val code = "GetLocations"

  val body = {
    <params>
      <param name=":place_id">{placeId}</param>
      <param name="auth_token">{authToken}</param>
    </params>.toString()
  }
}

object GetLocationsMessage {
  def apply (placeId:String,authToken:String): GetLocationsMessage ={
    new GetLocationsMessage(placeId,authToken)
  }
}