package omed.roi

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 03.12.13
 * Time: 15:27
 * To change this template use File | Settings | File Templates.
 */
case class UeoiExportExpertiseArray(data:java.util.ArrayList[UeoiExportExpertise])
case class UeoiExportExpertise (

                                    id:String,
                                    status:String,
                                    text:String
//                                  val id:String,
//                                  val system_number : String,
//                                  val expert: String,
//                                  val exp_date: String,
//                                  val level: String,
//                                  val solution_type: String,
//                                  val change_lvl_reason:UeoiChangeLevel,
//                                  val problem_having :UeoiProblem ,
//                                  val reasons:UeoiExportReason ,
//                                  val expert_conclusion:UeoiExpertConclusion,
//                                  val head_expert_conclusion:UeoiHeadExpertConclusion
                                )

case class UeoiChangeLevel(
                            val area_desc :String,
                            val area_note:String,
                            val actlist_desc:String,
                            val actlist_note:String,
                            val byconstitution_desc:String,
                            val byconstitution_note:String,
                            val byrights_desc:String,
                            val byrights_note:String
                          )

case class UeoiProblem(
                       val  have_problem  :String,
                       val answer:String
                      )

case class UeoiExportReason(
                            val bylaw_refusal : Int,
                            val bylaw_reason:String,
                            val noproblems_refusal : Int,
                            val noproblems_reason :String,
                            val noactual_refusal  : Int,
                            val noactual_reason  :String,
                            val nosolution_refusal  : Int,
                            val nosolution_reason :String,
                            val againscitizen_refusal : Int,
                            val againscitizen_reason :String,
                            val available_refusal  : Int,
                            val available_reason :String
                          )

case class UeoiExpertConclusion(
                                val to_voting :Int,
                                val no_voting :String
)

case class UeoiHeadExpertConclusion(
                                    val head_expert_name :String,
                                    val solution_desc :String
                                   )