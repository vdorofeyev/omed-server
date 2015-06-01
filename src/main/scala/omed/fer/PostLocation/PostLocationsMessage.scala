package omed.fer.PostLocation

import xml.{Text, TopScope, Null, Elem}
import omed.fer.FERMessage


/**
 *
 */
class PostLocationsMessage(placeId: String,
                           financialSource: String,
                           authToken: String,
                           timetableEmployee: PostLocationsMessage.TimetableEmployeeInfo) extends FERMessage {

  val code = "PostLocations"

  val body = {
    val xmlBody =
      <location>
        <prefix>{ timetableEmployee.name }</prefix>
        <medical_specialization_id>{ timetableEmployee.specialty }</medical_specialization_id>
        <cabinet_number>{ timetableEmployee.cabinet }</cabinet_number>
        <time_table_period>31</time_table_period>
        <reservation_time>1</reservation_time>
        <reserved_time_for_slot>{ timetableEmployee.duration }</reserved_time_for_slot>
        <reservation_type_id>{ timetableEmployee.reservationType }</reservation_type_id>
        <payment_method_id>{ financialSource }</payment_method_id>
        <service_types_ids>{
          for ( (serviceType, i) <- timetableEmployee.serviceTypes.zipWithIndex )
          yield Elem(null, "st" + (i + 1), Null, TopScope, Text(Option(serviceType).getOrElse("null")))
        }</service_types_ids>
        <params>
          <param name=":place_id">{ placeId }</param>
          <param name="auth_token">{ authToken } </param>
        </params>
      </location>

    xmlBody.toString()
  }

}

object PostLocationsMessage {

  case class TimetableEmployeeInfo(timetableEmployeeId: String,
                                   name: String,
                                   cabinet: String,
                                   specialty: String,
                                   duration: Int,
                                   reservationType: String,
                                   serviceTypes: Seq[String])

  def apply(placeId: String,
            financialSource: String,
            authToken: String,
            timetableEmployee: TimetableEmployeeInfo) =
    new PostLocationsMessage(placeId, financialSource, authToken, timetableEmployee)

}

