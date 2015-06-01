package omed.fer.PostReserve

import xml.{Text, TopScope, Null, Elem}
import java.util.Date
import java.text.SimpleDateFormat
import java.sql.Time
import omed.fer.FERMessage

/**
 *
 */
class PostReserveMessage(locationId: String, reserveDate: Date, reserveTime:Time, serviceTypeId: String, authToken: String) extends FERMessage {

  val code = "PostReserve"

  val body = {
    val df = new SimpleDateFormat("yyyy-MM-dd")
    val tf = new SimpleDateFormat("HH:mm")
    val xmlBody =
      <client_info>
        <location_id>{ locationId }</location_id>
        <date>{ df.format(reserveDate) }</date>
        <start_time>{ tf.format(reserveTime) }</start_time>
        <service_type_id>{ serviceTypeId }</service_type_id>
        <params>
          <param name="auth_token">{ authToken }</param>
        </params>
      </client_info>

    xmlBody.toString()
  }

}

object PostReserveMessage {

  def apply(locationId: String,  reserveDate: Date, reserveTime:Time, serviceTypeId: String, authToken: String) =
    new PostReserveMessage(locationId, reserveDate,reserveTime, serviceTypeId, authToken)

}