package omed.bf

import ru.atmed.omed.beans.model.meta.{Metafield, MetaGrid, MetaCard}
import omed.data.{DataReaderService, DataTable}
import scala.xml.Elem

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 19.09.13
 * Time: 15:18
 * To change this template use File | Settings | File Templates.
 */
trait DataSetBuilder {
  def getDataSetTable(name:String,metaCard:MetaCard, ids:Seq[String],reportTemplateId:String,withMeta:Boolean = true) :Seq[Elem]
 // def getDataSetTable(metaGrid:MetaGrid, data:DataTable,withMeta:Boolean = true) :Elem
  def getDataSetTable(name:String,fields:Seq[Metafield], data:Seq[Map[String,Any]],reportTemplateId:String,withMeta:Boolean) :Seq[Elem]

  def getTreeDataSetTable(fieldTypes:Seq[Metafield],data:String) :Elem
}
