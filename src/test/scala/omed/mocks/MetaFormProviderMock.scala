package omed.mocks

import sun.reflect.generics.reflectiveObjects.NotImplementedException
import omed.forms.MetaFormProvider
import ru.atmed.omed.beans.model.meta._
import omed.forms._
import scala.collection.mutable.HashMap

class MetaFormProviderMock extends MetaFormProvider {

  val cardFieldProperties = new HashMap[String, Seq[StatusFieldRedefinition]]
  val gridFieldProperties = new HashMap[String, Seq[StatusFieldRedefinition]]
  val metaCards = new HashMap[String, MetaCard]
  val metaGrids = new HashMap[String, MetaGrid]

  /**
   * Главное меню
   */
  def getMainMenu(): Seq[AppMenu] =
    throw new NotImplementedException

  /**
   * Главная форма-список
   */
  def getMainGrid(): MetaFormGrid =
    throw new NotImplementedException

  def getMainCard():MetaCard =
    throw new NotImplementedException
  /**
   * Меню
   */
  def getMenu(menuId: String): Seq[ContextMenu] =
    throw new NotImplementedException

  def getWindowGridForClass(classId:String):String=
    throw new NotImplementedException
  /**
   * Метоописание формы-списка
   */
  def getDiagramRelation(viewDiagramId:String):Seq[MetaDiagramRelation] =
    throw new NotImplementedException
  def getDiagramDetail(viewDiagramId:String):Seq[MetaDiagramDetail]  =
    throw new NotImplementedException
  def getMetaRelation(relationId:String):MetaDiagramRelation =
    throw new NotImplementedException
  def getMetaDiagramDetail(detailId:String):MetaDiagramDetail=
    throw new NotImplementedException
  def getViewDiagramId(windowGridId:String):String =
    throw new NotImplementedException
  def getWindowGridMeta(windowGridId: String): MetaGrid =
    throw new NotImplementedException

  def getMetaFormGrid(viewGridId: String): MetaFormGrid =
    throw new NotImplementedException

  def getReportFieldDetail(reportTemplateId:String):Seq[ReportFieldDetail] =
    throw new NotImplementedException
  /**
   * Метоописание формы-карточки
   */
  def getMetaCard(recordId: String,viewCardId:String=null,isSuperUser:Boolean = false): MetaCard =
    if (this.metaCards.contains(recordId))
      this.metaCards(recordId)
    else
      throw new NotImplementedException

  def getStatusFieldRedefinitions(viewCardId: String): Seq[StatusFieldRedefinition] =
    if ((this.cardFieldProperties ++ this.gridFieldProperties).contains(viewCardId))
      (this.cardFieldProperties ++ this.gridFieldProperties)(viewCardId)
    else
      throw new NotImplementedException

//  def getGridFieldProperties(windowGridId: String): Seq[StatusFieldRedefinition] =
//    if (this.gridFieldProperties.contains(windowGridId))
//      this.gridFieldProperties(windowGridId)
//    else
//      throw new NotImplementedException

  def getStatusWindowGrids(): Seq[StatusWindowGrid] = Nil

  def getStatusSections(status:String,viewCardId:String):Seq[StatusSection] = Nil

  def getObjectInCard(recordId:String,objectInCardItemId:String) :String = null

  def getTemplateClass(templateClassId: String, typeId: String) = null

  def getStatusMenuRedefinitions(statusId:String):Seq[StatusMenuRedefinition] = null
}