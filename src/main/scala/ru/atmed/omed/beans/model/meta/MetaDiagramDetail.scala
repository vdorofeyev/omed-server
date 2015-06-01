package ru.atmed.omed.beans.model.meta

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 10.01.14
 * Time: 16:39
 * To change this template use File | Settings | File Templates.
 */
case class MetaDiagramDetail(id:String,viewDiagramId:String,detailClassId:String,detailRelPropertyCode:String,windowGridId:String,isVisible:Boolean) {

}
case class MetaDiagramDetailSeq(data:Seq[MetaDiagramDetail])