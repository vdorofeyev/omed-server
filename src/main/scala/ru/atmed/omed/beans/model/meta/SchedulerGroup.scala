package ru.atmed.omed.beans.model.meta

import java.sql.ResultSet
import omed.lang.eval.DBUtils
import omed.rest.model2xml.Model2Xml
import omed.forms.{MetaDataSeq, MetaFormQuery, MetaCreation}
import omed.model.MetaClassProvider
import omed.system.ContextProvider

/**
 * Created with IntelliJ IDEA.
 * User: naryshkinaa
 * Date: 02.04.14
 * Time: 17:07
 * To change this template use File | Settings | File Templates.
 */
case class SchedulerGroup(windowGridId:String,sortOrder:Int,isVertical:Boolean,groupType:String,code:String,caption:String) {
  def xmlString:String={
    new StringBuilder().append("<group>")
      .append(Model2Xml.tag("code",code))
      .append(Model2Xml.tag("sortOrder",sortOrder.toString))
      .append(Model2Xml.tag("isVertical",isVertical.toString))
      .append(Model2Xml.tag("type",groupType))
      .append(Model2Xml.tag("caption",caption))
      .append("</group>").toString
  }
}
object SchedulerGroup extends MetaCreation[SchedulerGroup]{
  def apply(dbResult:ResultSet)={
    val windowGridId = dbResult.getString("WindowGridID")
    val sortOrder = dbResult.getInt("SortOrder")
    val isVertical = DBUtils.fromDbBoolean(dbResult.getString("isVertical"))
    val groupType = dbResult.getString("Type")
    val code = dbResult.getString("Code")
    val caption = dbResult.getString("Name")
    new SchedulerGroup(windowGridId,sortOrder,isVertical,groupType,code,caption)
  }
  def query(metaClassProvider:MetaClassProvider,contextProvider:ContextProvider):String={
    MetaFormQuery.schedulerGroupQuery + metaClassProvider.getFilterModuleInDomain(contextProvider.getContext.domainId,"MSG")
  }
  override def  groupValue=
    s => {
      s.windowGridId
    }
  override def createSeqObj(data:Seq[SchedulerGroup])={
      SchedulerGroupSeq(data)
  }
  override def storedSeqClass = classOf[SchedulerGroupSeq]

}

case class SchedulerGroupSeq(data:Seq[SchedulerGroup]) extends MetaDataSeq[SchedulerGroup]



