package omed.forms

import omed.db._
import ru.atmed.omed.beans.model.meta._
import com.google.inject.Inject
import omed.cache.ExecStatProvider
import scala.collection.mutable.ArrayBuffer
import ru.atmed.omed.beans.model.meta.MetaViewDiagram
import ru.atmed.omed.beans.model.meta.MetaDiagramDetail
import ru.atmed.omed.beans.model.meta.ReportFieldDetail
import ru.atmed.omed.beans.model.meta.MetaDiagramRelation
import ru.atmed.omed.beans.model.meta.MetaViewDiagram
import ru.atmed.omed.beans.model.meta.MetaGridColumn
import ru.atmed.omed.beans.model.meta.MetaDiagramDetail
import ru.atmed.omed.beans.model.meta.ReportFieldDetail
import ru.atmed.omed.beans.model.meta.MetafieldImpl
import ru.atmed.omed.beans.model.meta.MetaDiagramRelation
import omed.system.ContextProvider
import omed.model.MetaClassProvider
import omed.lang.eval.DBUtils

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 09.01.14
 * Time: 15:29
 * To change this template use File | Settings | File Templates.
 */
class MetaFormDBProviderImpl  extends MetaFormDBProvider with DataAccessSupport {
  @Inject
  var connectionProvider: ConnectionProvider = null
  @Inject
  var execStatProvider : ExecStatProvider = null
  @Inject
  var contextProvider: ContextProvider = null
  @Inject
  var metaClassProvider:MetaClassProvider = null
  private def arrayToDBParam(list:Seq[String]):String={
    if(list!=null) list.map(f => "<object ID=\""+f+"\"/>").mkString("\n") else null
  }
//  def loadReportFieldDetailFromDb:Seq[ReportFieldDetail] ={
//    DBProfiler.profile("load ReportFieldDetail from DB",execStatProvider,true)  {
//      connectionProvider.withConnection {
//      connection =>
//        val statement = connection.prepareStatement(MetaFormQuery.reportFieldDetailQuery)
//        val resultSet =statement.executeQuery()
//        val reportFieldsList = new ArrayBuffer[ReportFieldDetail]
//        while (resultSet.next()) {
//          val fieldId = resultSet.getString("FieldID")
//          val viewCardId = resultSet.getString("ViewCardID")
//          val reportTemplateId = resultSet.getString("ReportTemplateID")
//
//          if(fieldId!= null && viewCardId!= null && reportTemplateId != null)
//            reportFieldsList += new ReportFieldDetail(fieldId = fieldId,viewCardId = viewCardId,reportTemplateId = reportTemplateId)
//        }
//        reportFieldsList.toSeq
//      }
//    }
//  }
  def loadDiagramDetailFromDb:Seq[MetaDiagramDetail]   ={
    DBProfiler.profile("load DiagramDetailFromDb from DB",execStatProvider,true)  {
      connectionProvider.withConnection {
        connection =>
          val statement = connection.prepareStatement(MetaFormQuery.diagramDetailQuery)
          val resultSet =statement.executeQuery()
          val diagramDetailList = new ArrayBuffer[MetaDiagramDetail]
          while (resultSet.next()) {
            val id = resultSet.getString("ID")
            val viewDiagramId = resultSet.getString("ViewDiagramID")
            val detailClassId  = resultSet.getString("DetailClassID")
            val detailRelPropertyCode = resultSet.getString("DetailRelPropertyCode")
            val windowGridId =  resultSet.getString("WindowGridID")
            val isVisible = DBUtils.fromDbBoolean(resultSet.getString("IsVisible"))
            diagramDetailList += new MetaDiagramDetail(id = id,viewDiagramId = viewDiagramId,detailClassId = detailClassId,detailRelPropertyCode = detailRelPropertyCode,windowGridId = windowGridId,isVisible = isVisible)
          }
          diagramDetailList.toSeq
      }
    }
  }

  def loadDiagramRelationFromDb:Seq[MetaDiagramRelation] ={
    DBProfiler.profile("load DiagramRelationFromDb from DB",execStatProvider,true)  {
      connectionProvider.withConnection {
        connection =>
          val statement = connection.prepareStatement(MetaFormQuery.diagramRelationQuery)
          val resultSet = statement.executeQuery()
          val diagramRelationList = new ArrayBuffer[MetaDiagramRelation]
          while (resultSet.next()) {
              diagramRelationList+=MetaDiagramRelation(resultSet)
          }
          diagramRelationList.toSeq
      }
    }
  }

  def loadViewDiagramFromDb:Seq[MetaViewDiagram]={
    DBProfiler.profile("load ViewDiagramFromD from DB",execStatProvider,true)  {
      connectionProvider.withConnection {
        connection =>
          val statement = connection.prepareStatement(MetaFormQuery.viewDiagramQuery)
          val resultSet = statement.executeQuery()
          val viewDiagramlist= new ArrayBuffer[MetaViewDiagram]
          while (resultSet.next()) {
            val id = resultSet.getString("ID")
            val mainGridId = resultSet.getString("MainGridID")
            viewDiagramlist += new MetaViewDiagram(
              id = id,
              mainGridId = mainGridId
            )
          }
          viewDiagramlist.toSeq
      }
    }
  }
  def loadWindowGridMetaFromDB(windowGridIDList:Seq[String]):Seq[MetaGrid] ={
    connectionProvider.withConnection {
      connection =>
        val dbResult = DB.dbExec(connection, "[_Meta].[GetWindowGridMetadata]",
          contextProvider.getContext.sessionId,
          List(("WindowGridIDList",arrayToDBParam(windowGridIDList))),execStatProvider = execStatProvider)
        val result = ArrayBuffer[MetaGrid]()
        while(dbResult.next()){
          result+= MetaGrid(
            classId = dbResult.getString("ClassID"),
            windowGridId=  dbResult.getString("WindowGridID"),
            isDeleteAllowed =  DBUtils.fromDbBoolean(dbResult.getString("IsDeleteAllowed")),
            isInsertAllowed =  DBUtils.fromDbBoolean(dbResult.getString("IsInsertAllowed")),
            isEditAllowed =  DBUtils.fromDbBoolean(dbResult.getString("IsEditAllowed")),
            isGoOnCardAfterInsert =  DBUtils.fromDbBoolean(dbResult.getString("IsGoOnCardAfterInsert")),
            diagramType=  dbResult.getString("DiagramType"),
            abscissaCode =  dbResult.getString("AbscissaCode"),
            isSearchVisible=  DBUtils.fromDbBoolean(dbResult.getString("IsSearchVisible")),
            isSchedulerView =  DBUtils.fromDbBoolean(dbResult.getString("IsSchedulerView"))
          )
        }
        result.toSeq
    }
  }
  def loadMetaGridColumnsFromDB(windowGridIDList:Seq[String]):Seq[MetaGridColumn]={
     connectionProvider.withConnection {
      connection =>
        val dbResult = DB.dbExec(connection, "[_Meta].[GetWindowGridColumnMetadata]",
          contextProvider.getContext.sessionId,
          List("WindowGridIDList"->arrayToDBParam(windowGridIDList)),execStatProvider = execStatProvider)

        var columnsList = new ArrayBuffer[MetaGridColumn]
        while (dbResult.next()) {
          columnsList +=
            MetaGridColumn(
              metafield = MetafieldImpl(
                viewFieldId = dbResult.getString("ViewFieldID"),
                codeName = dbResult.getString("Code"),
                editorType = dbResult.getString("EditorType"),
                typeCode = dbResult.getString("Type_TypeCode"),
                typeExtInfo = dbResult.getString("Type_ExtInfo")),
              caption = dbResult.getString("Caption"),
              sortOrder = Option(dbResult.getInt("SortOrder")).getOrElse(0),
              isReadOnly = DBUtils.fromDbBoolean(dbResult.getString("IsReadOnly")),
              format = dbResult.getString("Format"),
              isDropDownNotAllowed = DBUtils.fromDbBoolean(dbResult.getString("IsDropDownNotAllowed")),
              isMasked = DBUtils.fromDbBoolean(dbResult.getString("IsMasked")),
              isVisible = DBUtils.fromDbBoolean(dbResult.getString("IsVisible")),
              isHidden = DBUtils.fromDbBoolean(dbResult.getString("IsHidden")),
              aggregateFunction = dbResult.getString("AggregateFunction"),
              width = Option(dbResult.getInt("Width")).getOrElse(0),
              defaultFormGridId = dbResult.getString("DefaultFormGridID"),
              extInfo = dbResult.getString("ExtInfo"),
              refParams = dbResult.getString("ReferenceParams"),
              mask = dbResult.getString("Mask"),
              isJoinMask = DBUtils.fromDbBoolean(dbResult.getString("IsJoinMask")),
              isShowOnChart = DBUtils.fromDbBoolean(dbResult.getString("IsShowOnChart")),
              normaMin = Option(dbResult.getDouble("NormaMin")).getOrElse(0),
              normaMax = Option(dbResult.getDouble("NormaMax")).getOrElse(0),
              isRefreshOnChange = DBUtils.fromDbBoolean(dbResult.getString("IsRefreshOnChange")),
              windowGridId = dbResult.getString("WindowGridID")
            )
        }
        if (columnsList.length == 0) Seq() else columnsList.toSeq
    }
  }
  def loadRelationGridListFromDB(viewCardId: String):Seq[MetaCardGrid] ={
    connectionProvider.withConnection {
      connection =>
        val dbResult = DB.dbExec(connection, "[_Meta].[GetRelationGridList]",
          contextProvider.getContext.sessionId,
          List(("ViewCardID", viewCardId)),execStatProvider = execStatProvider)
          DBProfiler.profile("cache GetRelationGridList",execStatProvider,true){
            var gridList = new ArrayBuffer[MetaCardGrid]
            while (dbResult.next()) {
              val windowGridId = dbResult.getString("WindowGridId")
              val viewCardGridId = dbResult.getString("ViewCardGridId")
              val caption = dbResult.getString("Caption")
              val sortOrder = Option(dbResult.getInt("SortOrder"))
              val glyph = Option(dbResult.getBytes("Glyph")).orNull
              val contextMenuId = dbResult.getString("ContextMenuID")
              val arrayName = dbResult.getString("ArrayName")
              gridList+=MetaCardGrid(
                caption = caption,
                arrayName = arrayName,
                glyph = glyph,
                viewCardGridId = viewCardGridId,
                windowGridId= windowGridId,
                contextMenuId = contextMenuId,
                sortOrder = sortOrder)
          }
          gridList.toSeq
        }
    }
  }
  def loadSubClassListFromDB(classList:Seq[String]):Seq[Subclass]={
   connectionProvider.withConnection {
      connection =>
        val dbResult = DB.dbExec(connection, "[_Meta].[GetSubclassList]",
          contextProvider.getContext.sessionId,
          List("ClassIDList"->arrayToDBParam(classList)),execStatProvider = execStatProvider)

        var subClassList = new ArrayBuffer[Subclass]
        while (dbResult.next()) {
          subClassList += new Subclass(
            subClassId = Option(dbResult.getString("SubClassID")).orNull,
            name = Option(dbResult.getString("Name")).orNull,
            parentId = Option(dbResult.getString("ParentID")).orNull,
           rootClassId =dbResult.getString("RootClassID"))
        }

        if (subClassList.isEmpty) Seq() else subClassList.toSeq
    }
  }

//  def loadGridInCardItemFromDb:Seq[GridInCardItem] ={
//    connectionProvider.withConnection {
//      connection =>
//        val statement = connection.prepareStatement(MetaFormQuery.GridInCardQuery + metaClassProvider.getFilterModuleInDomain(contextProvider.getContext.domainId))
//        val resultSet =  DBProfiler.profile("execute  loadGridInCardItem",execStatProvider)  { statement.executeQuery() }
//        var array = new ArrayBuffer[GridInCardItem]
//        dataOperation {
//          while (resultSet.next()) {
//            array += GridInCardItem(resultSet)
//          }
//        }
//        array.toSeq
//    }
//  }
//  def loadSchedulerGroup:Seq[SchedulerGroup]={
//    connectionProvider.withConnection {
//      connection =>
//        val statement = connection.prepareStatement(MetaFormQuery.schedulerGroupQuery + metaClassProvider.getFilterModuleInDomain(contextProvider.getContext.domainId,"MSG"))
//        val resultSet =  DBProfiler.profile("load  scheduler groups",execStatProvider)  { statement.executeQuery() }
//        var array = new ArrayBuffer[SchedulerGroup]
//        dataOperation {
//          while (resultSet.next()) {
//            array += SchedulerGroup(resultSet)
//          }
//        }
//        array.toSeq
//    }
//  }
 // загружает метаданные из БД на основе переданного компаньона класса
  def loadMetaData[A <:AnyRef](companion:MetaCreation[A]):Seq[A]={
    connectionProvider.withConnection {
      connection =>
        val statement = connection.prepareStatement(companion.query(metaClassProvider,contextProvider))
        val resultSet =  DBProfiler.profile("load "+ companion.getClass().getName,execStatProvider)  { statement.executeQuery() }
        var array = new ArrayBuffer[A]
        dataOperation {
          while (resultSet.next()) {
            array += companion.apply(resultSet)
          }
        }
        array.filter(f => f!= null).toSeq
    }
  }
}
