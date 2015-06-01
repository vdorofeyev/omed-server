package omed.forms

import ru.atmed.omed.beans.model.meta._

trait MetaFormProvider {
  /**
   * Главное меню
   */
  def getMainMenu(): Seq[AppMenu]

  /**
   * Главная форма-список
   */
  def getMainGrid(): MetaFormGrid

  /**
   * главная форма-карточка
   */
  def getMainCard():MetaCard

  /**
   * Меню
   */
  def getMenu(menuId: String): Seq[ContextMenu]

  /**
   * Метаописание формы-списка
   */
  def getMetaFormGrid(viewGridId: String): MetaFormGrid

  /**
   * Метаописание формы-карточки
   */
  def getMetaCard(recordId: String,viewCardId :String = null,isSuperUser:Boolean = false): MetaCard

  def getStatusFieldRedefinitions(viewId:String):Seq[StatusFieldRedefinition]

  //def getCardFieldProperties(viewCardId: String): Seq[CardFieldProperties]

 // def getGridFieldProperties(windowGridId: String): Seq[GridFieldProperties]
 def getWindowGridMeta(windowGridId: String): MetaGrid

  def getStatusWindowGrids(): Seq[StatusWindowGrid]

  def getStatusSections(status:String,viewCardId:String):Seq[StatusSection]

  def getObjectInCard(recordId:String,objectInCardItemId:String) :String

  def getReportFieldDetail(reportTemplateId:String):Seq[ReportFieldDetail]

  def getDiagramRelation(viewDiagramId:String):Seq[MetaDiagramRelation]

  def getMetaRelation(relationId:String):MetaDiagramRelation

  def getDiagramDetail(viewDiagramId:String):Seq[MetaDiagramDetail]

  def getMetaDiagramDetail(detailId:String):MetaDiagramDetail

  def getViewDiagramId(windowGridId:String):String

  def getWindowGridForClass(classId:String):String

  def getStatusMenuRedefinitions(statusId:String):Seq[StatusMenuRedefinition]

  def getTemplateClass(templateClassId:String,typeId:String):TemplateClass
}