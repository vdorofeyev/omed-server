package omed.data

import omed.db.{DBProfiler, DB, DataAccessSupport, ConnectionProvider}
import omed.system.ContextProvider
import omed.model._
import ru.atmed.omed.beans.model.meta._
import omed.lang.xml._
import omed.lang.eval._
import com.google.inject.Inject
import omed.errors._
import omed.cache.{ExecStatProvider, DomainCacheService}
import omed.forms.MetaFormProvider
import omed.lang.struct
import collection.mutable
import java.util.{Calendar, Date}
import scala.Predef._
import ru.atmed.omed.beans.model.meta.StatusWindowGrid
import ru.atmed.omed.beans.model.meta.NodeParameter
import ru.atmed.omed.beans.model.meta.FilterNode
import ru.atmed.omed.beans.model.meta.FilterNodeSeq

import omed.lang.eval.ValidatorContext
import omed.bf.{BusinessFunctionThreadPool, BusinessFunctionLogger, ConfigurationProvider}

import scala.xml.{Text, TopScope, Elem}
import scala.Option

import scala.collection.mutable.ArrayBuffer
import omed.auth.{PermissionType, PermissionProvider}
import omed.lang.struct.{Expression, Validator}


class DataReaderServiceImpl extends DataReaderService with DataAccessSupport {

  @Inject
  var connectionProvider: ConnectionProvider = null
  @Inject
  var contextProvider: ContextProvider = null
  @Inject
  var metaClassProvider: MetaClassProvider = null
  @Inject
  var metaFormProvider: MetaFormProvider = null
  @Inject
  var domainCacheService: DomainCacheService = null
  @Inject
  var dataWriterService: DataWriterService = null
  @Inject
  var expressionEvaluator: omed.model.services.ExpressionEvaluator = null
  @Inject
  var configProvider: ConfigurationProvider = null

  @Inject
  var execStatProvider:ExecStatProvider = null
  @Inject
  var entityFactory:EntityFactory = null
  @Inject
  var permissionProvider: PermissionProvider = null

  val blobTypes = Set(
    java.sql.Types.LONGVARBINARY,
    java.sql.Types.VARBINARY,
    java.sql.Types.BINARY,
    java.sql.Types.BLOB)

  private lazy val _domains = {
    connectionProvider.withConnection {
      connection =>
        val statement = connection.prepareStatement("select number from _domain")
        if (statement.execute()) {
          val buf = scala.collection.mutable.ListBuffer[Int]()
          val rs = statement.getResultSet()
          while (rs.next()) {
            buf append rs.getInt(1)
          }
          buf.toSeq
        } else null
    }
  }

  def getDomains(): Seq[Int] = _domains

  /**
   * Данные справочника
   */
  def getDictionaryData(viewFieldId: String, variablesXml: String, objectId: String = null): DataDictionary = {
    connectionProvider.withConnection {
      connection =>
        val params = List(
          "ViewFieldID" -> viewFieldId,
          "Variables" -> variablesXml,
          "ObjectID" -> objectId,
          "DNRoleID"->contextProvider.getContext.roleId
          ).filter(_._2 != null)
        val dbResult = dataOperation("Вызов GetDictionaryData") {
          DB.dbExec(connection,
            "[_Object].[GetDictionaryData]",
            contextProvider.getContext.sessionId,
            params,execStatProvider)
        }
        dataOperation("Чтение результатов GetDictionaryData") {
          val keysSeq = scala.collection.mutable.Buffer[String]()
          val dataSeq = scala.collection.mutable.Buffer[Any]()
          while (dbResult.next()) {
            val id = dbResult.getString(1)
            val data = dbResult.getObject(2)

            keysSeq += id
            dataSeq += data
          }
          new DataDictionary(keysSeq.toSeq, dataSeq.toArray)
        }
    }
  }

  /**
   * Построение дерева фильтрации по идентификатору корневого узла.
   * Для построения дерева используется результат из
   * [[omed.data.DataReaderServiceImpl.getAllTreeNodes]]
   */
  def getTreeFilter(rootNodeId: String): Seq[FilterNode] = {
    val key = rootNodeId

    val cached = domainCacheService.get(classOf[FilterNodeSeq], key)
    val nodes = if (cached != null)
      cached.data
    else {
      connectionProvider.withConnection {
        connection =>
          val allNodes = this.getAllTreeNodes

          def getKids(pId: String): List[FilterNode] = {
            val nodes = scala.collection.mutable.Buffer[FilterNode]()
            allNodes.foreach(node => {
              if (node.parentId != null && node.parentId == pId) {
                nodes += node
                nodes ++= getKids(node.id)
              }
            })
            nodes.toList
          }
          val nodesList = getKids(rootNodeId)

          domainCacheService.put(classOf[FilterNodeSeq], key, FilterNodeSeq(nodesList))
          nodesList
      }
    }

    nodes
  }

  /**
   * Получить все ноды всех деревьев (БД просто отдает список).
   * Результат используется для формирования деревьев в функции
   * [[omed.data.DataReaderServiceImpl.getTreeFilter]].
   */
  private def getAllTreeNodes: Seq[FilterNode] = {
    val key = "allTreeFilterNodes"

    val cached = domainCacheService.get(classOf[FilterNodeSeq], key)
    if (cached != null)
      cached.data
    else {
      connectionProvider.withConnection {
        connection =>
          val dbResult = dataOperation {
            DB.dbExec(connection,
              "[_Meta].[GetTreeNodes]",
              contextProvider.getContext.sessionId, List(),execStatProvider)
          }

          val nodesList = scala.collection.mutable.Buffer[FilterNode]()
          val allParameters = getAllTreeNodeParameters

          while (dbResult.next()) {
            val (id, name, parentId) = dataOperation {
              (dbResult.getString("NodeID"),
                dbResult.getString("Name"),
                dbResult.getString("ParentID"))
            }

            val hcuId = Option(contextProvider.getContext.hcuId).getOrElse("")
            val data = allParameters.filter(_.nodeId == id).map(p =>
              if ((p.defaultValue != null) && (p.defaultValue contains "@HCUID")) {
                val newDefaultValue = p.defaultValue.replaceAll("@HCUID", hcuId).trim match {
                  case x if x.isEmpty => null
                  case y => y
                }
                p.copy(defaultValue = newDefaultValue)
              } else p
            )

            nodesList += FilterNode(id, name, parentId, data)
          }

          val data = nodesList.toSeq
          domainCacheService.put(classOf[FilterNodeSeq], key, FilterNodeSeq(data))
          data
      }
    }
  }

  private def getAllTreeNodeParameters: List[NodeParameter] = {
    connectionProvider.withConnection {
      connection =>

        val dbResult = dataOperation {
          DB.dbExec(connection,
            "[_Meta].[GetTreeNodeParameters]",
            contextProvider.getContext.sessionId, List(),execStatProvider)
        }

        val paramList = scala.collection.mutable.Buffer[NodeParameter]()

        dataOperation {
          while (dbResult.next()) {

            val id = dbResult.getString("TreeParameterID")
            val nodeId = dbResult.getString("NodeID")
            val varName = dbResult.getString("VarName")
            val editorType = dbResult.getString("EditorType")
            val caption = dbResult.getString("Caption")
            val defaultValue = dbResult.getString("DefaultValue")
            val referenceParams = dbResult.getString("ReferenceParams")
            val parameterSqlFilter = dbResult.getString("ParameterSQLFilter")
            val viewFieldId = dbResult.getString("ViewFieldID")
            val typeTypeCode = dbResult.getString("Type_TypeCode")
            val typeExtInfo = dbResult.getString("Type_ExtInfo")
            val isNoTerminalSelection =  DBUtils.fromDbBoolean(dbResult.getString("IsNoTerminalSelection"))
            val isMultiSelect =  DBUtils.fromDbBoolean(dbResult.getString("IsMultiSelect"))
            paramList += NodeParameter(id, nodeId, caption,
              varName, editorType, defaultValue, referenceParams,
              parameterSqlFilter, viewFieldId, typeTypeCode, typeExtInfo,isNoTerminalSelection,isMultiSelect
            )
          }

          paramList.toList
        }
    }
  }

  /**
   * Данные сущности для формы-списка
   */
  def getGridData(
    gridId: String, nodeId: String, refId: String,
    nodeData: String, recordId: String, viewCardId: String, fieldId: String,
    variablesXml: String, treeVariablesXml: String,filters:Seq[Expression] = Seq(),
    context:Map[String,Value]=Map(),isFull:Boolean = false): DataTable = {

    val metaClass = metaClassProvider.getClassMetadata(metaFormProvider.getWindowGridMeta(gridId).classId)
    val perm = if(metaClass.storageDomain.getOrElse(null) == -2) Map(PermissionType.ReadExec ->true, PermissionType.Write ->(contextProvider.getContext.domainId == -2))
    else  permissionProvider.getSQLDataPermission( metaFormProvider.getWindowGridMeta(gridId).classId,filters,context)
    //получить разрешения на данные
    val sqlDataFilter = perm(PermissionType.ReadExec) match{
      case false =>  return new DataTable(Seq(), null, Seq(),Map())
      case true => null
      case s:String => s
    }
    //добавить в параметры значения системных переменных
    val updatedtreeParameter = updateTreeVariablesWithSystemVariables(treeVariablesXml)
    connectionProvider.withConnection {
      connection =>
        val params = List(
          "WindowGridID" -> gridId,
          "NodeID" -> nodeId,
          "RecordID" -> recordId,
          "RefID" -> refId,
          "NodeData" -> nodeData,
          "ViewCardID" -> viewCardId,
          "FieldID" -> fieldId,
          "Variables" -> variablesXml,
          "TreeVariables" -> updatedtreeParameter,
          "DNRoleID"->contextProvider.getContext.roleId,
          "SQLFilter"-> sqlDataFilter,
          "IsFull"->(if(isFull) "1" else "0"))
        val dbResult = dataOperation {
          DB.dbExec(
            connection,
            "[_Object].[GetWindowGridData]",
            contextProvider.getContext.sessionId,
            params,execStatProvider)
        }

        val (dataBuffer, columnSeq, binaries) = dataOperation {
          val meta = dbResult.getMetaData()
          val columnCount = meta.getColumnCount

          def isBlob(i: Int) = {
            val columnType = meta.getColumnType(i)
            blobTypes contains columnType
          }

          val columnSeq = for (i <- 1 to columnCount)
            yield meta.getColumnName(i)

          // evaluate isBlob attribute for each column
          val isBlobSeq = 1 to columnCount map isBlob
          // indexes of blob columns
          val binaries = for (i <- 0 until columnCount if isBlobSeq(i)) yield (i + 1)

          def fetchItem = (i: Int) =>
            if (isBlobSeq(i - 1)) dbResult.getBytes(i).asInstanceOf[Any]
            else dbResult.getObject(i).asInstanceOf[Any]

          val dataBuffer = scala.collection.mutable.Buffer[Array[Any]]()
          while (dbResult.next()) {
            val dataArr = (1 to columnCount map fetchItem).toArray
            dataBuffer += dataArr
          }
          (dataBuffer, columnSeq, binaries)
        }
        val column =  columnSeq.indexOf("ID")
        val tmp = dataBuffer.toSeq
        val writePermMap = perm(PermissionType.Write) match{
          case false =>  tmp.map(f => f(column).asInstanceOf[String] -> false).toMap
          case true =>  tmp.map(f => f(column).asInstanceOf[String] -> true).toMap
          case s:String => tmp.map(f => f(column).asInstanceOf[String] -> permissionProvider.getDataPermission(f(column).asInstanceOf[String])(PermissionType.Write)).toMap
        }
        new DataTable(columnSeq, binaries,tmp,writePermMap)
    }
  }

  // Определяет идентификатор класса по значениям полей в строке
  private def defineClassFromData(row: Array[Any], columnSeq: Seq[String]): String = {
    val classIdIndex = columnSeq.indexWhere(col => (col == "_ClassID"))
    if(classIdIndex == -1) throw new DataError("Поле _ClassID не найдено")
    row(classIdIndex).asInstanceOf[String]
  }
  // Определяет домен по значениям полей в строке
  private def defineDomainFromData(row: Array[Any], columnSeq: Seq[String]): Int = {
    val domainIndex = columnSeq.indexWhere(col => (col == "_Domain"))
    if(domainIndex == -1)contextProvider.getContext.domainId else row(domainIndex).asInstanceOf[Int]
  }
  private def defineStatusFromData(row: Array[Any], columnSeq: Seq[String]): String = {
    val statusIndex = columnSeq.indexWhere(col => (col == "_StatusID"))
    if(statusIndex == -1) throw new DataError("Поле _StatusID не найдено")
    row(statusIndex).asInstanceOf[String]
  }
  private class Colorator {
    import scala.collection.mutable.{Map => MutableMap}

    // Скомпилированные валидаторы
    val parsedValidators = MutableMap[String, Validator]()

    // Определяет контекст валидации, состоящий из одного объекта указанного класса
    def buildContextWithObjectOfClass(
      classId: String, columnSeq: Seq[String], row: Array[Any]) = {

      // сформировать данные для переменной this
      // (словарь кодов полей и их значений)
      val thisObjData = (columnSeq zip row).toMap

      // Создаем контекст валидации
      val thisObj = entityFactory.createEntityWithData(thisObjData)
      Map("this" -> thisObj)
    }
    def calculateColor(rules: Seq[ColorRule], context: Map[String,Value]) = {
      // get rules with defined color and sort by priority
      val actualRules = rules.filter(
        rule => rule.color != null && rule.condition != null).
        sortBy(_.priority)

      // get first successful rule
      val acceptedRule = actualRules.find(rule => {
        // TODO: use cached validators here (at least in one request)
        DataType.boolValueFromValue(expressionEvaluator.evaluate(rule.condition,configProvider.create(),context))
      })
      acceptedRule
    }

    // Формирует общий цвет для входной строки и массив значений цвета для каждого поля
    def colorize(row: Array[Any], columnSeq: Seq[String]) = {
      val classId = defineClassFromData(row, columnSeq)

      val classColorRules = metaClassProvider.getColorRules(classId)
      val (cellColorRules, rowColorRules) = if (classColorRules.length > 0)
        classColorRules.partition(_.isInstanceOf[FieldColorRule])
      else (Seq() -> Seq())
      val cellColorRulesMap = cellColorRules
        .map(_.asInstanceOf[FieldColorRule])
        .groupBy(_.propertyCode)

      val context = buildContextWithObjectOfClass(classId, columnSeq, row)
      val rowColor = calculateColor(rowColorRules, context).orNull
      val cellColors = columnSeq.map(col => {
        val cellRules = cellColorRulesMap.get(col).getOrElse(Seq())
        calculateColor(cellRules, context).map(_.color).orNull
      }).toArray

      rowColor -> cellColors
    }
  }

  private case class FieldProperties(priority: Int, properties: Map[String, Any])

  def getGridDataView(
                       windowGridId: String, nodeId: String, refId: String,
                       nodeData: String, recordId: String, viewCardId: String, fieldId: String,
                       variablesXml: String, treeVariablesXml: String, isDiagram : Boolean): DataViewTable = {

    val data = getGridData(
      windowGridId, nodeId, refId,
      nodeData, recordId, viewCardId,
      fieldId, variablesXml,
      treeVariablesXml)
    val objPositions = if(isDiagram) getObjectCoordinates(nodeId,treeVariablesXml,data) else null
    getGridDataView(windowGridId,data,isDiagram, objPositions)
  }

  private def getGridDataView(
    windowGridId: String,data:DataTable, isDiagram : Boolean, objectsPositions:Map[String,ObjectPosition]): DataViewTable = {
    if(data.data.length == 0) return new DataViewTable(null, null, Seq())
    //todo для переопределения гридов в зависимости от данных необходимо передать колонки грида
    val dataViewRows = getDataViewForData(data,windowGridId,Seq(),false,isDiagram,objectsPositions = objectsPositions)

    val metaDetail =  if(!isDiagram) null
                      else  metaFormProvider.getDiagramDetail(metaFormProvider.getViewDiagramId(windowGridId))
    val detailData =
      if(metaDetail ==null || data.data.length == 0) null
      else  {
         val column =  data.columns.indexOf("ID")
         metaDetail.map(f => {
           val metaClass = metaClassProvider.getClassMetadata(f.detailClassId)
           val tmp = data.data.map(p => getCollectionData(metaClass.code,f.detailRelPropertyCode,p(column).asInstanceOf[String]))
           val perm = tmp.map(_.perm).flatten.toMap
           getGridDataView(f.windowGridId,new DataTable(tmp(0).columns,Seq(), tmp.map(p => p.data).flatten,perm),false,null)
         })
      }
    val relations =
      if(!isDiagram) null
      else {
        val metaRelations = metaFormProvider.getDiagramRelation(metaFormProvider.getViewDiagramId(windowGridId))
        metaRelations.map(f => {
          // если связь задается полем на объекте
          if(f.viewDiagramDetailId==null) {
            val column =  data.columns.indexOf("ID")
            val column1 = data.columns.indexOf(f.startPropertyCode)
            val column2 = data.columns.indexOf(f.endPropertyCode)
            if (column1 == -1 || column2 == -1) Seq()
            else {
              dataViewRows.map( p =>
                dataViewRows.filter( e => e.data(column2) == p.data(column1) ).map(e => new DataRelation(f.id,p.data(column).asInstanceOf[String],null,p.data(column).asInstanceOf[String],e.data(column).asInstanceOf[String],p.fieldOverrides(f.startPropertyCode).get("isReadOnly").asInstanceOf[Option[Boolean]],p.fieldOverrides(f.endPropertyCode).get("isReadOnly").asInstanceOf[Option[Boolean]],Option(true)))
              ).flatten}
          }
          // если связь задается через связанный грид
          else {
             val detail =  detailData.find(p => p.windowGridId== metaDetail.find( p => p.id == f.viewDiagramDetailId).map(p => p.windowGridId).getOrElse(null)).getOrElse(null)
             if(detail==null)  Seq()
             else {
               val column =  data.columns.indexOf("ID")
               val column1 = detail.columns.indexOf(f.startPropertyCode)
               val column2 = detail.columns.indexOf(f.endPropertyCode)
               val column3 =  detail.columns.indexOf("ID")
               val column4 =  detail.columns.indexOf("_Name")
               if (column1 == -1 || column2 == -1) Seq()
               else {
                 detail.data.map (p => (data.data.find( e => e(column) == p.data(column1)), data.data.find( e => e(column) == p.data(column2)),p) ).filter(p => p._1.isDefined && p._2.isDefined)
                   .map( t => new DataRelation(f.id,t._3.data(column3).asInstanceOf[String],if(column4>=0) t._3.data(column4).asInstanceOf[String] else null,t._1.get(column).asInstanceOf[String],t._2.get(column).asInstanceOf[String],t._3.fieldOverrides(f.startPropertyCode).get("isReadOnly").asInstanceOf[Option[Boolean]],t._3.fieldOverrides(f.endPropertyCode).get("isReadOnly").asInstanceOf[Option[Boolean]],t._3.isDeleteNotAllowed ))
               }
             }
          }
        }).flatten
      }

    new DataViewTable(data.columns, data.binaryItems, dataViewRows,relations=relations,detailGrids = detailData,windowGridId = windowGridId)
  }

  /**
   * получение координат для Диаграмм
   * @param nodeId
   * @param treeVar
   * @param data
   * @return
   */
  private def getObjectCoordinates(nodeId:String,treeVar:String,data:DataTable):Map[String,ObjectPosition]={
    if(data.data.length == 0) return Map[String,ObjectPosition]()
    val hash = if(nodeId!= null) nodeId + "__" + treeVar else ""
    val idIndex = data.columns.indexWhere(col => (col == "ID"))
    val ids = "(" + data.data.map(f => "'"+f(idIndex)+"'").mkString(",")+")"
    val query = "select x,y,width,height,ID,ObjectID from _ViewDiagramObjectPosition where FilterHash = '" + hash + "' and ObjectID in" +ids
    connectionProvider.withConnection {
      connection =>
          val statement = connection.createStatement()
          val dbResult = statement.executeQuery(query)
          val reportFieldsList = new ArrayBuffer[ObjectPosition]
          while(dbResult.next()){
            val x = dbResult.getFloat("x")
            val y =  dbResult.getFloat("y")
            val width = dbResult.getFloat("width") // Option( dbResult.getObject("width")).map(f => new Double(f)).getOrElse(1.0)
            val height =dbResult.getFloat("height") //Option( dbResult.getObject("height")).map(f => new Double(f)).getOrElse(1.0)
            val id = dbResult.getString("ID")
            val objectId = dbResult.getString("ObjectID")
            reportFieldsList += ObjectPosition(x,y,width,height,id,objectId,true)
          }
          reportFieldsList.map(f => f.objectId -> f).toMap
        }
  }

  def checkColumnNameForSystemInfo(colName:String):Boolean={
    colName.startsWith("_Color")||colName.startsWith("$Color") || colName.endsWith("$")
  }

  def getCardDataView(recordId: String,viewCardId:String = null): (DataViewTable, Seq[StatusWindowGrid],Seq[StatusSection]) = {

    val data = getCardData(recordId)
    val cardMeta = DBProfiler.profile("cardMeta",execStatProvider,true) { metaFormProvider.getMetaCard(recordId,viewCardId) }
    // переопределения настроек и видимости гридов по статусу записи карточки
    val statusGrids =  metaFormProvider.getStatusWindowGrids()
    val recordStatusId = defineStatusFromData(data.data.head,data.columns)
    val statusDefaultTabId = metaClassProvider.getStatusDescription(recordStatusId).map(f=>f.defaultTabId).getOrElse(null)
    val refGridsSettings =  Option(cardMeta.refGrids).getOrElse(Seq()).map(g => {
      statusGrids.find(s => s.windowGridId == g.windowGridId && s.statusId == recordStatusId).orNull
    }).filterNot(s => s == null)
    val statusSections =  metaFormProvider.getStatusSections(recordStatusId,cardMeta.viewCardId)
    val dataViewRows = getDataViewForData(data,cardMeta.viewCardId,cardMeta.fields,true)
    (new DataViewTable(data.columns, data.binaryItems, dataViewRows,statusDefaultTabId),
      refGridsSettings,statusSections)
  }

  /**
   * Накладывает переопределения в статусе и переопределения в зависимости от данных, раскрашивает объекты
   * @param data
   * @param viewId
   * @param fields
   * @param isCard
   * @param isDiagram
   * @param objectsPositions
   * @return
   */
  def getDataViewForData(data: DataTable, viewId: String, fields: Seq[Metafield], isCard: Boolean, isDiagram: Boolean = false, objectsPositions: Map[String, ObjectPosition] = Map()): Seq[DataViewRow] = {
    DBProfiler.profile("getDataViewForData",execStatProvider,true) {
      val colorator: Colorator = if (!isCard) new Colorator else null
      val currentDomain = contextProvider.getContext.domainId
      val idColumnPos = data.columns.indexOf("ID")
      /*  используется для диаграммы*/
      val maxYposition:Double = if(objectsPositions!= null && !objectsPositions.isEmpty ) objectsPositions.maxBy(_._2.y)._2.y else -1.0
      var count :Int = -1
      val number = math.ceil(math.sqrt(data.data.length)).toInt
      // получить переопределения в статусе
      val statusFieldRedefinitionSeq = metaFormProvider.getStatusFieldRedefinitions(viewId)
      val statusFieldRedefinitions = statusFieldRedefinitionSeq.map(p => (p.viewFieldCode, p.statusId) -> p).toMap
      //получить переопределения в зависимости от данных
      val fieldConditionalRedefinitions = metaClassProvider.getConditionViewField()
      val fieldIdMap = fields.map(f => f.getCodeName -> f.getViewFieldId).toMap
      val conditionalRedefinitions = (for (
        colName <- data.columns;
        viewFieldId = fieldIdMap.get(colName).getOrElse("");
        cp = fieldConditionalRedefinitions.get(viewFieldId).map(_.data).getOrElse(Seq())
      ) yield colName -> cp).toMap

      data.data.map(r => {
        val fieldValues = (data.columns zip r).toMap
        // Определяем цвет строки и ячеек до обработки невидимых ячеек
        val (rowColor, cellColors) = if (isCard) (null, Array.ofDim[String](r.length))
        else DBProfiler.profile("colorize record", execStatProvider, true) {
          colorator.colorize(r, data.columns)
        }
        val domain = fieldValues("_Domain")
        val statusId = fieldValues("_StatusID").asInstanceOf[String]
        val id  = fieldValues("ID").asInstanceOf[String]
        // Считаем переопределения по статусу
        val statusIsEditNotAllowed = metaClassProvider.getStatusDescription(statusId).map(f => f.isEditNotAllowed).getOrElse(false)
        val statusIsDeletedNotAllowed = metaClassProvider.getStatusDescription(statusId).map(f => f.isDeleteNotAllowed).getOrElse(false)
        //данные из другого домена доступны только для чтения
        val domainIsReadOnly: Boolean = currentDomain != domain
        val domainIsDeletedNotAllowed = currentDomain != domain
        //запрет на редактирование в свойстве статуса или из другого домена
        val systemReadonlyMap: Map[String, Any] = if (statusIsEditNotAllowed || domainIsReadOnly || !data.perm(id)) Map("isReadOnly" -> true) else Map()
        //запрет на удаление записей в опрделенных статусах и из других доменов
        val systemDeleteNotAllowed = if (statusIsDeletedNotAllowed || domainIsDeletedNotAllowed || !data.perm(id)) Option(true) else None

        val statusProps: Map[String, Seq[FieldProperties]] = data.columns.map(colName => {
          val tmp = if (checkColumnNameForSystemInfo(colName)) Map[String, Any]() else statusFieldRedefinitions.get(colName -> statusId).map(f => f.redefinitions).getOrElse(Map()) ++ systemReadonlyMap

          colName -> Seq(FieldProperties(priority = 10, properties = tmp))
        }
        ).toMap

        val config = configProvider.create()
        // создаем объект
        val instance = entityFactory.createEntityWithData(fieldValues)

        // вычисляем переопределения свойств полей по доп. набору переопределений
        val acceptedProperties = conditionalRedefinitions.mapValues(_.filter(p => {
          DataType.boolValueFromValue(expressionEvaluator.evaluate(p.condition, config, Map("this" -> instance)))
        }))

        val condProps: FieldPropertySeqMap = acceptedProperties.mapValues(_.map(p => {
          val map = p.redefinitions.mapValues(f => {
            expressionEvaluator.evaluate(f, config, Map("this" -> instance)) match {
              case sv: SimpleValue => sv.data
              case _ => null
            }
          })

          FieldProperties(priority = p.priority, properties = map.toMap)
        }))
        val props = mergePropertyMaps(data, statusProps, condProps)

        val overriding = buildOverriding(data, props)
        // Заполняем невидимые ячейки пустой строкой
        val handledRow = data.columns.zip(r).map(_ match {
          case (rcode, rval) => {
            val propMap = overriding(rcode)
            val isVisible = propMap.get("isVisible")
            val newVal = if (isVisible.isDefined) {
              val maybeVisible = isVisible.get.asInstanceOf[Boolean]
              if (maybeVisible) rval else ""
            } else rval
            newVal
          }
        }).toArray
        val statusMenuRedefinitions = metaFormProvider.getStatusMenuRedefinitions(statusId)
        if (!isDiagram) new DataViewRow(handledRow, overriding, rowColor, cellColors, systemDeleteNotAllowed,menuOverrides = statusMenuRedefinitions)
        else {
          count += 1
          val id = handledRow(idColumnPos).asInstanceOf[String]
          val position = if (objectsPositions.contains(id)) objectsPositions(id)
          else ObjectPosition(1.0 + 2.0 * (count % number), maxYposition + 2.0 + 2.0 * (count / number), 1.0, 1.0, null, id, false)
          new DataViewRow(handledRow, overriding, rowColor, cellColors, systemDeleteNotAllowed, position,menuOverrides = statusMenuRedefinitions)
        }

      })
    }
  }


  private type FieldPropertySeqMap = Map[String, Seq[DataReaderServiceImpl.this.type#FieldProperties]]
  private type FieldPropertyValueMap = Map[String, Map[String, Any]]

  private def mergePropertyMaps(data: DataTable, propMaps: FieldPropertySeqMap*): FieldPropertySeqMap = {
    data.columns.map(colName => {
      // из каждого набора свойств выбираем свойства, соответствующие колонке
      val columnsPropGroups = propMaps.map(_.getOrElse(colName, Seq()))
      // складываем наборы свойств в одну последовательность
      val mergedProperties = columnsPropGroups.flatten
      // сортируем полученную последовательность по приоритету от большего к меньшему
      val sortedProps = mergedProperties.sortBy(_.priority)(Ordering[Int].reverse)

      colName -> sortedProps
    }).toMap
  }

  private def buildOverriding(data: DataTable, props: FieldPropertySeqMap): FieldPropertyValueMap = {
    data.columns.map(colName => {
      val p = props(colName)
      val map = new scala.collection.mutable.HashMap[String, Any]

      // boolean overriding
      if (p.exists(_.properties.contains("isMasked"))) {
        val isReadOnlyValue = p.exists(_.properties.getOrElse("isMasked", false).asInstanceOf[Boolean])
        map += "isMasked" -> isReadOnlyValue
      }
      if (p.exists(_.properties.contains("isReadOnly"))) {
        val isReadOnlyValue = p.exists(_.properties.getOrElse("isReadOnly", false).asInstanceOf[Boolean])
        map += "isReadOnly" -> isReadOnlyValue
      }
      val isVisibleContains = p.exists(_.properties.contains("isVisible"))
      if (isVisibleContains) {
        val isVisibleValue = p.forall(_.properties.getOrElse("isVisible", true).asInstanceOf[Boolean])
        map += "isVisible" -> isVisibleValue
      }
      val isDropDownNotAllowedContains = p.exists(_.properties.contains("isDropDownNotAllowed"))
      if (isDropDownNotAllowedContains) {
        val isDropDownNotAllowedValue = p.exists(_.properties.getOrElse("isDropDownNotAllowed", false).asInstanceOf[Boolean])
        map += "isDropDownNotAllowed" -> isDropDownNotAllowedValue
      }
      val isJoinMaskContains = p.exists(_.properties.contains("isJoinMask"))
      if (isJoinMaskContains) {
        val isJoinMaskValue = p.exists(_.properties.getOrElse("isJoinMask", false).asInstanceOf[Boolean])
        map += "isJoinMask" -> isJoinMaskValue
      }

      // string overriding
      val editorTypeContains = p.exists(_.properties.contains("editorType"))
      if (editorTypeContains) {
        val editorTypeValue = p.map(_.properties.get("editorType")).filter(_.isDefined).head.get.asInstanceOf[String]
        map += "editorType" -> editorTypeValue
      }
      val extInfoContains = p.exists(_.properties.contains("extInfo"))
      if (extInfoContains) {
        val extInfoValue = p.map(_.properties.get("extInfo")).filter(_.isDefined).head.get.asInstanceOf[String]
        map += "extInfo" -> extInfoValue
      }
      val maskContains = p.exists(_.properties.contains("mask"))
      if (maskContains) {
        val maskValue = p.map(_.properties.get("mask")).filter(_.isDefined).head.get.asInstanceOf[String]
        map += "mask" -> maskValue
      }
      val defaultFormGridIDContains = p.exists(_.properties.contains("defaultFormGridID"))
      if (defaultFormGridIDContains) {
        val defaultFormGridIDValue = p.map(_.properties.get("defaultFormGridID")).filter(_.isDefined).head.get.asInstanceOf[String]
        map += "defaultFormGridID" -> defaultFormGridIDValue
      }
      val formatContains = p.exists(_.properties.contains("format"))
      if (formatContains) {
        val formatValue = p.map(_.properties.get("format")).filter(_.isDefined).head.get.asInstanceOf[String]
        map += "format" -> formatValue
      }
      val sortOrderContains = p.exists(_.properties.contains("sortOrder"))
      if (sortOrderContains) {
        val sortOrderValue = p.map(_.properties.get("sortOrder")).filter(_.isDefined).head.get.asInstanceOf[String]
        map += "sortOrder" -> sortOrderValue
      }
      val captionContains = p.exists(_.properties.contains("caption"))
      if (captionContains) {
        val captionValue = p.map(_.properties.get("caption")).filter(_.isDefined).head.get.asInstanceOf[String]
        map += "caption" -> captionValue
      }
      val groupIdContains = p.exists(_.properties.contains("groupId"))
      if (groupIdContains) {
        val groupIdValue = p.map(_.properties.get("groupId")).filter(_.isDefined).head.get.asInstanceOf[String]
        map += "groupId" -> groupIdValue
      }

      colName -> map.toMap
    }).toMap
  }

  /**
   * Данные сущности для формы-карточки
   */
  def getCardData(recordId: String,isFull:Boolean = false): DataTable = {
    val obj = entityFactory.createEntity(recordId)
    val perm = if(obj.getDomain == -2) Map(PermissionType.ReadExec ->true, PermissionType.Write ->(contextProvider.getContext.domainId == -2))
    else permissionProvider.getDataPermission(obj)
    if(!perm(PermissionType.ReadExec)) throw new DataAccessError(recordId)
    connectionProvider.withConnection {
      connection =>
        val dbResult = dataOperation {
          val spName = if(isFull)   "[_Object].[GetViewCardDataDS]" else  "[_Object].[GetViewCardData]"
          DB.dbExec(connection, spName, contextProvider.getContext.sessionId,
            List("RecordID" -> recordId,"DNRoleID"->contextProvider.getContext.roleId),execStatProvider)
        }

        val (dataBuffer, columnSeq, binaries) = dataOperation {
          val meta = dbResult.getMetaData()

          val columnSeq = for (i <- 1 to meta.getColumnCount())
          yield meta.getColumnName(i)
          def isBlob(i: Int) = {
            val columnType = meta.getColumnType(i)
            blobTypes contains columnType
          }
          val binaries = for (i <- 1 to meta.getColumnCount() if isBlob(i)) yield i

          def fetchItem = (i: Int) =>
            if (isBlob(i)) dbResult.getBytes(i).asInstanceOf[Any]
            else dbResult.getObject(i).asInstanceOf[Any]

          val dataBuffer = if (dbResult.next()) {
            val dataArr = Range(1, meta.getColumnCount() + 1)
              .map(fetchItem)
              .toArray
            Seq(dataArr)
          } else Seq()
          (dataBuffer, columnSeq, binaries)
        }

        if (dataBuffer.size > 1)
          throw new DataError("Ошибка в данных. " +
            "Для одного идентификатора записи должено быть не более одного кортежа данных." +
            "Сейчас их " + dataBuffer.size.toString + ".")

        if (dataBuffer.size == 0)
          throw new DataError("Запись не существует или находится в другом домене")

        new DataTable(columnSeq, binaries, dataBuffer,Map(recordId-> perm(PermissionType.Write)))
    }
  }

  def getISFDataView(
                      recordId: String,
                      propertyCode: String): DataViewTable = {
    val data = connectionProvider.withConnection {
      connection =>
        dataOperation {
          val statement = connection.prepareStatement(DataQuery.ISFQuery)
          statement.setString(1, recordId)
          statement.setString(2, propertyCode)
          val dbResult = statement.executeQuery()
          val meta = dbResult.getMetaData()
          val columnSeq = for (i <- 1 to meta.getColumnCount())
          yield meta.getColumnName(i)

          def fetchItem = (i: Int) =>  dbResult.getObject(i).asInstanceOf[Any]
          val dataBuffer = scala.collection.mutable.Buffer[Array[Any]]()
          while (dbResult.next()) {
            val dataArr = (1 to columnSeq.length map fetchItem).toArray
            dataBuffer += dataArr
          }

          val column =  columnSeq.indexOf("ID")
          val tmp = dataBuffer.toSeq
          val writePermMap =  tmp.map(f => f(column).asInstanceOf[String] -> true).toMap
          new DataTable(columnSeq,Seq(),tmp,writePermMap)
        }
    }
    val dataViewRows = data.data.map(f => new DataViewRow(f,Map(),null, Array.ofDim[String](data.columns.length)))
    new DataViewTable(data.columns, data.binaryItems, dataViewRows)
  }
  def getObjectClass(objectId:String):String={
    connectionProvider.withConnection {
      connection =>
        dataOperation {
          val statement = connection.prepareStatement("select Cl.code from _Object Obj inner join _Meta_class Cl on Cl.ID = Obj._classID where Obj.ID = ?")
           statement.setString(1, objectId)
          val resultSet = DBProfiler.profile("query getObjectClass select Cl.code from _Object Obj inner join _Meta_class Cl on Cl.ID = Obj._classID",execStatProvider) {
            statement.executeQuery()
          }
          val result = if (resultSet != null && resultSet.next()) {
            resultSet.getString(1)
          } else throw new NotFoundError("Объект " + objectId + " отсутствует в базе")
          result
        }
    }
  }
  def getBFID(bfCode:String):String={
    connectionProvider.withConnection {
      connection =>
        dataOperation {
          val statement = connection.prepareStatement("select ID from _Meta_BusinessFunction  where Code = ?")
          statement.setString(1, bfCode)
          val resultSet = statement.executeQuery()
          val result = if (resultSet != null && resultSet.next()) {
            resultSet.getString(1)
          } else null
          result
        }
    }
  }

  def getDisplayName(objectId:String) :Option[String]={
    connectionProvider.withConnection {
      connection =>
        dataOperation {
          val statementText =  "select _Name from _Object.data where ID = ?"
          val statement = connection.prepareStatement(statementText)
          statement.setString(1, objectId)
          val resultSet = statement.executeQuery()
          val result = if (resultSet != null && resultSet.next()) {
            // map "column_name -> value"
            resultSet.getString(1)
          } else null
          Option(result)
        }
    }
  }
  def getObjectData(classCode: String = null, objectId: String): Map[String, Any] = {
    connectionProvider.withConnection {
      connection =>
        dataOperation {
          val code = if (classCode==null) getObjectClass(objectId)   else classCode
          val statementText =
            "select * from [%s].[DataAll] where ID = ?".format(code)

          val statement = connection.prepareStatement(statementText)
          statement.setString(1, objectId)
          val resultSet = DBProfiler.profile("query getObjectData (select * from [%s].[DataAll])",execStatProvider) {
            statement.executeQuery()
          }

          val result = if (resultSet != null && resultSet.next()) {
            val resultMeta = resultSet.getMetaData
            val colCount = resultMeta.getColumnCount
            // map "column_name -> value"
            (1 to colCount).map(i =>
              resultMeta.getColumnName(i) -> resultSet.getObject(i)).toMap
          } else throw new NotFoundError("Объект " + objectId + " отсутствует в базе")

          result
        }
    }
  }


  def getCollectionByArrayName(arrayName:String,fieldValue:String,filters:Seq[Expression]=Seq(),context:Map[String,Value]=Map()):DataTable =
  {
    val (classCode,fieldCode) = metaClassProvider.getClassAndProperty(arrayName)
    getCollectionData(classCode,fieldCode,fieldValue)
  }

  def getCollectionData(classCode: String, fieldCode: String, fieldValue: String,filters:Seq[Expression]=Seq(),context:Map[String,Value]=Map()): DataTable = {
    connectionProvider.withConnection {
      connection =>
        val dbResult = dataOperation {
          val spName = String.format("[%s].[eEnumByDS_%s]", classCode, fieldCode)
          DB.dbExec(connection, spName, contextProvider.getContext.sessionId,
            List(fieldCode -> fieldValue,"DNRoleID"->contextProvider.getContext.roleId),execStatProvider)
        }

        val result = dataOperation {
          val meta = dbResult.getMetaData()

          val columnSeq = for (i <- 1 to meta.getColumnCount()) yield meta.getColumnName(i)
          def isBlob(i: Int) = {
            val columnType = meta.getColumnType(i)
            blobTypes contains columnType
          }
          val binaries = for (i <- 1 to meta.getColumnCount() if isBlob(i)) yield i

          def fetchItem = (i: Int) =>
            if (isBlob(i)) dbResult.getBytes(i).asInstanceOf[Any]
            else dbResult.getObject(i).asInstanceOf[Any]


          val dataBuffer = mutable.ArrayBuffer[Array[Any]]()
          while (dbResult.next()) {
            val dataArr = Range(1, meta.getColumnCount() + 1)
              .map(fetchItem)
              .toArray
            dataBuffer += dataArr
          }
          val idPosition = columnSeq.indexOf("ID")
          val buf = dataBuffer.toSeq
          new DataTable(columnSeq, binaries, buf,buf.map(f => f(idPosition).asInstanceOf[String] -> true).toMap)
    }

        result
    }
  }

  def getClassData(classId:String,filters:Seq[Expression],context:Map[String,Value]):DataTable={
    val gridId=metaFormProvider.getWindowGridForClass(classId)
    getGridData(gridId,null,null,null,null,null,null,null,null,filters,context,true)
  }
  def updateTreeVariablesWithSystemVariables(treeVariablesXml:String):String={
    val substr = if (treeVariablesXml==null || !treeVariablesXml.contains("<variables>"))""
    else treeVariablesXml.replaceFirst("<variables>","").replaceFirst("</variables>","")

    val systemVariables= contextProvider.getContext.getSystemVariables
    "<variables>" + substr + systemVariables.filter(_._2.asInstanceOf[SimpleValue].data!=null).map(el=> Elem(null,el._1,null,TopScope,Text(el._2.toString))).mkString+ "</variables>" // )
  }

  def getClassDataFromAllDomain(classCode:String):Seq[Map[String,Any]]={
    connectionProvider.withConnection {
      connection =>
        dataOperation {
          val statementText =  "select * from " + classCode
          val statement = connection.prepareStatement(statementText)
          val dbResult = statement.executeQuery()
          val meta = dbResult.getMetaData()
          val columnCount = meta.getColumnCount

          def fetchItem = (i: Int) => meta.getColumnName(i) -> dbResult.getObject(i).asInstanceOf[Any]

          val dataBuffer = scala.collection.mutable.Buffer[Map[String,Any]]()
          while (dbResult.next()) {
            val dataArr = (1 to columnCount map fetchItem)
            dataBuffer += dataArr.toMap
          }
          dataBuffer.toSeq
        }
    }
  }

}