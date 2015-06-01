package omed.forms

import ru.atmed.omed.beans.model.meta._
import ru.atmed.omed.beans.model.meta.ReportFieldDetail
import ru.atmed.omed.beans.model.meta.MetaViewDiagram
import ru.atmed.omed.beans.model.meta.MetaDiagramRelation
import ru.atmed.omed.beans.model.meta.MetaDiagramDetail

/**
 * Класс обеспечивает загрузку метаданных для форм
 */
trait MetaFormDBProvider {
 // def loadReportFieldDetailFromDb:Seq[ReportFieldDetail]
  def loadDiagramDetailFromDb:Seq[MetaDiagramDetail]
  def loadDiagramRelationFromDb:Seq[MetaDiagramRelation]
  def loadViewDiagramFromDb:Seq[MetaViewDiagram]
  def loadMetaGridColumnsFromDB(windowGridIDList:Seq[String]):Seq[MetaGridColumn]
  def loadWindowGridMetaFromDB(windowGridIDList:Seq[String]):Seq[MetaGrid]
  def loadSubClassListFromDB(classList:Seq[String]):Seq[Subclass]
  def loadRelationGridListFromDB(viewCardId: String):Seq[MetaCardGrid]
  /**
   *  Метод позволяет кешировать абстрактный класс, если у последнего определены необходимые методы
   * @param companion
   * @tparam A
   * @return
   */
  def loadMetaData[A <:AnyRef](companion:MetaCreation[A]):Seq[A]
}
