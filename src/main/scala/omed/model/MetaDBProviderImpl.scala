package omed.model

import omed.db._
import scala.collection.mutable.ArrayBuffer
import com.google.inject.Inject
import omed.cache.ExecStatProvider
import omed.errors.MetaModelError
import ru.atmed.omed.beans.model.meta._
import omed.system.ContextProvider
import ru.atmed.omed.beans.model.meta.PredNotificationDescription
import ru.atmed.omed.beans.model.meta.MetaObjectStatus
import ru.atmed.omed.beans.model.meta.ObjectStatusTransition
import omed.data.{EntityFactory, FieldColorRule, ColorRule}
import ru.atmed.omed.beans.model.meta.PredNotificationDescription
import ru.atmed.omed.beans.model.meta.MetaObjectStatus
import ru.atmed.omed.beans.model.meta.ObjectStatusTransition
import omed.lang.eval.DBUtils
import omed.bf.ConfigurationProvider
import omed.model.services.ExpressionEvaluator

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 17.12.13
 * Time: 14:35
 * To change this template use File | Settings | File Templates.
 */
class MetaDBProviderImpl extends MetaDBProvider with DataAccessSupport {

  @Inject
  var connectionProvider: ConnectionProvider = null
  @Inject
  var execStatProvider: ExecStatProvider = null
  @Inject
  var contextProvider: ContextProvider = null
  @Inject
  var metaClassProvider: MetaClassProvider = null

  def loadStatusesFromDb: List[MetaObjectStatus] = {
    connectionProvider.withConnection {
      connection =>
        var statement = connection.prepareStatement(MetaQuery.getPredNotificationDescriptionsQuery)
        statement.setString(1, contextProvider.getContext.domainId.toString)
        var dbResult = statement.executeQuery()
        val predNotificationsList = new ArrayBuffer[PredNotificationDescription]
        while (dbResult.next()) {
          predNotificationsList += PredNotificationDescription(id = dbResult.getString("ID"),
            statusId = dbResult.getString("StatusID"),
            notificationGroupId = dbResult.getString("NotificationGroupID"),
            maxTransitionTime = dbResult.getInt("MaxProcessingTime"))
        }
        val predNotificationMap = predNotificationsList.groupBy(f => f.statusId)
        statement = connection.prepareStatement(MetaQuery.getAllStatusQuery)
        dbResult = statement.executeQuery()
        val statusList = new ArrayBuffer[MetaObjectStatus]
        while (dbResult.next()) {
          val id = Option(dbResult.getObject("ID")).map(x => x.toString).orNull
          val dId = Option(dbResult.getObject("StatusDiagramID")).map(x => x.toString).orNull
          val name = dbResult.getString("Name")
          val isNew = DBUtils.fromDbBoolean(dbResult.getString("IsNew"))
          val isEditNotAllowed = DBUtils.fromDbBoolean(dbResult.getString("IsEditNotAllowed"))
          val isDeleteNotAllowed = DBUtils.fromDbBoolean(dbResult.getString("IsDeleteNotAllowed"))
          val defaultTabId = dbResult.getString("DefaultTabID")
          //if (diagrammId == dId)
          val predNotidficaction = if (predNotificationMap.contains(id)) predNotificationMap(id) else Seq()

          statusList += new MetaObjectStatus(id, name, isNew, dId, isEditNotAllowed, isDeleteNotAllowed, defaultTabId, predNotidficaction)
        }
        statusList.toList
    }
  }

  def loadTransitionsFromDb: Map[String, Seq[ObjectStatusTransition]] = {
    connectionProvider.withConnection {
      connection =>
        val statement = connection.prepareStatement(MetaQuery.getAllTransitionQuery)
        val resultSet = statement.executeQuery()
        val transitionList = new ArrayBuffer[ObjectStatusTransition]
        while (resultSet.next()) {
          val id = resultSet.getObject("ID").asInstanceOf[String]
          val statusDiagramID = resultSet.getString("StatusDiagramID")
          val beginStatusID = resultSet.getString("BeginStatusID")
          val endStatusID = resultSet.getString("EndStatusID")
          val condition = resultSet.getString("condition")
          val moduleID = resultSet.getString("ModuleID")
          val classID = resultSet.getString("ClassID")
          transitionList += new ObjectStatusTransition(
            id, statusDiagramID,
            beginStatusID, endStatusID,
            condition, moduleID, classID)
        }
        val transitionMap = transitionList.groupBy(f => f.statusDiagramID)
        //для классов у которых не указана диаграмма взять диаграмму родительского класса
        metaClassProvider.getAllClassesMetadata().map(f => f.id -> (if (transitionMap.contains(f.diagramId)) transitionMap(f.diagramId) else null)).toMap
      //  metaClassProvider.getAllClassesMetadata().map(f => f.id->Option(getParentClassWithDiagram(f.id)).map(p => transitionMap(p)).getOrElse(null)).toMap
    }
  }

  private def getParentClassWithDiagram(classId: String): String = {
    if (classId == null) return null
    val meta = metaClassProvider.getClassMetadata(classId)
    if (meta.diagramId != null) meta.id
    else getParentClassWithDiagram(meta.parentId)
  }

  def loadClassesFromDb: ArrayBuffer[MetaObject] = {
    connectionProvider.withConnection {
      connection =>
        val statement = connection.prepareStatement(MetaQuery.getAllClassMetaDataQuery)

        val resultSet = DBProfiler.profile("execute load classes", execStatProvider) {
          statement.executeQuery()
        }
        var metaObjects = new ArrayBuffer[MetaObject]

        dataOperation {
          while (resultSet.next()) {
            metaObjects += {
              val id = resultSet.getString("ID") //Option(dbResult.getObject("ID")).map(x => x.toString).orNull
              val parentId = resultSet.getString("ParentID") //Option(dbResult.getObject("ParentID")).map(x => x.toString).orNull
              val code = resultSet.getString("Code")
              val diagramId = resultSet.getString("StatusDiagramID") // Option(dbResult.getObject("StatusDiagramID")).map(x => x.toString).orNull
              val lockTimeout = Option(resultSet.getObject("LockTimeout")).map(_.asInstanceOf[Int]) //Option(dbResult.getObject("LockTimeout")).map(_.asInstanceOf[Int])
              val storageDomain = Option(resultSet.getObject("StorageDomain")).map(_.asInstanceOf[Int])
              val aliasPattern = resultSet.getString("AliasPattern")
              MetaObject(id,aliasPattern, parentId, code, List.empty[MetaField], diagramId, lockTimeout, storageDomain)
            }
          }
        }

        metaObjects
    }
  }

  def loadFieldsFromDb(metaObjects: ArrayBuffer[MetaObject]): (Map[String, ArrayBuffer[MetaField]], Map[String, ArrayBuffer[ArrayField]]) = {
    Map()
    connectionProvider.withConnection {
      connection =>
        val statement = connection.prepareStatement(MetaQuery.getAllFieldsQuery)
        statement.setFetchSize(1000)
        val dbResult = DBProfiler.profile("execute load fields", execStatProvider) {
          statement.executeQuery()
        }
        val arrayStatement = connection.prepareStatement(MetaQuery.getArrayNameQuery)
        val arrayNameResult = DBProfiler.profile("execute load fields", execStatProvider) {
          arrayStatement.executeQuery()
        }
        var metaFieldsTmp = new ArrayBuffer[(String, MetaField)]
        val metaObjectsIndex = metaObjects.map(obj => obj.id -> obj).toMap
        val metaObjectBackRefsMap = metaObjects.map(_.code -> ArrayBuffer[ArrayField]()).toMap
        dataOperation {
          while (dbResult.next()) {
            val id = Option(dbResult.getObject("ID")).map(x => x.toString).orNull
            val classId = Option(dbResult.getObject("ClassID")).map(x => x.toString).orNull
            val code = Option(dbResult.getString("Code")).orNull
            val typeCode = Option(dbResult.getString("Type_Code")).orNull
            val typeTypeCode = Option(dbResult.getString("Type_TypeCode")).orNull

            val classCode = metaObjectsIndex.get(classId).map(_.code) getOrElse {
              val message = String.format(
                "Отсутствует класс %s, которому принадлежит поле %s",
                classId, id)
              throw new MetaModelError(message)
            }
            if (typeTypeCode != null) {
              val metaField = typeTypeCode.toLowerCase() match {
                case "ref" => {
                  //запрашивается отдельно
                  //  val arrayName = Option(dbResult.getString("ArrayName")).orNull
                  val refClassCode = Option(typeCode).map(_.replaceAll("Ref_", "")).orNull
                  if (!metaObjectBackRefsMap.contains(refClassCode)) {
                    val message = String.format(
                      "Отсутствует класс %s, на который ссылается поле %s класса %s",
                      refClassCode, code, classCode)
                    throw new MetaModelError(message)
                  }

                  ReferenceField(id = id, code = code, refObjectCode = refClassCode)
                }
                case "br" => {
                  null
                }

                case _ => {
                  val dataType = DataType.aliases.get(typeTypeCode) getOrElse {
                    throw new Exception("Тип '" + typeTypeCode + "' не найден.")
                  }
                  DataField(id = id, code = code, dataType = dataType)
                }
              }

              metaFieldsTmp += classId -> metaField
            }
          }


        }

        while (arrayNameResult.next()) {
          val arrayName = arrayNameResult.getString("ArrayName")
          val refClassCode = arrayNameResult.getString("ParentClassCode")
          val classCode = arrayNameResult.getString("RelationClassCode")
          val refPropertyCode = arrayNameResult.getString("RelationGridPropertyCode")
          if (arrayName != null && arrayName.trim().length > 0) {
            metaObjectBackRefsMap(refClassCode) += ArrayField(null, arrayName.trim(), classCode, refPropertyCode)
          }
        }
        DBProfiler.profile("MAP load fields", execStatProvider, true) {
          (metaFieldsTmp.groupBy(_._1).mapValues(_.map(_._2)), metaObjectBackRefsMap)
        }
    }
  }

  def loadModuleInDomainFromDb: Seq[ModuleInDomain] = {
    connectionProvider.withConnection {
      connection =>
        val statement = connection.prepareStatement(MetaQuery.getModuleInDomainQuery)

        val resultSet = DBProfiler.profile("execute load classes", execStatProvider) {
          statement.executeQuery()
        }
        var array = new ArrayBuffer[ModuleInDomain]

        dataOperation {
          while (resultSet.next()) {
            array += ModuleInDomain(resultSet)
          }
        }
        array.toSeq
    }
  }

  def loadColorRules: Seq[ColorRule] = {
    connectionProvider.withConnection {
      connection =>
        DBProfiler.profile("cache colorationRules appServer", execStatProvider,true) {
          var statement = connection.prepareStatement(MetaQuery.stringColorationQuery + metaClassProvider.getFilterModuleInDomain(contextProvider.getContext.domainId))
          var dbResult = dataOperation {
            DBProfiler.profile("[_Meta].[GetColorationRules]", execStatProvider) { statement.executeQuery() }
          }
          val colorRulesList = new scala.collection.mutable.HashMap[String, scala.collection.mutable.ArrayBuffer[ColorRule]]
          val colors = scala.collection.mutable.ArrayBuffer[ColorRule]()
          def addColor(colorRule: ColorRule){
              colors += colorRule
          }

          dataOperation {
            while (dbResult.next()) {
              val colorRule = ColorRule(dbResult)
              addColor(colorRule)
            }
          }
          statement = connection.prepareStatement(MetaQuery.fieldColorationQuery + metaClassProvider.getFilterModuleInDomain(contextProvider.getContext.domainId, "cc"))
          dbResult = dataOperation {
            DBProfiler.profile("[_Meta].[GetColorationRules]", execStatProvider) { statement.executeQuery() }
          }
          dataOperation {
            while (dbResult.next()) {
              val fieldColor = FieldColorRule(dbResult)
              addColor(fieldColor)
            }
          }
          colors.toSeq
//          metaClassProvider.getAllClasses().map(f =>{
//            val tmp  = metaClassProvider.getAllParents(f._2.id).map(colorRulesList.get(_).getOrElse(List())).flatten
//            f._2.id -> tmp.toSeq
//          } ).toMap

        }
    }
  }


}
