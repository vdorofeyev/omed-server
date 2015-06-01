package omed.push

import scala.xml.{Text, TopScope, Null, Elem}

/**
 * Created with IntelliJ IDEA.
 * User: SamoylovaTAl
 * Date: 05.08.13
 * Time: 11:58
 * To change this template use File | Settings | File Templates.
 */
case class PushNotification (
  parameters: Map[String,String],
  userId:String
  ) {
  def  xml = {
    val updateParametes = parameters.filter(_._2 !=null).map({case(key,value)=>key.replaceAll("\\$$", "__display_string")->value})
     <notification>
       {

       //  updateParametes.foreach({case(key,value) =>Elem(null,key, Null, TopScope,Text(value))})
       for((key,value)<-updateParametes) yield Elem(null,key, Null, TopScope,Text(value))
       }
     </notification>
  }
}

case class PushNotificationSeq(data:Seq[PushNotification])

case class PushNotificationCount(userId:String,count:Int)




