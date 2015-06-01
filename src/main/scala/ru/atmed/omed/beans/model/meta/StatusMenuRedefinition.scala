package ru.atmed.omed.beans.model.meta

import omed.rest.model2xml.Model2Xml
import omed.forms.{MetaDataSeq, MetaFormQuery, MetaCreation}
import java.sql.ResultSet
import omed.lang.eval.DBUtils
import omed.model.MetaClassProvider
import omed.system.ContextProvider

/**
 * Created with IntelliJ IDEA.
 * User: naryshkinaa
 * Date: 07.04.14
 * Time: 10:49
 * To change this template use File | Settings | File Templates.
 */
case class StatusMenuRedefinition(statusId:String, menuId:String,caption:String,isReadOnly:Option[Boolean],isVisible:Option[Boolean]) {
  def xmlString:String={
    new StringBuilder().append("<menu ")
      .append(Model2Xml.attribute("caption",caption))
      .append(Model2Xml.attribute("id",menuId))
      .append(Model2Xml.attribute("isReadOnly",isReadOnly.map(_.toString).orNull))
      .append(Model2Xml.attribute("isVisible",isVisible.map(_.toString).orNull))
      .append(" />").toString
  }
}
object StatusMenuRedefinition extends MetaCreation[StatusMenuRedefinition]{
  def apply(dbResult:ResultSet)={
    val caption = dbResult.getString("Caption")
    val menuId = dbResult.getString("MenuID")
    val statusId = dbResult.getString("StatusID")
    val isVisible = DBUtils.fromDbBooleanOption(dbResult.getString("IsVisible"))
    val isReadOnly = DBUtils.fromDbBooleanOption(dbResult.getString("IsReadOnly"))
    if(statusId!= null && menuId != null ) StatusMenuRedefinition(statusId,menuId,caption,isReadOnly,isVisible)
    else null
  }
  def query(metaClassProvider:MetaClassProvider,contextProvider:ContextProvider):String={
    MetaFormQuery.statusMenuQuery + metaClassProvider.getFilterModuleInDomain(contextProvider.getContext.domainId)
  }
  override def  groupValue=
    s => {
      s.statusId
    }
  override def createSeqObj(data:Seq[StatusMenuRedefinition])={
    StatusMenuRedefinitionSeq(data)
  }
  override def storedSeqClass = classOf[StatusMenuRedefinitionSeq]

}

case class StatusMenuRedefinitionSeq(data:Seq[StatusMenuRedefinition]) extends MetaDataSeq[StatusMenuRedefinition]