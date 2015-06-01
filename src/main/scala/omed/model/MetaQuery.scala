package omed.model

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 17.12.13
 * Time: 14:28
 * To change this template use File | Settings | File Templates.
 */
object MetaQuery {
   val getAllClassMetaDataQuery =
     """
       |SELECT c.ID
       |	,c.ParentID
       |	,c.Code
       |  ,c.StorageDomain
       |	,ap.StatusDiagramID
       |	,c.LockTimeout
       |  ,ap.AliasPattern
       |FROM _Meta_Class.DataAll c
       |INNER JOIN _Meta_Class.AllParent ap ON ap.ID = c.ID
       |	AND ap.ParentID = c.ID
       |WHERE c.IsCorrect = 'Y'
       |ORDER BY ap.ClassLevel
       |	,c.Code
     """.stripMargin
  val getAllFieldsQuery =
    """
      |SELECT p.ID
      |	,p.ClassID
      |	,p.Code
      |	,p.Type_Code
      |	,p.Type_TypeCode
      |	,Case when p1.classid=p.classid then p.ArrayName else null end as ArrayName
      |	,p.Type_ExtInfo
      |
      |FROM _Meta_Class.DataAll c
      |INNER JOIN _Meta_Class.AllParent ap ON ap.ID = c.ID
      |	AND ap.ParentID = c.ID
      |CROSS APPLY _Meta_Property.EnumAllByClass(c.ID) p
      |inner join _Meta_property p1 on p1.id=p.ID
      |WHERE c.IsCorrect = 'Y' and p1.IsCorrect = 'Y'
      |ORDER BY ap.ClassLevel
      |	,c.Code
      |	,p.Code
    """.stripMargin
  val getAllStatusQuery =
    """
      |SELECT st.ID
      |	,st.StatusDiagramID
      |	,st.NAME
      |	,st.IsNew
      |	,st.BusinessFunctionFromID
      |	,st.BusinessFunctionToID
      |	,st.ConditionFrom
      |	,st.ConditionTo
      |	,st.DefaultTabID
      |	,st.IsEditNotAllowed
      |	,st.IsDeleteNotAllowed
      |
      |FROM _Meta_Status.DataAll st
      |WHERE st._Domain = - 1
    """.stripMargin

  val getAllTransitionQuery =
    """
      |SELECT t.ID
      |	,t.StatusDiagramID
      |	,t.BeginStatusID
      |	,t.EndStatusID
      |	,t.Condition
      |	,t.ModuleID
      |	,c.ID as ClassID
      |
      |FROM  _Meta_Class c
      |inner join _Meta_Transition t on c.StatusDiagramID = t.StatusDiagramID
      |WHERE c.IsCorrect = 'Y' and t._Domain = - 1 and t.StatusDiagramID is not null and t.BeginStatusID is not null and t.EndStatusID is not null
      |ORDER BY t.StatusDiagramID
    """.stripMargin
  val getPredNotificationDescriptionsQuery =
    """
      |Select pn.ID,
      |	pn.NotificationGroupID,
      |	pn.StatusID,
      |	pn.MaxProcessingTime
      |from _Meta_PredNotificationDescription pn
      |where pn._Domain= - 1 and
      |	pn.StatusID is not null and
      |	(pn.ModuleID is null or pn.ModuleID in (
      |		SELECT dm.ModuleID
      |		FROM _Domain.Data d
      |		INNER JOIN _DomainModule.DataAll dm ON dm.DomainID = d.ID
      |		WHERE d.Number = ?)
      |	)
    """.stripMargin

  val getModuleInDomainQuery =
    """
      |select D.Number,ModuleID from _DomainModule  DM
      |inner join _Domain D on D.ID = DM.DomainID
      | where DM.DomainID is not null and DM.ModuleID is not null
    """.stripMargin

  val getArrayNameQuery =
    """
      |select
      |     c1.Code as ParentClassCode
      |    , cgp.PropertyCode as ParentPropertyCode
      |    , cgp.ParameterCode as RelationGridPropertyCode
      |    , c2.Code as RelationClassCode
      |    , cgp.ArrayName
      |from _Meta_ViewCardGridParameter cgp
      |join _Meta_ViewCardGrid cg on cg.id = cgp.ViewCardGridID
      |join _Meta_WindowGrid.data g on g.id = cg.WindowGridID
      |join _Meta_ClassView cv on cv.viewid = cg.ViewCardID
      |join _Meta_Class c1 on c1.ID = cv.ClassID
      |join _Meta_Class c2 on c2.ID = g.ClassID
    """.stripMargin
  val stringColorationQuery =
    """
      |    select
      |        Name,
      |        Condition,
      |        ConditionString,
      |        FalseMessage,
      |        ClassID,
      |        Color,
      |        Color2,
      |        Priority
      |    from _Meta_RecordColoration.DataAll
      |    where ClassID is not null
      |    -- Строго окраска строк, без потомков
      |    and _ClassID = _Meta_RecordColoration.ClassID()
    """.stripMargin

  val fieldColorationQuery =
    """
      |       select
      |        cc.Name,         -- Наименование валидатора для окрашивания строк
      |        cc.Condition,    -- Условие окрашивания
      |        cc.ConditionString,
      |        cc.FalseMessage, --    Сообщение пользователю
      |        cc.ClassID,      -- Класс
      |        cc.Color,        -- Цвет
      |        cc.Priority,     -- Приоритет
      |        cc.PropertyID,     -- Атрибут
      |        p.Code as Property_Code
      |    from _Meta_CellColoration.DataAll cc
      |    inner join _Meta_Property.Data p
      |        on p.ID = cc.PropertyID
      |    where cc.ClassID is not null
      |    -- Строго окраска строк, без потомков
      |    and cc._ClassID = _Meta_CellColoration.ClassID()
    """.stripMargin
}
