package ru.atmed.omed.beans.model.meta

import scala.xml.Elem

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 13.12.13
 * Time: 13:57
 * To change this template use File | Settings | File Templates.
 */
case class StatusSection (val sectionId:String, val caption:String, val groupId:String, val sortOrder:Option[Int], val statusId:String, val viewCardId:String) {
  def toXml :Elem={

      <section id={ sectionId }
       caption = {Option(caption) orNull}
       groupId ={Option(groupId) orNull}
       sortOrder= {Option(sortOrder.map(f => f.toString).getOrElse(null)) orNull}/>
  }

}

case class StatusSectionSeq(data:Seq[StatusSection])
