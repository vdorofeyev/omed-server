package omed.model

import omed.db._
import omed.system.{Context=>OmedContext, ContextProvider}
import ru.atmed.omed.beans.model.meta._
import com.google.inject.Inject
import com.hazelcast.core.Hazelcast
import collection.mutable.{ListBuffer, ArrayBuffer}
import scala.collection.JavaConversions._
import java.util.logging.Logger
import omed.errors._
import omed.auth.{PermissionType, PermissionProvider}
import omed.cache.{CommonCacheService, ExecStatProvider, DomainCacheService}
import omed.data.{FieldColorRule, ColorRuleSeq, ColorRule}
import omed.forms._
import java.sql.Statement
import ru.atmed.omed.beans.model.meta.ClassValidationRule
import ru.atmed.omed.beans.model.meta.StatusMenu
import ru.atmed.omed.beans.model.meta.ObjectStatusTransitionSeq
import ru.atmed.omed.beans.model.meta.FieldValidationRule
import ru.atmed.omed.beans.model.meta.ObjectStatusTransition
import ru.atmed.omed.beans.model.meta.MetaObjectStatus
import omed.data.ColorRuleSeq
import ru.atmed.omed.beans.model.meta.MetaObjectStatusSeq

import ru.atmed.omed.beans.model.meta.ClassValidationRuleSeq
import ru.atmed.omed.beans.model.meta.ModuleInDomainSeq
import omed.lang.eval.DBUtils

class MetaClassProviderImpl extends MetaClassProvider with DataAccessSupport {

  @Inject
  var connectionProvider: ConnectionProvider = null
  @Inject
  var contextProvider: ContextProvider = null
  @Inject
  var domainCacheService: DomainCacheService = null
  @Inject
  var metaObjectCacheManager: MetaObjectCacheManager = null
  @Inject
  var permissionProvider: PermissionProvider = null
  @Inject
  var execStatProvider : ExecStatProvider = null
  @Inject
  var metaQueryProvider:MetaDBProvider = null
  @Inject
  var commonCacheService:CommonCacheService = null

  private val logger = Logger.getLogger(classOf[MetaClassProviderImpl].getName())

  def getClassByRecord(objectId: String): MetaObject = {
    connectionProvider.withConnection {
      connection =>

        val statementText = """
select o._ClassID as ClassID
from _Object.Data o
inner join _Meta_Class.Data c
on o._ClassID = c.ID
where o.ID = ?"""

        val resultSet = dataOperation {
          val statement = connection.prepareStatement(statementText)
          statement.setString(1, objectId)
          DBProfiler.profile("query GetClassByRecord (select from _Object.Data, _Meta_Class.Data)",execStatProvider) {
            statement.executeQuery() }
        }
        if (resultSet == null || !resultSet.next())
          throw new MetaModelError("Ошибка в метаданных: для записи: " + objectId + " нет метаописания.")
        dataOperation {
          getClassMetadata(
            resultSet.getString(1)
          )
        }
    }
  }

  /**
   * Получить метаописание классов
   */
  def getAllClassesMetadata(): Seq[MetaObject] = {
    loadClassesToCache
    metaObjectCacheManager.getAllClasses
  }
  def getAllParents(classId:String):Set[String]={
       if(classId!=null)
          getAllParents(getClassMetadata(classId).parentId) ++ Set(classId)
       else Set()

  }
  def getAllClasses(): Map[String, MetaObject] = {
    DBProfiler.profile("getAllClasses",execStatProvider,true) {
      loadClassesToCache
      metaObjectCacheManager.getAllClasses.map(c => c.code -> c).toMap
    }
  }

  /**
   * Получить метаописание класса
   */
  def getClassMetadata(classId: String): MetaObject = {
    DBProfiler.profile("getClassMetadata",execStatProvider,true) {
      loadClassesToCache
      val result = metaObjectCacheManager.get(classId)
      if (result == null) throw new NoSuchElementException("Не найден класс" + classId)
      result
    }

  }
  def getClassAndProperty(arrayName:String):(String,String)={
    connectionProvider.withConnection {
      connection =>

        val statementText = """
                              |select
                              |    cgp.ParameterCode as PropertyCode
                              |    , c2.Code as ClassCode
                              |from _Meta_ViewCardGridParameter cgp
                              |join _Meta_ViewCardGrid cg on cg.id = cgp.ViewCardGridID
                              |join _Meta_WindowGrid.data g on g.id = cg.WindowGridID
                              |join _Meta_Class c2 on c2.ID = g.ClassID
                              |where cgp.ArrayName = ?
                              |""".stripMargin

        val resultSet = dataOperation {
          val statement = connection.prepareStatement(statementText)
          statement.setString(1, arrayName)
          DBProfiler.profile("query getClassAndProperty for ArrayName",execStatProvider) {
            statement.executeQuery() }
        }
        if (resultSet == null || !resultSet.next())
          throw new MetaModelError("Ошибка в метаданных: для записи:" + arrayName  +" нет метаописания.")

       val classCode = resultSet.getString("ClassCode")
       val fieldCode = resultSet.getString("PropertyCode")
       (classCode,fieldCode)
    }
  }
  def getClassByCode(code: String): MetaObject = {
     DBProfiler.profile("getClassByCode",execStatProvider,true) {
    loadClassesToCache
    val result = metaObjectCacheManager.getByCode(code)
    result
    }
  }
  private def getAllStatuses():Seq[MetaObjectStatus]={
    loadClassesToCache
    val keyAllStatuses = "keyAllStatuses"
    val cached = domainCacheService.get(classOf[MetaObjectStatusSeq], keyAllStatuses)
    if (cached != null) cached.data
    else Seq()
  }

  private def loadClassesToCache() {
    // double-checked locking
      new LockProvider().locked("omed.model.MetaClass") {
        if(metaObjectCacheManager.isEmpty())   {
          DBProfiler.profile("cache metaclasses",execStatProvider,true){
              val (classes,statuses) = loadClassMetadata()
              metaObjectCacheManager.drop()
              classes.foreach(x => metaObjectCacheManager.put(x.id, x))
              statuses.foreach(x=>domainCacheService.put(classOf[MetaObjectStatus],x.id,x))
              domainCacheService.put(classOf[MetaObjectStatusSeq],"keyAllStatuses",MetaObjectStatusSeq(statuses))
          }
        }
     }
  }

  /**
   * Получить метаописание классов
   */
  private def loadClassMetadata(): (Seq[MetaObject],Seq[MetaObjectStatus]) = {

        val metaObjects  = DBProfiler.profile("load classes from DB",execStatProvider,true)  {  metaQueryProvider.loadClassesFromDb  }
        val (metaFieldGroups,metaObjectBackRefsMap) = DBProfiler.profile("load fields from DB",execStatProvider,true)  { metaQueryProvider.loadFieldsFromDb(metaObjects)    }
        val metaObjectsIndex = metaObjects.map(obj => obj.id -> obj).toMap



        val statusList = DBProfiler.profile("load statuses from DB",execStatProvider,true)  { metaQueryProvider.loadStatusesFromDb  }
        // создаем ссылки на коллекции для потомков
       def getAllParentBackRefs(metaObject:MetaObject):Seq[ArrayField]={
         val parentsBackRef = if(metaObject.parentId!=null)getAllParentBackRefs(metaObjectsIndex(metaObject.parentId)) else Seq()
         metaObjectBackRefsMap(metaObject.code) ++ parentsBackRef
       }
        val metaObjectBackRefsMapWithParent=  metaObjects.map(p=> p.code-> getAllParentBackRefs(p)).toMap
        val resultObjects = metaObjects.map(obj => {
          val objFields = metaFieldGroups.get(obj.id).map(_.toList)
          val backRefs = metaObjectBackRefsMapWithParent(obj.code).toList
          if (objFields.isEmpty && backRefs.isEmpty)
            obj
          else
            obj.copy(
              fields = objFields.getOrElse(List()),
              backReferenceFields = backRefs
            )
        }).toList

        (resultObjects,statusList)

  }

  /**
   * Получить правила переопределения метаданных
   */
  def getConditionViewField(): Map[String, ConditionViewFieldSeq] = {

    if (!domainCacheService.isEmpty(classOf[ConditionViewFieldSeq])) {
      return domainCacheService.map(classOf[ConditionViewFieldSeq])
    }

    val lock = Hazelcast.getLock("omed.forms.ConditionViewFieldCard")
    lock.lock()
    DBProfiler.profile("cache ConditionViewFieldCard",execStatProvider,true){
      try {
        if (domainCacheService.isEmpty(classOf[ConditionViewFieldSeq])) {
          logger.fine("load meta redefinition from db")
          val loaded = loadConditionViewFieldCard() ++ loadConditionViewFieldGrid()
          val list = if (loaded.isEmpty) Map("" -> List()) else loaded
          list.foreach(c => domainCacheService.put(classOf[ConditionViewFieldSeq], c._1, ConditionViewFieldSeq(c._2)))
        } else {
          logger.finer("get meta redefinition from cache")
        }

        domainCacheService.map(classOf[ConditionViewFieldSeq])
      } finally {
        lock.unlock()
      }
    }
  }

  def loadConditionViewFieldCard(): Map[String, Seq[ConditionViewField]] = {
    connectionProvider.withConnection {
      connection =>

        val statement = dataOperation {
          DB.prepareStatement(connection, "[_Meta].[GetConditionViewFieldCard]",
            contextProvider.getContext.sessionId, List())
        }

        val resultAvailable = dataOperation {
          DBProfiler.profile("[_Meta].[GetConditionViewFieldCard]",execStatProvider) { statement.execute() }
        }

        val conditionViewFieldList = new scala.collection.mutable.HashMap[String, scala.collection.mutable.ArrayBuffer[ConditionViewField]]

        if (resultAvailable) {
          dataOperation {
            var dbResult =  statement.getResultSet()

            while (dbResult.next()) {
              val id = dbResult.getString("ID")
              val condition = dbResult.getString("ConditionString")
              val name = dbResult.getString("Name")
              val priority = Option(dbResult.getInt("Priority")).getOrElse(0)
              val viewFieldID = dbResult.getString("ViewFieldID")

              val defaultFormGridIDSourceExp = dbResult.getString("DefaultFormGridIDSourceExp")
              val dropDownNotAllowedSourceExp = dbResult.getString("DropDownNotAllowedSourceExp")
              val editorTypeSourceExp = dbResult.getString("EditorTypeSourceExp")
              val extInfoSourceExp = dbResult.getString("ExtInfoSourceExp")
              val formatSourceExp = dbResult.getString("FormatSourceExp")
              val visibleSourceExp = dbResult.getString("VisibleSourceExp")
              val joinMaskSourceExp = dbResult.getString("JoinMaskSourceExp")
              val maskSourceExp = dbResult.getString("MaskSourceExp")
              val readOnlySourceExp = dbResult.getString("ReadOnlySourceExp")
              val captionSourceExp = dbResult.getString("CaptionSourceExp")
              val sortOrderSourceExp = dbResult.getString("SortOrderSourceExp")
              val tabSourceExp = dbResult.getString("TabSourceExp")

              val redefinitions = Map(
                "isReadOnly"->readOnlySourceExp,
                "isVisible"->visibleSourceExp,
                "isDropDownNotAllowed"->dropDownNotAllowedSourceExp,
                "editorType"->editorTypeSourceExp,
                "extInfo"->extInfoSourceExp,
                "mask"->maskSourceExp,
                "defaultFormGridID"->defaultFormGridIDSourceExp,
                "isJoinMask"->joinMaskSourceExp,
                "format"->formatSourceExp,
                "sortOrder"->sortOrderSourceExp,
                "caption"->captionSourceExp,
                "groupId"->tabSourceExp
              ).filter(f => f._2 != null)
              if(viewFieldID!= null && condition !=null) {
                if (!conditionViewFieldList.contains(viewFieldID))
                  conditionViewFieldList.put(viewFieldID, scala.collection.mutable.ArrayBuffer.empty[ConditionViewField])

                conditionViewFieldList(viewFieldID) +=
                  ConditionViewField(id,condition,name,priority,viewFieldID,redefinitions)
              }
            }
          }
        }

        val xyz = conditionViewFieldList.mapValues(_.toSeq.sortBy(_.priority)).toMap

        xyz
    }
  }

  def loadConditionViewFieldGrid(): Map[String, Seq[ConditionViewField]] = {
    connectionProvider.withConnection {
      connection =>

        val statement = dataOperation {
          DB.prepareStatement(connection, "[_Meta].[GetConditionViewFieldGrid]",
            contextProvider.getContext.sessionId, List())
        }

        val resultAvailable = dataOperation {
          DBProfiler.profile("[_Meta].[GetConditionViewFieldGrid]",execStatProvider) { statement.execute() }
        }

        val conditionViewFieldList = new scala.collection.mutable.HashMap[String, scala.collection.mutable.ArrayBuffer[ConditionViewField]]

        if (resultAvailable) {
          dataOperation {
            var dbResult = statement.getResultSet()

            while (dbResult.next()) {
              val id = dbResult.getString("ID")
              val condition = dbResult.getString("ConditionString")
              val name = dbResult.getString("Name")
              val priority = Option(dbResult.getInt("Priority")).getOrElse(0)
              val viewFieldID = dbResult.getString("ViewFieldID")

              val defaultFormGridIDSourceExp = dbResult.getString("DefaultFormGridIDSourceExp")
              val dropDownNotAllowedSourceExp = dbResult.getString("DropDownNotAllowedSourceExp")
              val editorTypeSourceExp = dbResult.getString("EditorTypeSourceExp")
              val extInfoSourceExp = dbResult.getString("ExtInfoSourceExp")
              val formatSourceExp = dbResult.getString("FormatSourceExp")
              val visibleSourceExp = dbResult.getString("VisibleSourceExp")
              val joinMaskSourceExp = dbResult.getString("JoinMaskSourceExp")
              val maskSourceExp = dbResult.getString("MaskSourceExp")
              val readOnlySourceExp = dbResult.getString("ReadOnlySourceExp")
              val redefinitions = Map(
                "isReadOnly"->readOnlySourceExp,
                "isVisible"->visibleSourceExp,
                "isDropDownNotAllowed"->dropDownNotAllowedSourceExp,
                "editorType"->editorTypeSourceExp,
                "extInfo"->extInfoSourceExp,
                "mask"->maskSourceExp,
                "defaultFormGridID"->defaultFormGridIDSourceExp,
                "isJoinMask"->joinMaskSourceExp,
                "format"->formatSourceExp).filter(f => f._2 != null)

              if (!conditionViewFieldList.contains(viewFieldID))
                conditionViewFieldList.put(viewFieldID, scala.collection.mutable.ArrayBuffer.empty[ConditionViewField])

              conditionViewFieldList(viewFieldID) +=
                ConditionViewField(id,condition,name,priority,viewFieldID,redefinitions)
            }
          }
        }

        val xyz = conditionViewFieldList.mapValues(_.toSeq.sortBy(_.priority)).toMap

        xyz
    }
  }
  /**
   * Получить правила окрашивания
   */
    def getColorRules(classId:String): Seq[ColorRule] ={
      val allRulesKey = "allColorRules"
      new LockProvider().locked("ru.atmed.omed.beans.model.meta.ColorRule"){
        if (domainCacheService.isEmpty(classOf[ColorRuleSeq])) {
          logger.fine("load coloration rules from db")
          val loaded = metaQueryProvider.loadColorRules
          domainCacheService.put(classOf[ColorRuleSeq],allRulesKey,ColorRuleSeq(loaded))
        }
        val cached = domainCacheService.get(classOf[ColorRuleSeq],classId)
        if(cached != null) {
          cached.data
        }
        else{
          val parents = getAllParents(classId)
          val result = domainCacheService.get(classOf[ColorRuleSeq],allRulesKey).data.filter(t =>  parents.contains(t.classId))
          domainCacheService.put(classOf[ColorRuleSeq],classId,ColorRuleSeq(result))
          result
        }
        Option(domainCacheService.get(classOf[ColorRuleSeq],classId)).map(_.data).getOrElse(Seq())
      }
    }

  /**
   * @return List(<Наименование на форме карточки>, <ИД бизнес-функции>)
   */
  def getStatusMenu(recordId: String): List[StatusMenu] = {
    val menuItems = connectionProvider.withConnection {
      connection =>
        val dbResult = dataOperation {
          DB.dbExec(connection, "[_Meta].[GetStatusMenu]",
            contextProvider.getContext.sessionId,
            List(("RecordID", recordId)))
        }

        var statusMenuList = new ListBuffer[StatusMenu]
        dataOperation {
          while (dbResult.next()) {
            val name = dbResult.getString("Name")
            val bfId = dbResult.getString("BusinessFunctionID")
            val alignment = dbResult.getString("Alignment")
            val buttonPosition = dbResult.getString("ButtonPosition")
            val sectionId = dbResult.getString("SectionID")
            val row = dbResult.getInt("Row")
            val sortOrder = dbResult.getInt("SortOrder")
            val buttonGroupId = Option( dbResult.getObject("ButtonGroupID")).map(f=>f.toString).orNull
            statusMenuList +=StatusMenu(name = name,businessFunctionId = bfId,alignment = alignment,buttonPosition = buttonPosition, sectionId = sectionId,row = row,sortOrder = sortOrder,buttonGroupId = buttonGroupId)
          }
        }
        statusMenuList.toList
    }

    // проверить на наличие разрешений
    val result = menuItems.filter(m => permissionProvider.getMetaPermission(m.businessFunctionId)(PermissionType.ReadExec))
    result
  }

  def getClassStatusTransitions(classId: String): Seq[ObjectStatusTransition] = {
    if(domainCacheService.isEmpty(classOf[ObjectStatusTransitionSeq])){
      DBProfiler.profile("Cache status Transitions",execStatProvider,true){
        val transitionMap = metaQueryProvider.loadTransitionsFromDb
        transitionMap.foreach( f => domainCacheService.put(classOf[ObjectStatusTransitionSeq], f._1,ObjectStatusTransitionSeq( f._2)))
      }
    }
    val cachedData = domainCacheService.get(classOf[ObjectStatusTransitionSeq], classId)
    if(cachedData!=null) return cachedData.data else Seq()
  }

  def getClassStatusDiagramm(classId: String, diagrammId: String): Seq[MetaObjectStatus] = {
    val key = diagrammId

    val cached = domainCacheService.get(classOf[MetaObjectStatusSeq], key)
    if (cached != null)
      cached.data
    else {
         val allStatuses = getAllStatuses()
         val result = allStatuses.filter(p=>p.diagrammId == diagrammId)
         domainCacheService.put(classOf[MetaObjectStatusSeq],diagrammId,MetaObjectStatusSeq(result))
         result
    }
  }
  def getStatusDescription(statusId:String):Option[MetaObjectStatus]={
    if(statusId==null) Option(null)
    else{
      loadClassesToCache()
      val cached = domainCacheService.get(classOf[MetaObjectStatus],statusId)
      Option(cached)
    }
  }
  /**
   * Коллекция валидаторов для класса
   *
   * @return Seq[ClassValidationRule] - Map[<Ид_Класса>, <Список_Валидаторов>]
   */


  def getClassValidationRules(classId: String): Seq[ClassValidationRule] = {
    val allClassValidationKey = "AllClassValidationKey"
    val key = if (classId==null) allClassValidationKey else classId
    val cachedData = domainCacheService.get(classOf[ClassValidationRuleSeq], key)

    if (cachedData == null) {
      if (domainCacheService.isEmpty(classOf[ClassValidationRuleSeq])) {
        // fetch data from db
         DBProfiler.profile("cache class Validation Rule",execStatProvider,true){
          val storedData = getClassValidationRulesFromStorage
          // clear cache
          DBProfiler.profile("put class Validation Rule to Cache",execStatProvider,true){
          domainCacheService.drop(classOf[ClassValidationRuleSeq])

          storedData.foreach(g => g match {
            case (k, v) => domainCacheService.put(
              classOf[ClassValidationRuleSeq], k, ClassValidationRuleSeq(v))
          })
          domainCacheService.put(classOf[ClassValidationRuleSeq],allClassValidationKey,ClassValidationRuleSeq(storedData.map(f=>f._2).flatten.toSeq))
          }
        }
      }
      val result = domainCacheService.get(classOf[ClassValidationRuleSeq], key)
      Option(result).map(_.data)getOrElse(Seq())
    } else cachedData.data
  }

  def getClassValidationRulesFromStorage: Map[String, Seq[ClassValidationRule]] = {
    connectionProvider.withConnection {
      connection =>
        val statement = dataOperation {
          DB.prepareStatement(connection,
            "[_Meta].[GetValidationRules]",
            contextProvider.getContext.sessionId, List())
        }

        val resultAvailable = dataOperation {
          DBProfiler.profile("[_Meta].[GetValidationRules]",execStatProvider) { statement.execute() }
        }
        if (resultAvailable) {
          dataOperation {
            //dataOperation {

            // получить валидаторы классов
            var dbResult =  statement.getResultSet()

            val classValidatorsList = scala.collection.mutable.Buffer[ClassValidationRule]()
            DBProfiler.profile("build validation Rule",execStatProvider,true){
            while (dbResult.next()) {
              val classId = Option(dbResult.getObject("ClassID")).map(x => x.toString).orNull
              val name = dbResult.getString("Name")
              val id = dbResult.getString("ID")
              val falseMessage = dbResult.getString("FalseMessage")
              val condition = dbResult.getString("condition")
              val conditionString = dbResult.getString("ConditionString")
              val validationResultType = try {
                val vrt = dbResult.getString("ValidatorResultType")
                if (vrt == null || vrt == "")
                  ValidationResultType.Error
                else
                  ValidationResultType.aliases(vrt.toLowerCase())
              } catch {
                case _ => ValidationResultType.Error
              }
              val isEnabledInDomain = DBUtils.fromDbBoolean(dbResult.getString("IsEnabledInDomain"))
              val isUsedInStatus = DBUtils.fromDbBoolean(dbResult.getString("IsUsedInStatus"))

              classValidatorsList append ClassValidationRule(id, classId, name, conditionString, falseMessage,
                isEnabledInDomain, isUsedInStatus, validationResultType)
            }
            }
            classValidatorsList.toSeq.groupBy(_.classId)
          }
        } else Map.empty[String, Seq[ClassValidationRule]]
    }
  }

  /**
   * Коллекция валидаторов для поля
   *
   * @return Map[String, List[FieldValidationRule] ] - Map[<Ид_Поля>, <Список_Валидаторов>]
   */
  def getFieldValidationRules: Map[String, List[FieldValidationRule]] = {
    DBProfiler.profile("Коллекция валидаторов для поляв",execStatProvider,true){
      connectionProvider.withConnection {
        connection =>
          val statement = dataOperation {
            DB.prepareStatement(connection,
              "[_Meta].[GetValidationRules]",
              contextProvider.getContext.sessionId, List())
          }

          val resultAvailable = dataOperation {
            DBProfiler.profile("[_Meta].[GetValidationRules]",execStatProvider) { statement.execute() }
          }
          if (resultAvailable) {
            dataOperation {
              def getResultSet = if (statement.getMoreResults())
                statement.getResultSet()
              else throw new DataError("Not enough ResultSets")

              // получить валидаторы классов
              var dbResult = statement.getResultSet()

              // получить валидаторы полей
              dbResult = getResultSet
              val fieldValidationMap = {
                val fieldValidatorsList =
                  scala.collection.mutable.HashMap.empty[String, scala.collection.mutable.ArrayBuffer[FieldValidationRule]]
                while (dbResult.next()) {
                  val classId = Option(dbResult.getObject("ClassID")).map(x => x.toString).orNull
                  val propertyId = Option(dbResult.getObject("PropertyID")).map(x => x.toString).orNull
                  val propertyCode = dbResult.getString("PropertyCode")
                  val isEnabledInDomain = DBUtils.fromDbBoolean(dbResult.getString("IsEnabledInDomain"))
                  val name = dbResult.getString("Name")
                  val condition = dbResult.getString("condition")
                  val falseMessage = dbResult.getString("FalseMessage")
                  val validationResultType = try {
                    val vrt = dbResult.getString("ValidatorResultType")
                    if (vrt == null || vrt == "")
                      ValidationResultType.Error
                    else
                      ValidationResultType.aliases(vrt.toLowerCase())
                  } catch {
                    case _ => ValidationResultType.Error
                  }

                  val validationRules = {
                    val key = propertyCode + " " + classId
                    if (!fieldValidatorsList.contains(key))
                      fieldValidatorsList.put(key, scala.collection.mutable.ArrayBuffer.empty[FieldValidationRule])
                    fieldValidatorsList(key)
                  }

                  validationRules += new FieldValidationRule(classId, name, condition, falseMessage,
                    isEnabledInDomain,validationResultType ,propertyId, propertyCode)
                }
                fieldValidatorsList
              }.map(el => (el._1 -> el._2.toList)).toMap

              fieldValidationMap
            }
          } else Map.empty[String, List[FieldValidationRule]]

        // TODO: кешировать эти данные, после решения вопроса с инвалидацией кеша
      }
    }
  }

  /**
   * Получение коллекции валидаторов для статуса
   */
  def getStatusValidationRules: Map[String, StatusValidatorSeq] = {
    if (!domainCacheService.isEmpty(classOf[StatusValidatorSeq])) {
      return domainCacheService.map(classOf[StatusValidatorSeq])
    }

    val lock = Hazelcast.getLock("omed.model.StatusValidatorSeq")
    lock.lock()
    DBProfiler.profile("cache Status ValidationRule",execStatProvider,true){
      try {
        if (domainCacheService.isEmpty(classOf[StatusValidatorSeq])) {
          logger.fine("load status validation rules from db")
          val loaded = loadStatusValidationRules()
          val list = if (loaded.isEmpty) Map("" -> List()) else loaded
          list.foreach(c => domainCacheService.put(classOf[StatusValidatorSeq], c._1, StatusValidatorSeq(c._2)))
        } else {
          logger.finer("get status validation rules from cache")
        }

        domainCacheService.map(classOf[StatusValidatorSeq])
      } finally {
        lock.unlock()
      }
    }
  }


  def loadStatusValidationRules(): Map[String, Seq[StatusValidator]] = {
    connectionProvider.withConnection {
      connection =>

        val statement = dataOperation {
          DB.prepareStatement(connection, "[_Meta].[GetStatusValidationRules]",
            contextProvider.getContext.sessionId, List())
        }

        val resultAvailable = dataOperation {
          DBProfiler.profile("[_Meta].[GetStatusValidationRules]",execStatProvider) { statement.execute() }
        }

        val statusValidatorMap = new scala.collection.mutable.HashMap[String, scala.collection.mutable.ArrayBuffer[StatusValidator]]

        if (resultAvailable) {
          dataOperation {
            var dbResult = statement.getResultSet()

            while (dbResult.next()) {
              val statusId = dbResult.getString("StatusID")
              val validatorId = dbResult.getString("ValidatorID")

              if (!statusValidatorMap.contains(statusId))
                statusValidatorMap.put(statusId, scala.collection.mutable.ArrayBuffer.empty[StatusValidator])

              statusValidatorMap(statusId) += new StatusValidator(statusId, validatorId)
            }
          }
        }

        val xyz = statusValidatorMap.mapValues(_.toSeq).toMap

        xyz
    }
  }

  /**
   *  получение валидаторов БФ
   */
  def getBFValidationRules: Map[String, BFValidatorSeq] = {
    if (!domainCacheService.isEmpty(classOf[BFValidatorSeq])) {
      return domainCacheService.map(classOf[BFValidatorSeq])
    }

    val lock = Hazelcast.getLock("omed.model.BFValidatorSeq")
    lock.lock()

      try {
        if (domainCacheService.isEmpty(classOf[BFValidatorSeq])) {
          logger.fine("load BF validation rules from db")
          val loaded = loadBFValidationRules()
          val list = if (loaded.isEmpty) Map("" -> List()) else loaded
          list.foreach(c => domainCacheService.put(classOf[BFValidatorSeq], c._1, BFValidatorSeq(c._2)))
        } else {
          logger.finer("get BF validation rules from cache")
        }

        domainCacheService.map(classOf[BFValidatorSeq])
      } finally {
        lock.unlock()
      }

  }

  def loadBFValidationRules(): Map[String, Seq[BFValidator]] = {
    connectionProvider.withConnection {
      connection =>

        val statement = dataOperation {
          DB.prepareStatement(connection, "[_Meta].[GetStatusValidationRules]",
            contextProvider.getContext.sessionId, List())
        }

        val resultAvailable = dataOperation {
          DBProfiler.profile("[_Meta].[GetStatusValidationRules]",execStatProvider) { statement.execute() }
        }

        val bfValidatorMap = new scala.collection.mutable.HashMap[String, scala.collection.mutable.ArrayBuffer[BFValidator]]

        if (resultAvailable) {
          dataOperation {

            nextResultSet(statement)
            nextResultSet(statement)
            nextResultSet(statement)

            var dbResult = statement.getResultSet()

            while (dbResult.next()) {
              val bfId = dbResult.getString("BusinessFunctionID")
              val validatorId = dbResult.getString("ValidatorID")

              if (!bfValidatorMap.contains(bfId))
                bfValidatorMap.put(bfId, scala.collection.mutable.ArrayBuffer.empty[BFValidator])

              bfValidatorMap(bfId) += new BFValidator(bfId, validatorId)
            }
          }
        }

        val xyz = bfValidatorMap.mapValues(_.toSeq).toMap

        xyz
    }
  }

  /**
   * Получение коллекции валидаторов для входа в статус
   */
  def getStatusInputValidationRules: Map[String, StatusInputValidatorSeq] = {
    if (!domainCacheService.isEmpty(classOf[StatusInputValidatorSeq])) {
      return domainCacheService.map(classOf[StatusInputValidatorSeq])
    }

    val lock = Hazelcast.getLock("omed.model.StatusInputValidatorSeq")
    lock.lock()
    DBProfiler.profile("cache status input validation Rule",execStatProvider,true){
      try {
        if (domainCacheService.isEmpty(classOf[StatusInputValidatorSeq])) {
          logger.fine("load status input validation rules from db")
          val loaded = loadStatusInputValidationRules()
          val list = if (loaded.isEmpty) Map("" -> List()) else loaded
          list.foreach(c => domainCacheService.put(classOf[StatusInputValidatorSeq], c._1, StatusInputValidatorSeq(c._2)))
        } else {
          logger.finer("get status input validation rules from cache")
        }

        domainCacheService.map(classOf[StatusInputValidatorSeq])
      } finally {
        lock.unlock()
      }
    }
  }

  def loadStatusInputValidationRules(): Map[String, Seq[StatusInputValidator]] = {
    connectionProvider.withConnection {
      connection =>

        val statement = dataOperation {
          DB.prepareStatement(connection, "[_Meta].[GetStatusValidationRules]",
            contextProvider.getContext.sessionId, List())
        }

        val resultAvailable = dataOperation {
          DBProfiler.profile("[_Meta].[GetStatusValidationRules]",execStatProvider) { statement.execute() }
        }

        val statusValidatorMap = new scala.collection.mutable.HashMap[String, scala.collection.mutable.ArrayBuffer[StatusInputValidator]]

        if (resultAvailable) {
          dataOperation {

            nextResultSet(statement)

            var dbResult = statement.getResultSet()

            while (dbResult.next()) {
              val statusId = dbResult.getString("StatusID")
              val validatorId = dbResult.getString("ValidatorID")

              if (!statusValidatorMap.contains(statusId))
                statusValidatorMap.put(statusId, scala.collection.mutable.ArrayBuffer.empty[StatusInputValidator])

              statusValidatorMap(statusId) += new StatusInputValidator(statusId, validatorId)
            }
          }
        }

        val xyz = statusValidatorMap.mapValues(_.toSeq).toMap

        xyz
    }
  }

  /**
   * Получение коллекции валидаторов для перехода
   */
  def getTransitionValidationRules: Map[String, TransitionValidatorSeq] = {
    if (!domainCacheService.isEmpty(classOf[TransitionValidatorSeq])) {
      return domainCacheService.map(classOf[TransitionValidatorSeq])
    }

    val lock = Hazelcast.getLock("omed.model.TransitionValidatorSeq")
    lock.lock()
    DBProfiler.profile("cache transition validation rule",execStatProvider,true){
      try {
        if (domainCacheService.isEmpty(classOf[TransitionValidatorSeq])) {
          logger.fine("load transition validation rules from db")
          val loaded = loadTransitionValidatorRules()
          val list = if (loaded.isEmpty) Map("" -> List()) else loaded
          list.foreach(c => domainCacheService.put(classOf[TransitionValidatorSeq], c._1, TransitionValidatorSeq(c._2)))
        } else {
          logger.finer("get transition validation rules from cache")
        }

        domainCacheService.map(classOf[TransitionValidatorSeq])
      } finally {
        lock.unlock()
      }
    }
  }

  def loadTransitionValidatorRules(): Map[String, Seq[TransitionValidator]] = {
    connectionProvider.withConnection {
      connection =>

        val statement = dataOperation {
          DB.prepareStatement(connection, "[_Meta].[GetStatusValidationRules]",
            contextProvider.getContext.sessionId, List())
        }

        val resultAvailable = dataOperation {
          DBProfiler.profile("[_Meta].[GetStatusValidationRules]",execStatProvider) { statement.execute() }
        }

        val transitionValidatorMap = new scala.collection.mutable.HashMap[String, scala.collection.mutable.ArrayBuffer[TransitionValidator]]

        if (resultAvailable) {
          dataOperation {
            nextResultSet(statement)
            nextResultSet(statement)

            var dbResult = statement.getResultSet()

            while (dbResult.next()) {
              val transitionId = dbResult.getString("TransitionID")
              val validatorId = dbResult.getString("ValidatorID")

              if (!transitionValidatorMap.contains(transitionId))
                transitionValidatorMap.put(transitionId, scala.collection.mutable.ArrayBuffer.empty[TransitionValidator])

              transitionValidatorMap(transitionId) += new TransitionValidator(transitionId, validatorId)
            }
          }
        }

        val xyz = transitionValidatorMap.mapValues(_.toSeq).toMap

        xyz
    }
  }
  def getModuleInDomain(domain:Int):Seq[String]={
      if(commonCacheService.isEmpty(classOf[ModuleInDomainSeq])){
        val modulsInDomain =  metaQueryProvider.loadModuleInDomainFromDb
        modulsInDomain.groupBy(f =>f.domain).foreach(f => commonCacheService.put(classOf[ModuleInDomainSeq],f._1.toString,ModuleInDomainSeq(f._2)))
      }
    Option(commonCacheService.get(classOf[ModuleInDomainSeq],domain.toString)).map(f => f.data.map( p=> p.moduleId)).getOrElse(Seq())
  }
  def getFilterModuleInDomain(domain :Int,alias:String =null):String={
    val moduls = getModuleInDomain(domain)
    val withAlias = if (alias!= null) alias + ".ModuleID" else "ModuleID"
    val str = if( moduls.length>0) " or "+ withAlias + " in (" + moduls.map(f => "'" +f +"'").mkString(",") + ")"  else ""
    " and ( "+withAlias+" is null "+ str + ")"
  }
  def dropCache(domains:Seq[Int]){
    // для всех доменов вызываем сброс кэша, зависимого от домена
    for (domain <- domains) {
      val fakeContextProvider = new ContextProvider {
        def getContext = new OmedContext(
          sessionId = null,
          domainId = domain,
          hcuId = null,
          userId = null,
          authorId = null,
          isSuperUser = true,
          request = null,
          timeZone = null,
          roleId = null)
      }

      metaObjectCacheManager.contextProvider = fakeContextProvider
      domainCacheService.contextProvider = fakeContextProvider

      metaObjectCacheManager.drop()

      domainCacheService.drop(classOf[omed.forms.StatusFieldRedefinitionSeq])

      domainCacheService.drop(classOf[omed.forms.MetaFormDescription])

      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.ClassValidationRuleSeq])

      domainCacheService.drop(classOf[omed.forms.ConditionViewFieldSeq])

      domainCacheService.drop(classOf[omed.model.StatusValidatorSeq])
      domainCacheService.drop(classOf[omed.model.StatusInputValidatorSeq])
      domainCacheService.drop(classOf[omed.model.TransitionValidatorSeq])
      domainCacheService.drop(classOf[omed.data.ColorRuleSeq])

      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.FilterNodeSeq])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.MetaGridColumnSeq])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.MetaCardFieldsAndGroups])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.MetaGrid])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.MetaGridSeq])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.AppMenuSeq])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.ContextMenuSeq])

      domainCacheService.drop(classOf[omed.bf.BusinessFunction])

      domainCacheService.drop(classOf[omed.auth.UserRoleSeq])
      domainCacheService.drop(classOf[omed.auth.PermissionMetaSeq])
      domainCacheService.drop(classOf[omed.auth.PermissionDataSeq])

      domainCacheService.drop(classOf[omed.data.SettingsItem])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.MetaObjectStatus])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.MetaObjectStatusSeq])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.ObjectStatusTransitionSeq])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.CardInCardItemSeq])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.CardInCardItem])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.MetaCard])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.StatusWindowGridSeq])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.StatusSectionSeq])
      domainCacheService.drop(classOf[omed.triggers.TriggerSeq])
      domainCacheService.drop(classOf[omed.data.ClientThemeDescription])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.ReportFieldDetailSeq])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.MetaDiagramDetailSeq])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.MetaDiagramRelationSeq])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.MetaDiagramRelation])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.MetaViewDiagram])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.MetaDiagramDetail])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.GridInCardItemSeq])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.SchedulerGroupSeq])
      commonCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.ModuleInDomainSeq])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.TemplateClass])
      domainCacheService.drop(classOf[ru.atmed.omed.beans.model.meta.StatusMenuRedefinitionSeq])

    }

  }
  private def nextResultSet(statement:Statement) {
    DBProfiler.profile("getMoreResults",execStatProvider,true){ if (!statement.getMoreResults())  throw new DataError("Not enough ResultSets") }
  }
}