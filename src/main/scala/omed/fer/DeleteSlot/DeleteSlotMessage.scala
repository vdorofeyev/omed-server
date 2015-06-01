package omed.fer.DeleteSlot

import omed.fer.FERMessage

/**
 *
 */
class DeleteSlotMessage(slotId: String, authToken: String) extends FERMessage {

  val code = "DeleteSlot"

  val body = {
    val xmlBody =
      <params>
        <param name=":slot_id">{ slotId }</param>
        <param name="auth_token">{ authToken }</param>
      </params>

    xmlBody.toString()
  }

}

object DeleteSlotMessage {

  def apply(slotId: String, authToken: String) =
    new DeleteSlotMessage(slotId, authToken)

}