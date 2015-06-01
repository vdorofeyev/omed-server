package omed.forms

import omed.db._
import com.google.inject.Inject
import ru.atmed.omed.beans.model.meta._
import omed.system.ContextProvider
import scala.collection.mutable.ArrayBuffer
import omed.auth.{ AuthError, PermissionType, PermissionProvider }
import omed.cache.{ExecStatProvider, DomainCacheService}
import omed.data.{EntityFactory, DataWriterService}
import omed.model.services.ExpressionEvaluator
import omed.bf.ConfigurationProvider
import com.hazelcast.core.Hazelcast
import omed.model.SimpleValue
import ru.atmed.omed.beans.model.meta.StatusWindowGrid
import ru.atmed.omed.beans.model.meta.CardInCardItemSeq
import ru.atmed.omed.beans.model.meta.MetaGridSeq
import ru.atmed.omed.beans.model.meta.FieldSection
import ru.atmed.omed.beans.model.meta.MetaGridColumn
import ru.atmed.omed.beans.model.meta.AppMenu
import ru.atmed.omed.beans.model.meta.MetaGridColumnSeq
import ru.atmed.omed.beans.model.meta.FieldGroup
import ru.atmed.omed.beans.model.meta.AppMenuSeq
import ru.atmed.omed.beans.model.meta.ContextMenu

import ru.atmed.omed.beans.model.meta.MetaCardField
import ru.atmed.omed.beans.model.meta.MetaCardFieldsAndGroups
import ru.atmed.omed.beans.model.meta.CardInCardItem
import ru.atmed.omed.beans.model.meta.MetaCard
import ru.atmed.omed.beans.model.meta.MetafieldImpl
import ru.atmed.omed.beans.model.meta.Subclass
import ru.atmed.omed.beans.model.meta.ContextMenuSeq
import omed.errors.{MetaModelError, DataError}
import java.sql.ResultSet
import omed.lang.eval.DBUtils

/**
 * Данные о параметрах полей карточек в статусах
 * @param id ИД переопределения
 * @param statusID ИД статуса
 * @param viewFieldID ИД поля на форме

 */


case class StatusFieldRedefinition(
   id: String,
   statusId: String,
   viewFieldId: String,
   viewId:String,
   viewFieldCode: String,
   propertyId: String,
   redefinitions:Map[String,Any]
)
case class StatusFieldRedefinitionSeq(data:Seq[StatusFieldRedefinition])

//case class CardFieldProperties(
//  id: String,
//  statusID: String,
//  viewFieldID: String,
//  caption: String,
//  editorType: String,
//  sortOrder: Int,
//  isVisible: Option[Boolean],
//  isReadOnly: Option[Boolean],
//  format: String,
//  isMasked: Option[Boolean],
//  isDropDownNotAllowed: Option[Boolean],
//  propertyID: String,
//  tabID: String,
//  tabCaption: String,
//  tabSortOrder: String,
//  viewCardID: String,
//  viewFieldCode: String)
//
//case class CardFieldPropertiesSeq(data: Seq[CardFieldProperties])
//
///**
// * Данные о параметрах полей карточек в статусах
// * @param id ИД переопределения
// * @param statusID ИД статуса
// * @param viewFieldID ИД поля на форме
// * @param editorType Тип контрола
// * @param isVisible Видимость
// * @param isReadOnly Только чтение
// * @param format Формат поля
// * @param isMasked Признак "Маскируется звездочками при отображении"
// * @param isDropDownNotAllowed Признак "Запретить выпадение списка"
// * @param propertyID ИД атрибута, к которому относится поле
// */
//case class GridFieldProperties(
//  id: String,
//  statusID: String,
//  viewFieldID: String,
//  editorType: String,
//  isVisible: Option[Boolean],
//  isReadOnly: Option[Boolean],
//  format: String,
//  isMasked: Option[Boolean],
//  isDropDownNotAllowed: Option[Boolean],
//  propertyID: String,
//  fieldCode: String,
//  windowGridID: String)
//
//case class GridFieldPropertiesSeq(data: Seq[GridFieldProperties])

class MetaFormProviderImpl extends MetaFormProvider with DataAccessSupport {

  @Inject
  var connectionProvider: ConnectionProvider = null
  @Inject
  var contextProvider: ContextProvider = null
  @Inject
  var domainCacheService: DomainCacheService = null
  @Inject
  var permissionProvider: PermissionProvider = null
  @Inject
  var dataWriterService: DataWriterService = null
  @Inject
  var expressionEvaluator:ExpressionEvaluator = null
  @Inject
  var configProvider:ConfigurationProvider = null

  @Inject
  var execStatProvider: ExecStatProvider  =null

  @Inject
  var metaFormDBprovider:MetaFormDBProvider = null
  @Inject
  var entityFactory:EntityFactory = null
  /**
   * Главное меню
   */
  val classWraperMap = Map(
    "SchedulerGroup"-> classOf[SchedulerGroupSeq],
    "GridInCardItem"-> classOf[GridInCardItemSeq]
  )
  def getString(rs:ResultSet,columnName:String):String={
    try{
      rs.getString(columnName)
    }
    catch {
      case _ => null
    }
  }
  def getMainMenu(): Seq[AppMenu] = {
    val domainStr = this.contextProvider.getContext.domainId.toString

    val cached = domainCacheService.get(classOf[AppMenuSeq], domainStr)
    val menuItems = if (cached == null) {
      val result =
        connectionProvider.withConnection {
          connection =>
            val dbResult = DB.dbExec(connection, "[_Meta].[GetMainMenu]",
              contextProvider.getContext.sessionId, null,execStatProvider = execStatProvider)

            val menuList = scala.collection.mutable.Buffer[AppMenu]()
            while (dbResult.next()) {
              menuList += AppMenu(
                id = Option(dbResult.getObject("ID")).map(x => x.toString).orNull,
                menuLevel = 0,
                name = dbResult.getString("Name"),
                parentId = Option(dbResult.getObject("ParentMenuID")).map(x => x.toString).orNull,
                openViewId = Option(dbResult.getObject("OpenViewGridID")).map(x => x.toString).orNull,
                businessFunctionId = Option(dbResult.getObject("BusinessFunctionID")).map(x => x.toString).orNull,
                glyph = Option(dbResult.getBytes("Glyph")).orNull)
            }

            // добавить корневой элемент в меню
            val menu = Seq(AppMenu(
              id = "DE43E491-FF3A-4CAF-A37C-8436AF4110AF",
              menuLevel = -1,
              name = "Главное меню",
              parentId = null,
              openViewId = null,
              businessFunctionId = null,
              glyph = null)) ++ menuList

            menu
        }
      domainCacheService.put(classOf[AppMenuSeq], domainStr, AppMenuSeq(result))
      result
    } else cached.data

    // filter items by "read" access rights for item or
    // "read/write" access rights for "openViewId" grid form
    val result = menuItems.filter(item =>
      item.menuLevel == -1 || permissionProvider.getMetaPermission(item.id)(PermissionType.ReadExec))
    result
  }

  /**
   * Главная форма-список
   */
  def getMainGrid(): MetaFormGrid = {
    val result =
      connectionProvider.withConnection {
        connection =>
          val dbResult = DB.dbExec(connection, "[_Meta].[GetMainViewGrid]",
            contextProvider.getContext.sessionId, List(("RoleID",  contextProvider.getContext.roleId)),execStatProvider = execStatProvider)

          val mainViewGridId = if (dbResult.next())
            Option(dbResult.getObject("ViewID")).map(x => x.toString).orNull
          else
            throw new Exception("Не найдена главная форма-список.")
          // сформировать объектную модель для формы-списка
          val mainMetaGrid = this.getMetaFormGrid(mainViewGridId)

          mainMetaGrid
      }
    result
  }

  /**
   * главная форма-карточка
   */
  def getMainCard():MetaCard={
     val role =  permissionProvider.getCurrentRole
     val objectId = expressionEvaluator.evaluate(role.openObjExp,configProvider.create(),contextProvider.getContext.getSystemVariables)
     if(objectId!=null) getMetaCard(objectId.getId)
     else null
  }

  /**
   * Меню
   */
  def getMenu(menuId: String): Seq[ContextMenu] = {
    val domainStr = this.contextProvider.getContext.domainId.toString
    val key = domainStr + ":" + menuId

    val cached = domainCacheService.get(classOf[ContextMenuSeq], key)
    val menu = if (cached == null) {
      val result = connectionProvider.withConnection {
        connection =>
          val dbResult = DB.dbExec(connection, "[_Meta].[GetMenu]",
            contextProvider.getContext.sessionId,
            List(("MenuID", menuId)),execStatProvider = execStatProvider)

          // реализация логики метода
          var menuList = scala.collection.mutable.Buffer[ContextMenu]()
          while (dbResult.next()) {

            menuList += new ContextMenu(
              id = Option(dbResult.getObject("MenuID")).map(x => x.toString).orNull,
              name = dbResult.getString("Name"),
              parentId = Option(dbResult.getObject("ParentMenuID")).map(x => x.toString).orNull,
              method = dbResult.getString("MethodCode"),
              methodTypeCharCode = dbResult.getString("MethodTypeCharCode"),
              isPublicOnFormGrid = true,
              isPublicOnFormCard = true,
              isConfirmation = DBUtils.fromDbBoolean(dbResult.getString("IsConfirmation")),
              isRefresh = DBUtils.fromDbBoolean(dbResult.getString("IsRefresh")),
              message = dbResult.getString("Msg"),
              shortcut = dbResult.getString("Shortcut"),
              glyph = null,
              businessFuncId = Option(dbResult.getObject("BusinessFunctionID")).map(x => x.toString).orNull,
              alignment = Option(dbResult.getObject("Alignment")).map(x => x.toString).orNull,
              buttonPosition = Option(dbResult.getObject("ButtonPosition")).map(x => x.toString).orNull,
              sectionId =  Option(dbResult.getObject("SectionID")).map(x => x.toString).orNull,
              row = dbResult.getShort("Row"),
              sortOrder = dbResult.getInt("SortOrder"),
              buttonGroupId =  Option(dbResult.getObject("ButtonGroupID")).map(x => x.toString).orNull
            )
          }
          menuList.toSeq
      }

      domainCacheService.put(classOf[ContextMenuSeq], key, ContextMenuSeq(result))
      result
    } else cached.data

    val filteredMenu = menu.filter(m => {
      val menuPerm = permissionProvider.getMetaPermission(m.id)(PermissionType.ReadExec)
      val funcPerm = permissionProvider.getMetaPermission(m.businessFuncId)(PermissionType.ReadExec)
      menuPerm || funcPerm
    })
    filteredMenu
  }



  /**
   * Метаописание формы-списка. Права на метаданные больше не проверяются а доступ регулируются правами на данные
   */
  def getMetaFormGrid(viewGridId: String): MetaFormGrid = {
    val grid = connectionProvider.withConnection {
      connection =>
        val dbResult = DB.dbExec(connection, "[_Meta].[GetViewGridMetadata]",
          contextProvider.getContext.sessionId,
          List(("ViewGridID", viewGridId)),execStatProvider = execStatProvider)

        // получение базовых полей формы-списка
        dbResult.next()

        val methodMenuId = Option(dbResult.getObject("MethodMenuID")).map(x => x.toString).orNull
        val glyph = Option(dbResult.getBytes("Glyph")).orNull
        val caption = Option(dbResult.getString("Caption")).orNull
        val treeId = Option(dbResult.getObject("TreeID")).map(x => x.toString).orNull
        val isInformPanelVisible = DBUtils.fromDbBoolean(dbResult.getString("IsInformPanelVisible"))
        val isTreeVisible = DBUtils.fromDbBoolean(dbResult.getString("IsTreeVisible"))
        val isVisibleAuxiliaryGrid = DBUtils.fromDbBoolean(dbResult.getString("IsVisibleAuxiliaryGrid"))
        val menuId = Option(dbResult.getObject("MenuID")).map(x => x.toString).orNull
        val windowGridId = Option(dbResult.getObject("WindowGridID")).map(x => x.toString).orNull
        val isSchedulerView =DBUtils.fromDbBoolean(dbResult.getString("IsSchedulerView"))
        val isSearchView =  DBUtils.fromDbBoolean(dbResult.getString("IsSearchView"))
        val captionPropertyCode = getString(dbResult,"CaptionPropertyCode") //dbResult.getString("CaptionPropertyCode")
        val commentPropertyCode =  getString(dbResult,"CommentPropertyCode")//dbResult.getString("CommentPropertyCode")

        val diagramDetailGrids =
          if(captionPropertyCode == null) null
         else getDiagramDetail(viewGridId).map(f => {
            val meta = getWindowGridMeta(f.windowGridId)
            meta.relationPropertyCode = f.detailRelPropertyCode
            meta
          } )
        val relations =
          if(captionPropertyCode == null) null
          else getDiagramRelation(viewGridId)

        // получение данных для грида
        val windowGridMetaData = getWindowGridMeta(windowGridId)

        // получение контекстного меню
        val contextMenu = if (methodMenuId != null) this.getMenu(methodMenuId) else null
        windowGridMetaData.contextMenu = contextMenu
        // получение меню
        val menu = if (menuId != null) this.getMenu(menuId) else null

        MetaFormGrid (
          viewGridId = viewGridId,
          isTreeVisible = isTreeVisible,
          isVisibleAuxiliaryGrid = isVisibleAuxiliaryGrid,
          treeId = treeId,
          isSearchView = isSearchView ,
          diagramMeta = DiagramMeta(captionPropertyCode = captionPropertyCode,commentPropertyCode = commentPropertyCode,detailDiagramGrids = diagramDetailGrids,metaDiagramRelations = relations),
          menu = menu,
          caption = caption,
          mainGrid  = windowGridMetaData)
    }
    grid
  }

  /**
   * Метаописание формы-карточки. Права на метаданные больше не проверяются а доступ регулируются правами на данные
   */
  def getMetaCard(recordId: String,viewCardIdparameter:String = null,isSuperUser:Boolean = false): MetaCard = {
    val obj = entityFactory.createEntity(recordId)
    dataWriterService.lockObject(obj)
    val card = connectionProvider.withConnection {
      connection =>
        val dbResult = DB.dbExec(connection,  "[_Meta].[GetViewCardMetadata]",
          contextProvider.getContext.sessionId,
          List("RecordID"-> recordId,"ViewCardID"->viewCardIdparameter),execStatProvider = execStatProvider)

        if(!dbResult.next()) throw new DataError("Не найдены метаданные формы-карточки recordId = " + recordId + " viewCardID = " + viewCardIdparameter )

        // получение базовых полей формы-карточки
        val methodMenuId = Option(dbResult.getObject("MethodMenuID")).map(x => x.toString).orNull
        val glyph = Option(dbResult.getBytes("Glyph")).orNull
        val caption = Option(dbResult.getString("Caption")).orNull
        val width = Option(dbResult.getInt("Width")).getOrElse(0)
        val height = Option(dbResult.getInt("Height")).getOrElse(0)
        val fieldsPanelHeight = Option(dbResult.getInt("FieldsPanelHeight")).getOrElse(0)
        val isReadOnly = DBUtils.fromDbBoolean(dbResult.getString("IsReadOnly"))
        val viewCardId = Option(dbResult.getObject("ViewCardID")).map(x => x.toString).orNull
        val isVisibleAlias =   DBUtils.fromDbBoolean(dbResult.getString("IsVisibleAlias"))
        // получить контекстное меню формы-карточки
        val contextMenu = if (methodMenuId != null) this.getMenu(methodMenuId) else null

        // получить поля
        val (formFields, fieldGroups,fieldSections) =  if (viewCardId != null) this.getMetaCardFields(viewCardId) else null

        // получение связанных гридов
        val refGrids =   this.getRelationGrids(viewCardId,isSuperUser)

        val cardsInCard =  this.getCardsInCard(viewCardId)
        val gridsInCard = getSeqMetaData(viewCardId,GridInCardItem) //this.getGridsInCard(viewCardId)


        val mc = MetaCard(classId = null,
          viewCardId = viewCardId,
          caption = caption,
          glyph = glyph,
          width = width,
          height = height,
          fieldsPanelHeight = fieldsPanelHeight,
          isReadOnly = isReadOnly,
          contextMenu = contextMenu,
          fields = formFields,
          groups = fieldGroups,
          sections = fieldSections,
          refGrids = refGrids,
          isVisibleAlias = isVisibleAlias,
          cardsInCard = cardsInCard,
          gridsInCard = gridsInCard)
        mc
    }
    card

  }

  def getStatusWindowGrids(): Seq[StatusWindowGrid] = {
    val key = "allStatusWindowGrid"

    val cachedData = domainCacheService.get(classOf[StatusWindowGridSeq], key)
    if(cachedData!=null) cachedData.data
    else {
      val lock = Hazelcast.getLock("[ru.atmed.omed.beans.model.meta.StatusWindowGridSeq")
      lock.lock()

      try {
         DBProfiler.profile("cache GetStatusWindowGrid",execStatProvider,true){
           val result = connectionProvider.withConnection {
            connection =>
              val dbResult = DB.dbExec(connection, "[_Meta].[GetStatusWindowGrid]",
                contextProvider.getContext.sessionId,
                null,execStatProvider = execStatProvider)

              var gridList = new ArrayBuffer[StatusWindowGrid]
              while (dbResult.next()) {
                val id = dbResult.getString("ID")
                val statusId = dbResult.getString("StatusID")
                val windowGridId = dbResult.getString("WindowGridID")
                val isVisible = DBUtils.fromDbBoolean(dbResult.getString("IsVisible"))
                val isDeleteAllowed = DBUtils.fromDbBoolean(dbResult.getString("IsDeleteAllowed"))
                val isInsertedAllowed = DBUtils.fromDbBoolean(dbResult.getString("IsInsertAllowed"))
                val isEditAllowed = DBUtils.fromDbBoolean(dbResult.getString("IsEditAllowed"))

                gridList += StatusWindowGrid(id, statusId, windowGridId,
                  isVisible, isDeleteAllowed, isInsertedAllowed, isEditAllowed)
              }

              gridList.toSeq
           }
           domainCacheService.put(classOf[StatusWindowGridSeq],key,StatusWindowGridSeq(result))
           result
         }
      } finally {
        lock.unlock()
      }
    }

  }

  def getStatusSections(status:String,viewCardId:String):Seq[StatusSection]={
    if(status==null) Seq()
    else if(domainCacheService.isEmpty(classOf[StatusSectionSeq])) loadStatusSectionsFromDB
     Option(domainCacheService.get(classOf[StatusSectionSeq],viewCardId+status)).map(f =>f.data).getOrElse(Seq())
  }

  private def loadStatusSectionsFromDB{
    DBProfiler.profile("cache GetStatusSection",execStatProvider,true){
      connectionProvider.withConnection {
        connection =>
          val dbResult = DB.dbExec(connection, "[_Meta].[GetStatusSection]",
            contextProvider.getContext.sessionId,
            List(),execStatProvider = execStatProvider)

          val statusArray = new ArrayBuffer[StatusSection]
          while(dbResult.next() ){
            val sectionId= dbResult.getString("SectionID")
            val viewCardId= dbResult.getString("ViewCardID")
            val statusId = dbResult.getString("StatusID")
            if(statusId!=null && viewCardId!=null && sectionId!=null)
               statusArray+= StatusSection(sectionId = sectionId,caption = dbResult.getString("Caption"),groupId = dbResult.getString("TabID"),
                 sortOrder = Option(dbResult.getObject("SortOrder")).asInstanceOf[Option[Int]],statusId = statusId,viewCardId =viewCardId)

          }
          statusArray.groupBy(f => f.viewCardId + f.statusId ).foreach( f=> domainCacheService.put(classOf[StatusSectionSeq],f._1,StatusSectionSeq(f._2.toSeq)))
      }
   }
  }
    // -------- private methods -------------

  /**
   * Получение метаописание данных грида
   */
  private def getWindowGridMeta(windowGridList:Seq[String]): Map[String,MetaGrid] = {
      val toLoadGrids=windowGridList.filter(p => !domainCacheService.map(classOf[MetaGrid]).contains(p))
      if(toLoadGrids.length>0) {
         val grids = metaFormDBprovider.loadWindowGridMetaFromDB(toLoadGrids)
         val columns = grids.map(f => f.windowGridId -> Seq()).toMap ++  metaFormDBprovider.loadMetaGridColumnsFromDB(toLoadGrids).groupBy(f => f.windowGridId)
         val schedulerGroups = grids.map(f => f.windowGridId -> getSeqMetaData(f.windowGridId,SchedulerGroup)).toMap
         val subClasses = metaFormDBprovider.loadSubClassListFromDB(grids.map(f => f.classId)).groupBy(f => f.rootClassId)
         grids.foreach(f => domainCacheService.put(classOf[MetaGrid],f.windowGridId,f.copy(fields = columns(f.windowGridId),subClassList = subClasses(f.classId),schedulerGroups = schedulerGroups(f.windowGridId) )))
      }
      windowGridList.map(f => f->domainCacheService.get(classOf[MetaGrid],f)).toMap
  }
   def getWindowGridMeta(windowGridId: String): MetaGrid = {
      getWindowGridMeta(Seq(windowGridId))(windowGridId)
  }

  /**                                                                                                                                                r
   * Получение метаописания полей формы-карточки
   */
  private def getMetaCardFields(viewCardId: String): (List[MetaCardField], List[FieldGroup],List[FieldSection]) = {
    val key = viewCardId

    val cachedData = domainCacheService.get(classOf[MetaCardFieldsAndGroups], key)

    if (cachedData == null) {
      val (resFields, resGroups,resSections) = connectionProvider.withConnection {
        connection =>
          val dbResult = DB.dbExec(connection, "[_Meta].[GetViewCardFieldMetadata]",
            contextProvider.getContext.sessionId,
            List(("ViewCardID", viewCardId)),execStatProvider = execStatProvider)

          // for fast search for group by id
          val fieldGroupSet = new scala.collection.mutable.HashSet[String]
          val fieldSectionSet = new scala.collection.mutable.HashSet[String]
          val fieldGroups = new ArrayBuffer[FieldGroup]
          var fieldsList = new ArrayBuffer[MetaCardField]
          val fieldSections = new ArrayBuffer[FieldSection]

          while (dbResult.next()) {
            // read group info from field meta
            val fieldGroupId = Option(dbResult.getString("TabID")).orNull
            val fieldGroupName = Option(dbResult.getString("TabCaption")).orNull
            val fieldGroupSortOrder = Option(dbResult.getInt("TabSortOrder")).getOrElse(0)
            val fieldSectionId = Option(dbResult.getString("SectionID")).orNull
            val fieldSectionParentId = Option(dbResult.getString("SectionParentID")).orNull
            val fieldSectionTabId = Option(dbResult.getString("SectionTabID")).orNull
            val fieldSectionName = Option(dbResult.getString("SectionCaption")).orNull
            val fieldSectionSortOrder = Option(dbResult.getInt("SectionSortOrder")).getOrElse(0)
            // if fieldGroup defined for current field
            if (fieldGroupId != null &&
              // fieldGroup not in collection yet
              !(fieldGroupSet contains fieldGroupId)) {
              fieldGroups += new FieldGroup(fieldGroupId, fieldGroupName, fieldGroupSortOrder)
              fieldGroupSet += fieldGroupId
            }

             //if fieldSection not in collection and defined
            if(fieldSectionId!= null && !(fieldSectionSet contains fieldSectionId)){
              fieldSections+= new FieldSection(fieldSectionId,fieldSectionName,fieldSectionSortOrder,fieldSectionTabId,fieldSectionParentId)
              fieldSectionSet += fieldSectionId
            }
            // read field info
            fieldsList += MetaCardField(
              metafield = MetafieldImpl(
                viewFieldId = dbResult.getString("ViewFieldID"),
                codeName = dbResult.getString("Code"),
                editorType = dbResult.getString("EditorType"),
                typeCode = dbResult.getString("Type_TypeCode"),
                typeExtInfo = dbResult.getString("Type_ExtInfo")
              ),
              caption = dbResult.getString("Caption"),
              sortOrder = Option(dbResult.getInt("SortOrder")).getOrElse(0),
              isReadOnly = DBUtils.fromDbBoolean(dbResult.getString("IsReadOnly")),
              format = dbResult.getString("Format"),
              isDropDownNotAllowed = DBUtils.fromDbBoolean(dbResult.getString("IsDropDownNotAllowed")),
              isMasked = DBUtils.fromDbBoolean(dbResult.getString("IsMasked")),
              isVisible = DBUtils.fromDbBoolean(dbResult.getString("IsVisible")),
              height = Option(dbResult.getInt("Height")).getOrElse(0),
              width = Option(dbResult.getInt("Width")).getOrElse(0),
              groupId = fieldGroupId,
              sectionId = fieldSectionId,
              defaultFormGridId = dbResult.getString("DefaultFormGridID"),
              extInfo = dbResult.getString("ExtInfo"),
              isRequired = DBUtils.fromDbBoolean(dbResult.getString("IsRequired")),
              refParams = dbResult.getString("ReferenceParams"),
              mask = dbResult.getString("Mask"),
              isJoinMask = DBUtils.fromDbBoolean(dbResult.getString("IsJoinMask")),
              captionStyle = dbResult.getString("CaptionStyle"),
              isJoined = DBUtils.fromDbBoolean(dbResult.getString("isJoinPrev")),
              isRefreshOnChange = DBUtils.fromDbBoolean(dbResult.getString("IsRefreshOnChange"))
            )
          }

          (fieldsList.toList,fieldGroups.toList,fieldSections.toList)
      }

      domainCacheService.put(classOf[MetaCardFieldsAndGroups],
        key, MetaCardFieldsAndGroups(resFields, resGroups,resSections))
      (resFields,resGroups,resSections)
    } else (cachedData.fields, cachedData.groups,cachedData.sections)
  }

  /**
   * Получение метаописание связанных с формой гридов
   */
  private def getRelationGrids(viewCardId: String, isSuperUser: Boolean = false): Seq[MetaGrid] = {
    val key = "relationGrids:" + viewCardId
    val cachedData = domainCacheService.get(classOf[MetaGridSeq], key)
    lazy val result ={
      val gridCardMeta = metaFormDBprovider.loadRelationGridListFromDB(viewCardId)
      val grids = getWindowGridMeta(gridCardMeta.map(f => f.windowGridId))
      gridCardMeta.foreach(f =>{
        val contextMenu = if (f.contextMenuId != null)
          this.getMenu(f.contextMenuId)
        else null
        grids(f.windowGridId).contextMenu = contextMenu
        grids(f.windowGridId).metaCardGrid = f
      } )
      grids.map(f => f._2).toSeq.sortBy(f => f.metaCardGrid.sortOrder)
    }
    val gridList = if (cachedData == null) {
      domainCacheService.put(classOf[MetaGridSeq], key, MetaGridSeq(result))
      result
    } else cachedData.data

    val permissionsOfAllGrids = gridList.map(grid => {
      val gridPermissions = permissionProvider.getMetaPermission(grid.windowGridId,isSuperUser)
      (grid -> gridPermissions)
    }).toMap

    gridList.filterNot(grid => {
      // показываем только те гриды, на которые есть права
      val gridPermissions = permissionsOfAllGrids(grid)
      gridPermissions.filter(_._2).isEmpty
    }).map(grid => {
      // накладываем права на связанный грид
      // получаем права на грид
      val gridPermissions = permissionsOfAllGrids(grid)
      val gridReadPerm = gridPermissions.get(PermissionType.ReadExec)
      val gridWritePerm = gridPermissions.get(PermissionType.Write)

      // переопределяем свойства полей грида в зависимости от прав
      val filteredFields = grid.fields.map(f => {
        val fieldPerm = permissionProvider.getOptionMetaPermission(f.getViewFieldId,isSuperUser)

        val fieldReadPerm = fieldPerm(PermissionType.ReadExec)
        val fieldWritePerm = fieldPerm(PermissionType.Write)

        // поля грида наследут права на грид
        val visible = gridReadPerm.getOrElse(true) &&
          fieldReadPerm.getOrElse(true) &&
          (gridReadPerm.isDefined || fieldReadPerm.isDefined)
        val editable = gridWritePerm.getOrElse(true) &&
          fieldWritePerm.getOrElse(true) &&
          (gridWritePerm.isDefined || fieldWritePerm.isDefined)

        f.copy(isVisible = (visible && f.isVisible), isReadOnly = (!editable || f.isReadOnly))
      })
      // переопределяем свойства таблицы в зависимости от прав
      val modifiedGrid = if (!gridPermissions.contains(PermissionType.Write)) {
        grid.copy(
          fields = filteredFields,
          isDeleteAllowed = false,
          isInsertAllowed = false,
          isEditAllowed = false )
      } else {
        grid.copy(fields = filteredFields)
      }
      modifiedGrid
    })
  }


  /**
   *  получение данных о вставке карточек в другие карточки

   */

  private def getCardsInCard(viewCardId:String): Seq[CardInCardItem] = {
    loadObjectsInCard
    Option(domainCacheService.get(classOf[CardInCardItemSeq],viewCardId)).map(f=>f.data).getOrElse(null)
  }

  def getObjectInCard(recordId:String,objectInCardItemId:String) :String ={
    loadObjectsInCard
    val key = objectInCardItemId
    val objectInCardItem = domainCacheService.get(classOf[CardInCardItem], key)
    if(objectInCardItem==null) return null
    Option(expressionEvaluator.evaluate(objectInCardItem.filter,configProvider.create(),Map("this"->SimpleValue(recordId)))).map(f => f.getId).getOrElse(null)
  }

  private def loadObjectsInCard{
    if (domainCacheService.isEmpty(classOf[CardInCardItemSeq])) {
      val lock = Hazelcast.getLock("ru.atmed.omed.beans.model.meta.CardInCardItem")
      lock.lock()
      try{
        val result = connectionProvider.withConnection {
          connection =>
            val dbResult = DB.dbExec(connection, "[_Meta].[GetObjectsByCard]",
              contextProvider.getContext.sessionId,
              List(),execStatProvider = execStatProvider)

            // реализация логики метода
            var cardsInCardList = scala.collection.mutable.Buffer[CardInCardItem]()
            while (dbResult.next()) {
              cardsInCardList += new CardInCardItem(
                id = dbResult.getString("id"),
                viewCardId =   dbResult.getString("ViewCardId"),
                caption = dbResult.getString("Caption"),
                sortOrder =  dbResult.getInt("SortOrder"),
                groupId =  dbResult.getString("TabID"),
                insertViewCardId = dbResult.getString("ObjectViewCardID"),
                gridId = dbResult.getString("WindowGridID"),
                filter = dbResult.getString("Filter")
              )
            }
            cardsInCardList.toSeq
        }
        result.filter(f => f.viewCardId !=null).groupBy(f=>f.viewCardId).foreach(p=> domainCacheService.put(classOf[CardInCardItemSeq],p._1,new CardInCardItemSeq(p._2)) )
        result.foreach(f=>domainCacheService.put(classOf[CardInCardItem],f.id,f))
      }
      finally {
        lock.unlock()
      }
    }
  }

  def getStatusFieldRedefinitions(viewId:String):Seq[StatusFieldRedefinition] ={
    val cachedData = domainCacheService.get(classOf[StatusFieldRedefinitionSeq], viewId)
    if (cachedData == null) {
      if (domainCacheService.isEmpty(classOf[StatusFieldRedefinitionSeq])) {
        // fetch all properties from db
        val storedData = DBProfiler.profile("cache CardField properties in status",execStatProvider,true){ getCardFieldPropertiesFromStorage()  ++ getGridFieldPropertiesFromStorage()}
        // split all properties into groups by recordId
        val groupedProperties = storedData.groupBy(_.viewId)
        groupedProperties.foreach(g => g match {
          case (k, v) => domainCacheService.put(
            classOf[StatusFieldRedefinitionSeq],
            k, StatusFieldRedefinitionSeq(v))
        })
      }
      val result = domainCacheService.get(classOf[StatusFieldRedefinitionSeq], viewId)
      Option(result).map(_.data).getOrElse(Seq())
    } else cachedData.data
  }

  /**
   * Получение данных о параметрах полей формы-карточки
   */
  private def getCardFieldPropertiesFromStorage(): Seq[StatusFieldRedefinition] = {
    connectionProvider.withConnection {
      connection =>
        dataOperation("Получение параметров полей карточки из хранилища данных") {
          val rs = DB.dbExec(connection,
            "[_Meta].[GetStatusViewFieldCard]",
            contextProvider.getContext.sessionId,
            Nil,execStatProvider = execStatProvider)

          val fieldList = scala.collection.mutable.Buffer[StatusFieldRedefinition]()

          while (rs.next()) {
            val redefinitions = Map(
              "caption" -> Option(rs.getObject("Caption")).map(f => f.toString).getOrElse(null),
              "editorType" -> Option(rs.getObject("EditorType")).map(f => f.toString).getOrElse(null),
              "sortOrder" ->  Option(rs.getObject("SortOrder")).map(f => f.toString).getOrElse(null),
              "isVisible" -> DBUtils.fromDbBooleanOption(rs.getString("IsVisible")).getOrElse(null) ,
              "isReadOnly" -> DBUtils.fromDbBooleanOption(rs.getString("IsReadOnly")).getOrElse(null) ,
              "format" ->  Option(rs.getObject("Format")).map(f => f.toString).getOrElse(null),
              "isMasked" ->  DBUtils.fromDbBooleanOption(rs.getString("IsMasked")).getOrElse(null),
              "isDropDownNotAllowed" -> DBUtils.fromDbBooleanOption(rs.getString("IsDropDownNotAllowed")).getOrElse(null) ,
              "groupId"-> Option(rs.getObject("TabID")).map(f => f.toString).getOrElse(null),
              "groupCaption"-> Option(rs.getObject("TabCaption")).map(f => f.toString).getOrElse(null),
              "groupSortOrder" ->  Option(rs.getObject("TabSortOrder")).map(f => f.toString).getOrElse(null)
            ).filter(f => f._2 != null)

            fieldList append StatusFieldRedefinition(
              id = rs.getString("ID"),
              statusId = rs.getString("StatusID"),
              viewFieldId = rs.getString("ViewFieldID"),
              viewFieldCode = rs.getString("ViewField_Code"),
              viewId = rs.getString("ViewID"),
              propertyId = rs.getString("PropertyID"),
              redefinitions = redefinitions
            )

          }

          fieldList.filterNot(_.viewId == null).toSeq
        }
    }
  }

  /**
   * Получение данных о параметрах полей формы-карточки из хранилища данных
   */
  private def getGridFieldPropertiesFromStorage(): Seq[StatusFieldRedefinition] = {
    connectionProvider.withConnection {
      connection =>
        dataOperation("Получение параметров полей таблицы из хранилища данных") {
          val rs = DB.dbExec(connection,
            "[_Meta].[GetStatusViewFieldGrid]",
            contextProvider.getContext.sessionId,
            Nil,execStatProvider = execStatProvider)


          val fieldList = scala.collection.mutable.Buffer[StatusFieldRedefinition]()
          while (rs.next()) {
            val redefinitions = Map(
              "editorType" -> Option(rs.getObject("EditorType")).map(f => f.toString).getOrElse(null),
              "isVisible" -> DBUtils.fromDbBooleanOption(rs.getString("IsVisible")).getOrElse(null) ,
              "isReadOnly" -> DBUtils.fromDbBooleanOption(rs.getString("IsReadOnly")).getOrElse(null) ,
              "format" ->  Option(rs.getObject("Format")).map(f => f.toString).getOrElse(null) ,
              "isMasked" ->  DBUtils.fromDbBooleanOption(rs.getString("IsMasked")).getOrElse(null),
              "isDropDownNotAllowed" -> DBUtils.fromDbBooleanOption(rs.getString("IsDropDownNotAllowed")).getOrElse(null)
            ).filter(f => f._2 != null)

            fieldList append StatusFieldRedefinition(
              id = rs.getString("ID"),
              statusId = rs.getString("StatusID"),
              viewFieldId = rs.getString("ViewFieldID"),
              viewFieldCode = rs.getString("ViewField_Code"),
              viewId = rs.getString("WindowGridID"),
              propertyId = rs.getString("PropertyID"),
              redefinitions = redefinitions
            )

          }

          fieldList.filterNot(_.viewId == null).toSeq

        }
    }
  }
  def getReportFieldDetail(reportTemplateId:String):Seq[ReportFieldDetail]={
    getSeqMetaData(reportTemplateId,ReportFieldDetail)
  }

  def getDiagramRelation(viewDiagramId:String):Seq[MetaDiagramRelation] ={
    loadRelationsToCache()
    Option(domainCacheService.get(classOf[MetaDiagramRelationSeq],viewDiagramId)).map(_.data).getOrElse(Seq())
  }

  def getMetaRelation(relationId:String):MetaDiagramRelation={
    loadRelationsToCache()
    domainCacheService.get(classOf[MetaDiagramRelation],relationId)
  }
  private def loadRelationsToCache(){
    new LockProvider().locked("ru.atmed.omed.beans.model.meta.MetaDiagramRelation"){
      if(domainCacheService.isEmpty(classOf[MetaDiagramRelationSeq])) {
        val metaDiagramRelation =  metaFormDBprovider.loadDiagramRelationFromDb
        val updated = metaDiagramRelation.map(f =>
          if(f.viewDiagramDetailId==null) {
            val metaGrid =   getWindowGridMeta(f.mainGridId)
            val isEditAllowed =metaGrid.isEditAllowed
            val fromReadOnly = metaGrid.fields.find(p => p.getCodeName == f.startPropertyCode).map( p => p.isReadOnly)
           // val toReadOnly = columns.find(p => p.getCodeName == f.endPropertyCode).map( p => p.isReadOnly)
            f.copy(isInsertAllowed = isEditAllowed && !fromReadOnly.getOrElse(false),
              isDeleteAllowed = isEditAllowed && !fromReadOnly.getOrElse(false),
              fromReadOnly =true,
              toReadOnly = !isEditAllowed || fromReadOnly.getOrElse(false))

          }
          else {
            val diagramDetails= getDiagramDetail(f.viewDiagramId)
            val detailGridID = diagramDetails.find(p => p.id == f.viewDiagramDetailId)
            if(detailGridID.isEmpty) throw new MetaModelError("Не найден detailGrid "+ f.viewDiagramDetailId)
            val metaGrid = getWindowGridMeta(detailGridID.get.windowGridId)
            val isEditAllowed = metaGrid.isEditAllowed
            val fromReadOnly = metaGrid.fields.find(p => p.getCodeName == f.startPropertyCode).map( p => p.isReadOnly)
            val toReadOnly = metaGrid.fields.find(p => p.getCodeName == f.endPropertyCode).map( p => p.isReadOnly)
            f.copy(isInsertAllowed = metaGrid.isInsertAllowed,
              isDeleteAllowed =metaGrid.isDeleteAllowed,
              fromReadOnly = !isEditAllowed || fromReadOnly.getOrElse(false),
              toReadOnly = !isEditAllowed || toReadOnly.getOrElse(false))
          }
        )
        updated.foreach(f => domainCacheService.put(classOf[MetaDiagramRelation],f.id,f))
        updated.groupBy(f =>f.viewDiagramId).foreach(f => domainCacheService.put(classOf[MetaDiagramRelationSeq],f._1,MetaDiagramRelationSeq(f._2)))
      }
    }
  }

  def getMetaDiagramDetail(detailId:String):MetaDiagramDetail={
    loadDiagramDetailToCache()
    domainCacheService.get(classOf[MetaDiagramDetail],detailId)
  }
  def getDiagramDetail(viewDiagramId:String):Seq[MetaDiagramDetail]={
    loadDiagramDetailToCache()
    Option(domainCacheService.get(classOf[MetaDiagramDetailSeq],viewDiagramId)).map(_.data).getOrElse(Seq())
  }
  private def loadDiagramDetailToCache(){
    new LockProvider().locked("ru.atmed.omed.beans.model.meta.MetaDiagramDetail"){
      if(domainCacheService.isEmpty(classOf[MetaDiagramDetailSeq])) {
        val metaDiagramDetail =  metaFormDBprovider.loadDiagramDetailFromDb
        metaDiagramDetail.foreach(f=>domainCacheService.put(classOf[MetaDiagramDetail],f.id,f))
        metaDiagramDetail.groupBy(f =>f.viewDiagramId).foreach(f => domainCacheService.put(classOf[MetaDiagramDetailSeq],f._1,MetaDiagramDetailSeq(f._2)))
      }
    }
  }
  def getViewDiagramId(windowGridId:String):String={
    new LockProvider().locked("ru.atmed.omed.beans.model.meta.MetaViewDiagram"){
      if(domainCacheService.isEmpty(classOf[MetaViewDiagram])) {
        val metaViewDiagram =  metaFormDBprovider.loadViewDiagramFromDb
        metaViewDiagram.foreach(f => domainCacheService.put(classOf[MetaViewDiagram],f.mainGridId,f))
      }
    }
    Option(domainCacheService.get(classOf[MetaViewDiagram],windowGridId)).map(f =>f.id).getOrElse(null)
  }
  def getStatusMenuRedefinitions(statusId:String):Seq[StatusMenuRedefinition] ={
     getSeqMetaData(statusId,StatusMenuRedefinition)
  }
  def getTemplateClass(templateClassId:String,typeId:String):TemplateClass={
    new LockProvider().locked("ru.atmed.omed.beans.model.meta.TemplateClass"){
      if(domainCacheService.isEmpty(classOf[TemplateClass])) {
        val templates = metaFormDBprovider.loadMetaData(TemplateClass)
        val templateProperties =  metaFormDBprovider.loadMetaData(TemplateClassProperty).groupBy(_.templateClassId)
        templates.foreach(f => domainCacheService.put(classOf[TemplateClass],f.classId + f.templateClassTypeId,f.copy(templateProperties = templateProperties.get(f.id).getOrElse(Seq()))))

      }
    }
    domainCacheService.get(classOf[TemplateClass],templateClassId+typeId)
  }

  /**
   * Получить массив метаданных по ключу и кешируемому классу
   * @param key
   * @param companion
   * @tparam A
   * @return
   */
  def getSeqMetaData[A <: AnyRef](key:String,companion:MetaCreation[A]):Seq[A]={
    if(key == null ) return Seq()
    cacheMetaData(companion)
    Option(domainCacheService.get(companion.storedSeqClass.asInstanceOf[Class[MetaDataSeq[A]]],key)).map(_.data).getOrElse(Seq())
  }

  /**
   * Получить метаданные по ключу и кешируемому классу
   * @param key
   * @param companion
   * @tparam A
   * @return
   */
  def getObjectMetaData[A <: AnyRef](key:String,companion:MetaCreation[A]) ={
    if(key == null )  null
    else
    {
      cacheMetaData(companion)
      domainCacheService.get(companion.storedObjectClass,key)
    }
  }

  /**
   * Кешировать абстрактные метаданные
   *
   * @param companion
   * @tparam A
   */
  def cacheMetaData[A <: AnyRef](companion:MetaCreation[A]){
    new LockProvider().locked(companion.getClass.getName){
      val seqClass = companion.storedSeqClass.asInstanceOf[Class[MetaDataSeq[A]]]
      if(domainCacheService.isEmpty(seqClass)) {
        val objects =  metaFormDBprovider.loadMetaData(companion)
        // сохраняем сгруппированные значения
        if(companion.groupValue!=null) {
          objects.groupBy(companion.groupValue).foreach(f => {
            if(f._1 !=null) {
              val obj = companion.createSeqObj(f._2)
              domainCacheService.put(seqClass,f._1,obj)
            }
          })
        }
        // сохраняем отдельные значения
        if(companion.idValue!=null) {
          val objClass = companion.storedObjectClass.asInstanceOf[Class[AnyRef]]
          objects.foreach(f => {
            domainCacheService.put(objClass,companion.idValue(f),f)
          })
        }
      }
    }
  }

  def getWindowGridForClass(classId:String):String={
    connectionProvider.withConnection {
      connection =>
        val statement = connection.prepareStatement(MetaFormQuery.WindowGridQuery)
        statement.setString(1,classId)
        val resultSet = statement.executeQuery()
        var result:String = null
        while (resultSet.next()) {
           result= resultSet.getString("ID")
        }
        result
    }
  }

}
