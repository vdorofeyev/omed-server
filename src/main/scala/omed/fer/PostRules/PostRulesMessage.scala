package omed.fer.PostRules

import xml.{TopScope, Null, Elem}
import omed.fer.FERMessage


/**
 *
 */
class PostRulesMessage(scheduleName: String,
                       placeId: String,
                       authToken: String,
                       dayRules: Seq[PostRulesMessage.DailyTimetable]) extends FERMessage {

  val code = "PostRules"

  val body = {
    val clearedTimetable = PostRulesMessage.clearTimetable(dayRules)

    val xmlBody =
      <rule_data>
        <schedules_rule>
          <name>{ scheduleName }</name>
        </schedules_rule>
        <day_rule>{
          clearedTimetable map {
            case PostRulesMessage.DailyTimetable(n, wb, bb, be, we) => {
              val intervals = if (bb == null) {
                <int0>
                  <time0>{wb}</time0>
                  <time1>{we}</time1>
                </int0>
              } else {
                <int0>
                  <time0>{wb}</time0>
                  <time1>{bb}</time1>
                </int0>
                  <int1>
                    <time0>{be}</time0>
                    <time1>{we}</time1>
                  </int1>
              }

              Elem(null, "day" + n, Null, TopScope, intervals: _*)
            }
          }
        }</day_rule>
        <params>
          <param name=":place_id">{ placeId }</param>
          <param name="auth_token">{ authToken }</param>
        </params>
      </rule_data>

    xmlBody.toString()
  }
   def isEmpty={
     val clearedTimetable = PostRulesMessage.clearTimetable(dayRules)
     clearedTimetable.isEmpty
   }
}

object PostRulesMessage {

  case class DailyTimetable(dayNumber: Int,
                            workBeginTime: String,
                            breakBeginTime: String,
                            breakEndTime: String,
                            workEndTime: String)

  def apply(scheduleName: String,
            placeId: String,
            authToken: String,
            dayRules: Seq[PostRulesMessage.DailyTimetable]) =
    new PostRulesMessage(scheduleName, placeId, authToken, dayRules)

  private def clearTimetable(timetable: Seq[DailyTimetable]) = {
    timetable.map({
      case d @ DailyTimetable(n, wb, bb, be, we) if (wb != null && we != null && bb != null && be != null) => d
      case DailyTimetable(n, wb, null, _, we) if (wb != null && we != null) =>
        DailyTimetable(n, wb, null, null, we)
      case DailyTimetable(n, wb, _, null, we) if (wb != null && we != null) =>
        DailyTimetable(n, wb, null, null, we)
      case DailyTimetable(n, wb, bb, _, null) if (wb != null && bb != null) =>
        DailyTimetable(n, wb, null, null, bb)
      case DailyTimetable(n, null, _, be, we) if (be != null && we != null) =>
        DailyTimetable(n, be, null, null, we)
      case _ => null
    }).filterNot(_ == null)
  }

}