package omed.forms

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 09.01.14
 * Time: 15:28
 * To change this template use File | Settings | File Templates.
 */
object MetaFormQuery {
    val reportFieldDetailQuery =
      """
        |select FieldID,ViewCardID,ReportTemplateID
        |from _Meta_ReportFieldDetail
      """.stripMargin

  val diagramDetailQuery=
    """
      |select VDD.ID,
      |VDD.DetailClassID,
      |MP.code as DetailRelPropertyCode,
      |VDD.IsVisible,
      |VDD.ViewDiagramID,
      |VDD.WindowGridID
      |from  _Meta_ViewDiagramDetail VDD
      |inner join  _Meta_property MP on MP.id = VDD.DetailRelPropertyID
    """.stripMargin

  val diagramRelationQuery =
    """

      |select  MP1.ReferenceParams as StartReferenceParams,MP2.ReferenceParams as EndReferenceParams,MWGC1.ID as StartViewFieldID, MWGC2.ID as EndViewFieldID,VDR.ID,MP1.code as StartPropertyCode, MP2.Code as EndPropertyCode,
      |MVD.MainGridID,VDR.Name,VDR.RelationTypeID,VDR.ViewDiagramID,VDRD.ViewDiagramDetailID
      |from _Meta_ViewDiagramRelation VDR
      |left join _Meta_ViewDiagramRelationByDetail VDRD on VDRD.ID = VDR.ID
      |left join _Meta_ViewDiagramDetail MVDD on MVDD.ID = VDRD.ViewDiagramDetailID
      |inner join _Meta_Property MP1 on MP1.id = VDR.StartPropertyID
      |inner join _Meta_Property MP2 on MP2.id = VDR.EndPropertyID
      |inner join _Meta_ViewDiagram MVD on MVD.ID = VDR.ViewDiagramID
      |inner join _Meta_WindowGridColumn MWGC1 on MWGC1.PropertyID = VDR.StartPropertyID and (MWGC1.WindowGridID = MVD.MainGridID or MWGC1.WindowGridID = MVDD.WindowGridID)
      |inner join _Meta_WindowGridColumn MWGC2 on MWGC2.PropertyID = VDR.EndPropertyID and (MWGC2.WindowGridID = MVD.MainGridID or MWGC2.WindowGridID = MVDD.WindowGridID)
    """.stripMargin
  val viewDiagramQuery =
    """
      |select ID,MainGridID from _Meta_ViewDiagram
    """.stripMargin

  val GridInCardQuery=
    """
      |select WindowGridID,Caption,SortOrder,TabID,ViewCardID from _meta_GridByCard where _Domain = -1 and ViewCardID is not null
    """.stripMargin

  val WindowGridQuery =
    """
      |select  ID from _Meta_WindowGrid where IsDefault = 'Y' and ClassID = ?
    """.stripMargin

  val schedulerGroupQuery =
    """
      |select MSG.Code,MSG.SortOrder,IsVertical,WindowGridID,MSGT.Code as Type,MSG.Name from _Meta_SchedulerGroup MSG
      |join _Meta_SchedulerGroupType MSGT on MSGT.ID = MSG.Type
    """.stripMargin

  val templateClassQuery =
    """
      |select  ID,NewObjectClassID,ClassID,TemplateClassTypeID from _Meta_TemplateClass
    """.stripMargin

  val templatePropertyClassQuery =
    """
      |select ClassPropertyCode,NewObjectClassPropertyCode,TemplateClassID from _Meta_TemplateClassProperty
    """.stripMargin

  val statusMenuQuery =
    """
      |select StatusID,MenuID,IsVisible,IsReadOnly,Caption from _Meta_StatusMenu where StatusID is not null and MenuID is not null
    """.stripMargin
}
