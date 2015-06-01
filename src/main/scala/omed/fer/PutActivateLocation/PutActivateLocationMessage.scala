package omed.fer.PutActivateLocation

import java.util.Date
import java.text.SimpleDateFormat
import omed.fer.FERMessage


/**
 *
 */
class PutActivateLocationMessage(val locationId: String,
                           val authToken: String) extends FERMessage {

  val code = "PutActivateLocation"

  val body = {
    val xmlBody =
      <params>
        <param name="auth_token">{ authToken }</param>
        <param name=":location_id">{ locationId }</param>
      </params>

    xmlBody.toString()
  }

}

object PutActivateLocationMessage {

  def apply(locationId: String,
            authToken: String) =
    new PutActivateLocationMessage(locationId, authToken)

}






