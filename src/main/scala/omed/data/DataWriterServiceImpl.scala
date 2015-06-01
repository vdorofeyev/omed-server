package omed.data

import java.util.{Calendar, Date, UUID}
import com.google.inject.Inject
import omed.model._
import omed.triggers._
import omed.lang.eval._
import omed.lang.xml.ValidatorExpressionXmlReader
import omed.system.ContextProvider
import omed.db._
import omed.model.MetaClassProvider
import ru.atmed.omed.beans.model.meta._
import omed.errors._
import omed.bf.{ValidationWarningPool, ConfigurationProvider}
import omed.validation.ValidationProvider
import omed.cache.ExecStatProvider
import omed.model.services.ExpressionEvaluator
import omed.lang.eval.ValidatorContext
import omed.model.DataField
import omed.predNotification.PredNotificationProvider
import omed.forms.MetaFormProvider
import omed.auth.{PermissionType, PermissionProvider}
import java.util.logging.Logger

class DataWriterServiceImpl extends DataWriterService
  with DataAccessSupport {
  val logger = Logger.getLogger(this.getClass.getName())
  @Inject
  var connectionProvider: ConnectionProvider = null
  @Inject
  var contextProvider: ContextProvider = null
  @Inject
  var metaClassProvider: MetaClassProvider = null
  @Inject
  var metaFormProvider: MetaFormProvider = null
  @Inject
  var triggerExecutor: TriggerExecutor = null
  @Inject
  var dataReaderService: DataReaderService = null
  @Inject
  var configProvider: ConfigurationProvider = null
  @Inject
  var dbProvider: DBProvider = null
  @Inject
  var validationProvider:ValidationProvider = null
  @Inject
  var execStatPrivider:ExecStatProvider = null
  @Inject
  var predNotificationProvider:PredNotificationProvider = null
  @Inject
  var entityFactory:EntityFactory = null
  @Inject
  var permissionProvider: PermissionProvider = null
  @Inject
  var execStatProvider:ExecStatProvider = null
  @Inject
  var validationWarningPool:ValidationWarningPool = null
  /**
   * @return newGuid
   */
  def addRecord(classId: String,relationField:Map[String,String]=Map()): EntityInstance = {
    // сгенерировать новый идентификатор
    val newGuid = UUID.randomUUID().toString.toUpperCase

    connectionProvider.inTransaction {
      connection =>
        // получить метаданные класса
        val metaClass = metaClassProvider.getClassMetadata(classId)

        // добавить статус "Новый"
        val newState = if (metaClass.diagramId != null) {
          // получить список статусов
          val statusList = metaClassProvider.getClassStatusDiagramm(
            classId, metaClass.diagramId)

          statusList.find(el => el.isNew).orNull
        } else null

        val fieldValues =
          (metaClass.fields.map(_.code -> null.asInstanceOf[Any]) ++
          Seq("ID" -> newGuid.asInstanceOf[Any],"_StatusID"->newState,"_ClassID"->classId) ).toMap  ++relationField


        dataOperation {
          dbProvider.dbExecNoResultSet(connection, "[_Object].[AddRecord]",
            contextProvider.getContext.sessionId,
            List(("ClassID", classId), ("RecordID", newGuid)),execStatPrivider)
        }
        val initObject = entityFactory.createEntityWithDataAndObject(metaClass,fieldValues)

        //  Триггеры на добавление и изменние начальных значений срабатывают одновременно и один раз
        trigged(initObject,relationField.keys.toSeq ++ Seq("ID"), TriggerEvent.OnInsert,false)
        {
          if (newState != null) directSaveField(classId, newGuid, "_StatusID", newState.id)
          relationField.foreach(f => directSaveField(classId, newGuid, f._1, f._2) )
        }
        // создание предуведомлений
        if(newState!=null) predNotificationProvider.createPredNotificationsForObject(initObject.drop)
        initObject.drop

    }
  }

  /**
   * @return  (isValid, newGuid, classErrors, classWarnings, fieldErrors)
   */
  def addRelRecord(classId: String, viewCardId: String, cardRecordId: String,
    windowGridId: String): EntityInstance = {

    //(Boolean, String, Seq[CompiledValidationRule]) = {

    /* try {*/
    connectionProvider.inTransaction {
      connection =>

      // получить параметры, через которые происходит связывание
        val (cardPropertyCode, gridPropertyCode) =
          this.getRelationInfo(viewCardId, windowGridId)

        // получить значение для поля cardPropertyCode
        val cardData = dataReaderService.getCardData(cardRecordId)

        val metaClass = metaClassProvider.getClassMetadata(classId)

        val cardPropertyData: Object =
          if (cardData.data.length == 1) {
            val row = cardData.data(0).asInstanceOf[Array[Object]]
            val i = cardData.columns.indexWhere(key => key == cardPropertyCode)
            row(i)
          } else throw new NotFoundError("Данные для данной формы карточки не найдены.")

        // создать новую запись
        val newGuid = this.addRecord(classId,Map(gridPropertyCode->cardRecordId))
        newGuid
    }

  }

  def addRelation(relationId:String,fromObjectId:String):String ={
    val metaRelation = metaFormProvider.getMetaRelation(relationId)
    if (metaRelation==null) throw new MetaModelError("Не найден relation type:" + relationId)
    if(metaRelation.viewDiagramDetailId!=null){
       val metaDetail = metaFormProvider.getMetaDiagramDetail(metaRelation.viewDiagramDetailId)
       val recId = addRecord(metaDetail.detailClassId)
       editRecord(recId,Map(metaRelation.startPropertyCode->fromObjectId))
       recId.getId
    }
    else{
      fromObjectId
    }
  }
  def deleteRelation(relationId:String,objectId:String){
    val metaRelation = metaFormProvider.getMetaRelation(relationId)
    if(metaRelation.viewDiagramDetailId!=null){
      deleteRecord(objectId)
    }
    else{
      editRecord(objectId,Map(metaRelation.startPropertyCode->null))
    }
  }
  def editRelation(relationId:String,objectId:String,fromObjectId:String,toObjectId:String){
    val metaRelation = metaFormProvider.getMetaRelation(relationId)
    if(metaRelation.viewDiagramDetailId!=null){
      editRecord(objectId,Map(metaRelation.startPropertyCode->fromObjectId,metaRelation.endPropertyCode->toObjectId).filter(f => f._2 !=null))
    }
    else{
      val obj = dataReaderService.getObjectData(null,toObjectId)
      editRecord(fromObjectId,Map(metaRelation.startPropertyCode->obj(metaRelation.endPropertyCode).toString))
    }
  }

  def editPosition(nodeId:String,treeParams:String,objectId:String,windowGridId:String,newValues:Map[String,String])={
    val hash = if(nodeId!= null) nodeId + "__" + treeParams else ""
     val data = dataReaderService.getGridDataView(windowGridId,nodeId,null,null,null,null,null,null,treeParams,true)
    connectionProvider.inTransaction {
      connection =>
       data.data.foreach(f => if (!f.position.saved) {
         val newObj = addRecord("AA1D695A-7C2F-42B1-9919-05BE8E74400C",Map()).getId
         f.position.id = newObj
         directSaveField("AA1D695A-7C2F-42B1-9919-05BE8E74400C",newObj,"X",f.position.x)
         directSaveField("AA1D695A-7C2F-42B1-9919-05BE8E74400C",newObj,"Y",f.position.y)
         directSaveField("AA1D695A-7C2F-42B1-9919-05BE8E74400C",newObj,"Width",f.position.width)
         directSaveField("AA1D695A-7C2F-42B1-9919-05BE8E74400C",newObj,"Height",f.position.height)
         directSaveField("AA1D695A-7C2F-42B1-9919-05BE8E74400C",newObj,"ObjectID",f.position.objectId)
         directSaveField("AA1D695A-7C2F-42B1-9919-05BE8E74400C",newObj,"FilterHash",hash)
       })
      val obj = data.data.find(f => f.position.objectId == objectId).map(_.position.id).orNull
      if(obj ==null) throw new RuntimeException("Редактируемый объект не найден в выборке данных")
      newValues.foreach( f => directSaveField("AA1D695A-7C2F-42B1-9919-05BE8E74400C",obj,f._1,f._2) )
    }
  }
  /**
   * Save field without validation, firing triggers etc.
   * In other words, "clean" save to DB (through [_Object].[EditRecord).
   */
  def directSaveField(classId: String, recordId: String, fieldCode: String, fieldValue: Any) {
    connectionProvider.withConnection {
      connection =>
        // Ищем мета-класс текущего объекта
        val metaObject = metaClassProvider.getClassMetadata(classId)

        // получить метаописание поля
        val fieldMetaData = metaObject(fieldCode)

        val results = {
          // для поля с бинарными данными
          if (fieldMetaData.isInstanceOf[DataField] &&
            fieldMetaData.asInstanceOf[DataField].dataType.toString == "binary"
            && fieldValue != null && fieldValue != "") {
            // раскодировать в бинарный поток
            import net.iharder.Base64
            val data = Base64.decode(fieldValue.toString)
            dataOperation {
              dbProvider.dbExecNoResultSet(connection, "[_Object].[EditRecord]",
                contextProvider.getContext.sessionId,
                List(("RecordID", recordId),
                  ("PropertyCode", fieldMetaData.code),
                  ("ValueBIN", data.asInstanceOf[AnyRef])),execStatPrivider)
            }
          } else // для всех остальных полей
            dataOperation {
              dbProvider.dbExecNoResultSet(connection, "[_Object].[EditRecord]",
                contextProvider.getContext.sessionId,
                List(("RecordID", recordId),
                  ("PropertyCode", fieldMetaData.code),
                  ("Value", fieldValue.asInstanceOf[AnyRef])),execStatPrivider)
            }
          // TODO: update lock time
        }

        // возвращаем результат сохранения
        results
    }
  }

  /**
   * Редактирование объекта
   */
  def editRecord(recordId: String, fields: Map[String, String]):
  (Boolean, Seq[CompiledValidationRule])={
    editRecord(entityFactory.createEntity(recordId),fields)
  }
  def editRecord(
    record: EntityInstance,
    fields: Map[String, String]): (Boolean, Seq[CompiledValidationRule]) = {
    if(! permissionProvider.getDataPermission(record.getId)(PermissionType.Write)) throw new DataAccessError(record.getId)
    try {
      connectionProvider.inTransaction {
        connection =>
          val metaObject =  lockObject(record)
          val parsedFields = fields.map(_ match {
            case (code, value) =>
              val fieldMeta = metaObject(code)
              val dataType = fieldMeta match {
                case df: DataField => df.dataType
                case _ => DataType.String
              }

              if (dataType != DataType.String)
                (code, DataType.parse(value, dataType))
              else
                (code, value)
          })
          // выполнить валидацию
          val (isValid, errors) =
          DBProfiler.profile("validate edit",execStatPrivider,true) {
            validate(entityFactory.createEntityWithDataAndObject(record.obj,record.data++parsedFields), fields)  }
          validationWarningPool.addWarnings(errors)
          if (!isValid)
            throw new ValidationException(isValid,validationWarningPool.getWarnings.toSeq)
          // сохранить поля
          trigged(record, fields.keys.toSeq, TriggerEvent.OnUpdate,false) {
            fields.foreach(f => directSaveField(metaObject.id, record.getId, f._1, f._2))
            record.update(parsedFields)
          }
          (isValid, errors)
      }
    } catch {
      case e: ValidationException => throw e
      case e @ _ => throw e
    }
  }

  /**
   * Удаление объекта
   */
  def deleteRecord(recordId: String) {
    if(! permissionProvider.getDataPermission(recordId)(PermissionType.Write)) throw new DataAccessError(recordId)
    connectionProvider.withConnection {
      connection =>
        val inst = entityFactory.createEntity(recordId)
        trigged(inst, Seq("ID"), TriggerEvent.OnDelete, refresh = false) {
          dataOperation {
            dbProvider.dbExecNoResultSet(connection, "[_Object].[DeleteRecord]",
              contextProvider.getContext.sessionId,
              List(("RecordID", recordId)),execStatPrivider)
          }
        }
    }
  }

  def lockObject(obj: EntityInstance): MetaObject = {
    DBProfiler.profile("lockObject",execStatProvider,true) {
      if(obj==null) return null
      connectionProvider.inTransaction {
        connection =>
          val metaclass = throwIfObjectLocked(obj)
          if (metaclass.lockTimeout.isDefined) {
            directSaveField(metaclass.id, obj.getId, "LockTime",
              new java.text.SimpleDateFormat(DataType.DateTimeFormat).format(new Date()))
            directSaveField(metaclass.id, obj.getId, "LockUserID", contextProvider.getContext.authorId)
          }
          metaclass
      }
    }
  }

  def unlockObject(obj: EntityInstance) {
    connectionProvider.inTransaction {
      connection =>
        val metaclass = throwIfObjectLocked(obj)
        if (metaclass.lockTimeout.isDefined) {
          directSaveField(metaclass.id, obj.getId, "LockTime", null)
          directSaveField(metaclass.id, obj.getId, "LockUserID", null)
        }
    }
  }

  def throwIfObjectLocked(obj: EntityInstance): MetaObject = {
    val metaclass = obj.obj
    if (metaclass.lockTimeout.isDefined) {
      val c = Calendar.getInstance

      if (obj.getLockUserId != null && obj.getLockUserId != contextProvider.getContext.authorId &&
        obj.getLockTime != null) {

        c.setTime(obj.getLockTime)
        c.add(Calendar.SECOND, metaclass.lockTimeout.get)
        if (c.getTime.after(new Date())) {
          val fio =
            try{
              val user = dataReaderService.getObjectData("UserAccount", obj.getLockUserId)
              val employee = dataReaderService.getObjectData("Employee", user.get("EmployeeID").get.asInstanceOf[String])

              if(employee==null)  ""
              else List(
                employee.get("LastName").get,
                employee.get("FirstName").get,
                employee.get("SecondName").orNull).mkString(" ").trim
            }
            catch {
              case _ => ""
            }
          throw new ObjectLockedException(fio)
        }
      }
    }
    metaclass
  }

  /*----------------------------------------------------------------*/

  /**
   * Вызов триггеров
   * @param refresh Refresh entity from DB after @f invocation
   *                to call "after" triggers with new data
   */
  def trigged[RType](inst: EntityInstance, fields: Seq[String],
    tEvent: TriggerEvent.Value, refresh: Boolean = true)(f: => RType): RType = {

    // триггеры до
    triggerExecutor.fireTriggers(inst, fields,
      tEvent, TriggerPeriod.Before)

    val result = f
    val refreshed = if (refresh) {
      inst.drop
    } else inst
    // триггеры после
    triggerExecutor.fireTriggers(refreshed, fields,
      tEvent, TriggerPeriod.After)

    result
  }

  protected def getRelationInfo(viewCardId: String, windowGridId: String): (String, String) = {
    connectionProvider.withConnection {
      connection =>
        val dbResult = dataOperation {
          dbProvider.dbExec(connection, "[_Meta].[GetViewCardGridParameter]",
            contextProvider.getContext.sessionId,
            List(("ViewCardID", viewCardId),
              ("WindowGridID", windowGridId)),execStatPrivider)
        }

        if (!dbResult.next())
          throw new MetaModelError("Ошибка при получении полей связывания.")

        val (cardPropertyCode, gridPropertyCode) = dataOperation {
          (dbResult.getString("CardPropertyCode"),
            dbResult.getString("GridPropertyCode"))
        }

        (cardPropertyCode, gridPropertyCode)
    }
  }

  /**
   * Провалидировать объект
   *
   * @param fields коллекция полей которые изменяем
   * @return (isValid, classErrors, classWarnings, fieldErrors)
   */
  def validate(obj:EntityInstance, fields: Map[String, Any]):
  (Boolean, Seq[CompiledValidationRule]) = {
    val context = {
      Map("this" -> obj) ++ contextProvider.getContext.getSystemVariables
    }
    val (classErrors,classWarnings) =validationProvider.getComlpexEditValidators(obj.getClassId,obj.getStatusId,fields,context)
    /**
     * Признак пройдена валидация или нет
     */
    def validationSucceeded =
      if (classErrors == null || classErrors.length == 0)
        true
      else false

    (validationSucceeded, classErrors ++ classWarnings )
  }
}
