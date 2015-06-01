package ru.atmed.omed.beans.model.meta

import scala.xml.{XML, Elem}
import omed.rest.model2xml.Model2Xml
import java.sql.ResultSet

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 10.01.14
 * Time: 15:19
 * To change this template use File | Settings | File Templates.
 */
case class MetaDiagramRelation (id:String, viewDiagramId:String, name:String,relationType:String,startPropertyCode:String,endPropertyCode:String,viewDiagramDetailId:String,
      mainGridId:String, startViewFieldId:String,endViewFieldID:String,startReferenceParams:Map[String,Seq[String]],endReferenceParams:Map[String,Seq[String]],
      var isInsertAllowed :Boolean= false, var isDeleteAllowed:Boolean= false,var fromReadOnly:Boolean = false,var toReadOnly:Boolean = false) {
    def update(isInsertAllowed :Boolean,  isDeleteAllowed:Boolean, fromReadOnly:Boolean, toReadOnly:Boolean) {

       this.isDeleteAllowed = isDeleteAllowed
       this.isInsertAllowed = isInsertAllowed
       this.fromReadOnly = fromReadOnly
       this.toReadOnly = toReadOnly
    }
  def xmlString:String ={
    new StringBuilder().append("<relation>")
      .append(Model2Xml.tag("id",id))
      .append(Model2Xml.tag("relationType",relationType))
      .append(Model2Xml.tag("isInsertAllowed",isInsertAllowed.toString))
      .append(Model2Xml.tag("isDeleteAllowed",isDeleteAllowed.toString))
      .append(Model2Xml.tag("fromReadOnly",fromReadOnly.toString))
      .append(Model2Xml.tag("toReadOnly",toReadOnly.toString))
      .append("</relation>").toString
  }

}

object MetaDiagramRelation{
  def extractParam(xmlParam:String):Map[String,Seq[String]]={
    if(xmlParam== null) return null
    val xml =  XML.loadString("<root>"+xmlParam + "</root>")
    (xml \\ "param").map(f =>{
      val source = f.attribute("source").head.text
      val attributes = if (source.toLowerCase == "#this") Seq("ID")
        else source.replace("#CURRENT","").replace("#SELECTION","").replace("(","").replace(")","").replace(" ","").split(",").toSeq
      f.attribute("var").head.text -> attributes
    }).toMap
  }
  def apply(resultSet:ResultSet) : MetaDiagramRelation={
    val id = resultSet.getString("ID")
    val viewDiagramId = resultSet.getString("ViewDiagramID")
    val name = resultSet.getString("Name")
    val relationType  = resultSet.getString("RelationTypeID")
    val startPropertyCode = resultSet.getString("StartPropertyCode")
    val endPropertyCode  = resultSet.getString("EndPropertyCode")
    val viewDiagramDetailId = resultSet.getString("ViewDiagramDetailID")
    val mainGridId = resultSet.getString("MainGridID")
    val startViewFieldId = resultSet.getString("StartViewFieldID")
    val endViewFieldId = resultSet.getString("EndViewFieldID")
    val startRefParam = extractParam(resultSet.getString("StartReferenceParams"))
    val endRefParam = extractParam(resultSet.getString("EndReferenceParams"))


    new MetaDiagramRelation (
      id = id,
      viewDiagramId = viewDiagramId,
      name=name,relationType=relationType,
      startPropertyCode=startPropertyCode,
      endPropertyCode=endPropertyCode,
      viewDiagramDetailId=viewDiagramDetailId,
      mainGridId = mainGridId ,
      startViewFieldId =  startViewFieldId,
      endViewFieldID = endViewFieldId,
      startReferenceParams = startRefParam,
      endReferenceParams = endRefParam
    )
  }
}
case class MetaDiagramRelationSeq(data:Seq[MetaDiagramRelation])
