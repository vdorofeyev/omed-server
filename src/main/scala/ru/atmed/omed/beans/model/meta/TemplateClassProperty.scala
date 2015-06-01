package ru.atmed.omed.beans.model.meta

import java.sql.ResultSet
import omed.forms.{MetaFormQuery, MetaCreation}
import omed.system.ContextProvider
import omed.model.MetaClassProvider

/**
 * Created with IntelliJ IDEA.
 * User: naryshkinaa
 * Date: 04.04.14
 * Time: 11:37
 * To change this template use File | Settings | File Templates.
 */
case class TemplateClassProperty(templateClassId:String, fromPropertyCode:String,toPropertyCode:String)

object TemplateClassProperty extends MetaCreation[TemplateClassProperty]{
  def apply(dbResult:ResultSet)={
    val templateClassId = dbResult.getString("TemplateClassID")
    val fromPropertyCode = dbResult.getString("ClassPropertyCode")
    val toPropertyCode = dbResult.getString("NewObjectClassPropertyCode")
    new TemplateClassProperty(templateClassId,fromPropertyCode,toPropertyCode)
  }
  def query(metaClassProvider:MetaClassProvider,contextProvider:ContextProvider):String={
    MetaFormQuery.templatePropertyClassQuery
  }
  override def groupValue=
  s => {
    s.templateClassId
  }

}
