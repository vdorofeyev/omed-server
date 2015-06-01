package ru.atmed.omed.beans.model.meta

import omed.forms.{MetaFormQuery, MetaCreation}
import java.sql.ResultSet
import omed.lang.eval.DBUtils
import omed.model.MetaClassProvider
import omed.system.ContextProvider

/**
 * Created with IntelliJ IDEA.
 * User: naryshkinaa
 * Date: 03.04.14
 * Time: 18:13
 * To change this template use File | Settings | File Templates.
 */
case class TemplateClass(id:String,classId:String,newObjectClassId:String,templateClassTypeId:String,var templateProperties:Seq[TemplateClassProperty]) {

}

object TemplateClass extends MetaCreation[TemplateClass]{
  def apply(dbResult:ResultSet)={
    val id = dbResult.getString("ID")
    val classId = dbResult.getString("ClassID")
    val newObjectClassId = dbResult.getString("NewObjectClassID")
    val templateClassTypeId = dbResult.getString("TemplateClassTypeID")
    new TemplateClass(id,classId,newObjectClassId,templateClassTypeId,Seq())
  }
  def query(metaClassProvider:MetaClassProvider,contextProvider:ContextProvider):String={
    MetaFormQuery.templateClassQuery
  }
  override def idValue =f => {
     f.classId + f.templateClassTypeId
  }
  override def  storedObjectClass = classOf[TemplateClass]

//  override def  childsCompanions = {
//    Seq(
//      (TemplateClassProperty, f => f.id)
//    )
//  }

}
