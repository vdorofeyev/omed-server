package ru.atmed.omed.beans.model.meta

import java.sql.ResultSet
import omed.rest.model2xml.Model2Xml
import omed.forms.{MetaDataSeq, MetaFormQuery, MetaCreation}
import omed.model.MetaClassProvider
import omed.system.ContextProvider

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 07.02.14
 * Time: 14:55
 * To change this template use File | Settings | File Templates.
 */
case class GridInCardItem ( viewCardId :String,caption:String,sortOrder:Option[Int],groupId:String,windowGridId:String) {
  def xmlString:String={
    new StringBuilder().append("<gridInCardItem>")
      .append(Model2Xml.tag("caption",caption))
      .append(Model2Xml.tag("sortOrder",sortOrder.map(f => f.toString).getOrElse(null)))
      .append(Model2Xml.tag("groupId",groupId))
      .append(Model2Xml.tag("windowGridId",windowGridId))
      .append("</gridInCardItem>").toString
  }
}

object  GridInCardItem extends  MetaCreation[GridInCardItem]{
  def apply(resultSet:ResultSet) ={
    new GridInCardItem(
      viewCardId = resultSet.getString("ViewCardID"),
      caption = resultSet.getString("Caption"),
      sortOrder = Option(resultSet.getObject("SortOrder")).map((f => f.asInstanceOf[Int])),
      groupId = resultSet.getString("TabID"),
      windowGridId = resultSet.getString("WindowGridID")
    )
  }
  def query(metaClassProvider:MetaClassProvider,contextProvider:ContextProvider):String={
    MetaFormQuery.GridInCardQuery + metaClassProvider.getFilterModuleInDomain(contextProvider.getContext.domainId)
  }
  override def  groupValue =
    s => {
      s.viewCardId
    }
  override def createSeqObj(data:Seq[GridInCardItem])={
    GridInCardItemSeq(data)
  }
  override def storedSeqClass = classOf[GridInCardItemSeq]
}
case class GridInCardItemSeq(data: Seq[GridInCardItem]) extends MetaDataSeq[GridInCardItem]