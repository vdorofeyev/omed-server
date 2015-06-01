package omed.roi
import java.util
/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 02.12.13
 * Time: 19:55
 * To change this template use File | Settings | File Templates.
 */




case class UeoiImportExpertiseArray (val data :util.ArrayList[UeoiImportExpertise])

case class UeoiImportExpertise (
                             id:String,
                             code:String,
                             title:String,
                             category :util.ArrayList[String],
                             description:String,
                             prospective:String,
                             attachment:UeoiAttachment,
                             date: UeoiModerationDate,
                             threshold:Int,
                             level : UeoiLevel,
                             location:UeoiLocation,
                             moderator:UeoiModerator,
                             decision:util.ArrayList[UeoiDecision]
//                             val content:String,
//                             val result:Int,
//                             val insult:Int,
//                             val repeated: UeoiRepeatedExpertise,
//                             val reasons:UeoiImportReason,
//                             val attach_links :util.ArrayList[UeoiAttachedLinks]
                          )



case class UeoiAttachment(document:util.ArrayList[UeoiDocument],photo:util.ArrayList[UeoiDocument])
case class UeoiModerationDate(moderation:UeoiDate)
case class UeoiDate(begin:String)
case class UeoiDocument(title:String, url:String,description:String)
//case class UeoiPhoto(title:String, url:String)
case class UeoiLevel (id:String,title:String)
case class UeoiLocation(region:UeoiLocationDetail, municipality:UeoiLocationDetail)
case class UeoiLocationDetail(id:String,title:String)
case class UeoiModerator(id:String, name :String)
case class UeoiDecision (text:String,attachment:util.ArrayList[UeoiDocument])

case class UeoiRepeatedExpertise(
                                  val denial:Int,
                                  val useless: Int,
                                  val repeated : Int,
                                  val link_number:String
                                )

case class UeoiImportReason(
                       val foreign_indeed :Int,
                       val foreign_desc:String,
                       val threat_indeed: Int,
                       val threat_desc:String,
                       val extreme_indeed:Int,
                       val extreme_desc:String,
                       val infring_indeed:Int,
                       val infring_desc:String,
                       val nolaw_indeed:Int,
                       val nolaw_desc:String,
                       val secrecy_indeed:Int,
                       val secrecy_desc:String,
                       val obscure_indeed:Int,
                       val obscure_desc:String,
                       val commercial_indeed:Int,
                       val commercial_desc:String
                      )

case class UeoiAttachedLinks(val number:Int,val desc:String,val link:String)


