package omed.fer.PutSlot

import java.util.Date
import java.text.SimpleDateFormat
import omed.fer.FERMessage

/**
 *
 */
class PutSlotMessage(name: String, surname: String, patronymic: String, phone: String, clientId: String,
                     slotId: String, authToken: String) extends FERMessage {

  val code = "PutSlot"

  val body = {
    val xmlBody =
      <client_info>
        <name>{ name }</name>
        <surname>{ surname }</surname>
        <patronymic>{ patronymic }</patronymic>
        <phone>{ phone }</phone>
        <client_id>{ clientId }</client_id>
        <params>
          <param name="auth_token">{ authToken }</param>
          <param name=":slot_id">{ slotId }</param>
        </params>
      </client_info>

    xmlBody.toString()
  }

}

object PutSlotMessage {

  def apply(name: String, surname: String, patronymic: String, phone: String, clientId: String,
            slotId: String, authToken: String) =
    new PutSlotMessage(name, surname, patronymic, phone, clientId, slotId, authToken)

}