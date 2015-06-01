package omed.fer.PutLocationSchedule

import xml.{Text, TopScope, Null, Elem}
import java.util.Date
import java.text.SimpleDateFormat
import omed.fer.FERMessage


/**
 *
 */
class PutLocationScheduleMessage(val locationId: String,
                           val authToken: String,
                           val templates: Seq[LocationSchedulerParameter],
                           val startDate: Date,
                           val endDate: Date) extends FERMessage {

  val code = "PutLocationSchedule"

  val body = {
    val df = new SimpleDateFormat("yyyy.MM.dd")
    def typeStr(isEven:Boolean):String = if (isEven) "even_week" else "odd_week"

    val xmlBody =
      <applied_schedule>
        <applied_short_day />
        <applied_nonworking_day />
        <applied_exception />
        <applied_rule>{
          for ( (templateParameter,i) <- templates.zipWithIndex )

            yield {
              val  rule =  <rule_id>{ templateParameter.ruleId }</rule_id>
                <start_date>{ df.format(startDate) }</start_date>
                <end_date>{ df.format(endDate) }</end_date>
                <type>{ typeStr(templateParameter.isEven) }</type>;

              Elem(null, "rule" + (i + 1), Null, TopScope, rule: _*)
            }


          }
        </applied_rule>
        <params>
          <param name="auth_token">{ authToken }</param>
          <param name=":location_id">{ locationId }</param>
        </params>
      </applied_schedule>


    xmlBody.toString()
  }

}

case class LocationSchedulerParameter(ruleId:String, isEven:Boolean)

object PutLocationScheduleMessage {

  def apply(locationId: String,
            authToken: String,
            templates:Seq[LocationSchedulerParameter],
            startDate: Date,
            endDate: Date
            ) =
    new PutLocationScheduleMessage(locationId, authToken, templates, startDate, endDate)

}




