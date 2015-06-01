package ru.atmed.omed.beans.model.meta

import omed.rest.model2xml.Model2Xml

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 10.01.14
 * Time: 18:57
 * To change this template use File | Settings | File Templates.
 */
case class DiagramMeta (
                          captionPropertyCode:String = null,
                          commentPropertyCode:String = null,
                          var detailDiagramGrids : Seq[MetaGrid] =null,
                          metaDiagramRelations:Seq[MetaDiagramRelation] = null){
  def xmlString:String={
    new StringBuilder().append("<diagram>")
      .append(Model2Xml.tag("detailGrids",if(detailDiagramGrids!=null) detailDiagramGrids.map(f=> Model2Xml.tag("detailGrid",f.xmlString)).mkString("\n") else null))
      .append(Model2Xml.tag("relations",if(metaDiagramRelations!=null) metaDiagramRelations.map(f=>f.xmlString).mkString("\n") else null))
      .append(Model2Xml.tag("captionPropertyCode",captionPropertyCode))
      .append(Model2Xml.tag("commentPropertyCode",commentPropertyCode))
      .append("</diagram>").toString
  }

}
