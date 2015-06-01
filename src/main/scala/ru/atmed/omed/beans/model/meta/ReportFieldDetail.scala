package ru.atmed.omed.beans.model.meta

import omed.forms.{MetaDataSeq, MetaFormQuery, MetaCreation}
import java.sql.ResultSet
import omed.lang.eval.DBUtils
import omed.model.MetaClassProvider
import omed.system.ContextProvider

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 09.01.14
 * Time: 15:22
 * To change this template use File | Settings | File Templates.
 */
case class ReportFieldDetail (val fieldId :String, val viewCardId:String,reportTemplateId:String) {

}

object ReportFieldDetail extends MetaCreation[ReportFieldDetail]{
  def apply(dbResult:ResultSet)={
    val fieldId = dbResult.getString("FieldID")
    val viewCardId = dbResult.getString("ViewCardID")
    val reportTemplateId = dbResult.getString("ReportTemplateID")
    if(fieldId!= null && viewCardId!= null && reportTemplateId != null)
      new ReportFieldDetail(fieldId = fieldId,viewCardId = viewCardId,reportTemplateId = reportTemplateId)
    else null
  }
  def query(metaClassProvider:MetaClassProvider,contextProvider:ContextProvider):String={
    MetaFormQuery.reportFieldDetailQuery
  }
  override def  groupValue =
    s => {
       s.reportTemplateId
    }
  override def createSeqObj(data:Seq[ReportFieldDetail])={
    ReportFieldDetailSeq(data)
  }
  override def storedSeqClass = classOf[ReportFieldDetailSeq]

}

case class ReportFieldDetailSeq(val data:Seq[ReportFieldDetail])  extends MetaDataSeq[ReportFieldDetail]
