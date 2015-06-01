package omed.data.write

import java.io.StringWriter

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{ FunSuite, BeforeAndAfter }

import omed.mocks._
import omed.data._
import ru.atmed.omed.beans.model.meta._
import omed.triggers.TriggerPeriod
import omed.model.{EntityInstance, MetaObject, MetaModel}
import omed.lang.xml.ValidatorExpressionXmlWriter
import omed.lang.eval.Configuration
import omed.model.services.{ExpressionEvaluator, TriggerServiceImpl}
import omed.bf.{ValidationWarningPool, ExtendedConfiguration}
import omed.validation.ValidationProviderImpl
import omed.errors.ValidationException

@RunWith(classOf[JUnitRunner])
class EditTest extends FunSuite with BeforeAndAfter {

  // подготовить mock объекты
  var dbProviderMock = DalExecutorMock
  var connectionProvider: ConnectionProviderMock = null
  var contextProvider: ContextProviderMock = null
  var metaClassProvider: MetaClassProviderMock = null
  var dataReaderService: DataReaderServiceMock = null
  var triggerExecutor: TriggerExecutorMock = null
  var validationProvider:ValidationProviderImpl = null
  var validationWarningPool:ValidationWarningPool = null
  var writerService: DataWriterServiceImpl = null
  var entityFactory: EntityFactoryImpl = null
  var expressionEvaluator: ExpressionEvaluator = null
  class DataWriterServiceMock2 extends DataWriterServiceImpl {
    override def lockObject(objectId: EntityInstance): MetaObject = {
      objectId.obj
     // metaClassProvider.getClassByRecord(objectId)
    }
  }

  before {
    connectionProvider = new ConnectionProviderMock
    contextProvider = new ContextProviderMock
    metaClassProvider = new MetaClassProviderMock
    dataReaderService = new DataReaderServiceMock
    triggerExecutor = new TriggerExecutorMock
    validationWarningPool = new ValidationWarningPool
    validationProvider = new  ValidationProviderImpl
    expressionEvaluator = new ExpressionEvaluator
    validationProvider.metaClassProvider = metaClassProvider
    val configProvider1 = new ExtendedConfiguration
    configProvider1.dataConfigProvider = new DataAwareConfiguration
    validationProvider.configProvider = configProvider1
    validationProvider.contextprovider = contextProvider
    expressionEvaluator.contextProvider = contextProvider
    validationProvider.validationWarningPool = validationWarningPool
    validationProvider.expressionEvaluator = expressionEvaluator
    writerService = new DataWriterServiceMock2
    writerService.dbProvider = dbProviderMock

    writerService.connectionProvider = connectionProvider
    writerService.contextProvider = contextProvider
    writerService.metaClassProvider = metaClassProvider
    writerService.triggerExecutor = triggerExecutor
    writerService.dataReaderService = dataReaderService

    val configProvider = new ExtendedConfiguration
    configProvider.dataConfigProvider = new DataAwareConfiguration
    writerService.configProvider = configProvider
    writerService.validationProvider = validationProvider
    writerService.predNotificationProvider = new PredNotifocationProviderMock
    writerService.permissionProvider = new PermissionProviderMock
    writerService.validationWarningPool = validationWarningPool
    // контекст мета объектов
    dataReaderService.objectClass += "recordId-9012312312"-> "Patient"
    metaClassProvider.metaObjects += "classId-0001" ->
      omed.model.MetaObject(
        id = "classId-0001",
        aliasPattern = null,
        parentId = null,
        code = "Patient",
        fields = List(new omed.model.DataField("ID", omed.model.DataType.String),
          new omed.model.DataField("Name", omed.model.DataType.String),
          new omed.model.DataField("_StatusID", omed.model.DataType.String)),
        diagramId = "diagrammId-0001")
    // у мета объекта Patient есть привязка к диаграмме статусов и этот статус Новый
    metaClassProvider.objectStatuses += ("classId-0001", "diagrammId-0001") ->
      List(new MetaObjectStatus(id = "diagrammId-0001", name = "New", isNew = true,diagrammId = "111",isEditNotAllowed = false, isDeleteNotAllowed = false),
        new MetaObjectStatus(id = "diagrammId-0002", name = "Other", isNew = false,diagrammId = "111",isEditNotAllowed = false, isDeleteNotAllowed = false))

    // правила валидации для класса
    metaClassProvider.classValidtionRules += "classId-0001" ->
      Seq(
        ClassValidationRule(
          id = "val-0001",
          classId = "classId-0001",
          name = "Name check",
          condition = "@this.Name != \"Вася\"",
          falseMessage = "Error! Name should'n be Вася",
          isEnabledInDomain = true,
          isUsedInStatus = false,
          validationResultType = ValidationResultType.Error),
        ClassValidationRule(
          id = "val-0002",
          classId = "classId-0001",
          name = "Name check 2. for other domain",
          condition =  "@this.Name != \"Вася\"",
          falseMessage = "Error! Name should'n be Вася",
          isEnabledInDomain = false,
          isUsedInStatus = false,
          validationResultType = ValidationResultType.Error),
        ClassValidationRule(
          id = "val-0003",
          classId = "classId-0001",
          name = "Id check",
          condition = "@this.ID != \"recordId-001\"",
          falseMessage = "Warn! Id possibly should'n be recordId-001",
          isEnabledInDomain = true,
          isUsedInStatus = false,
          validationResultType = ValidationResultType.Warning))
    entityFactory = new EntityFactoryImpl
    entityFactory.entityDataProvider = new EntityDataProviderMock
    entityFactory.dataReader = dataReaderService
    writerService.entityFactory=  entityFactory
    entityFactory.model =  MetaModel(metaClassProvider.metaObjects.values.toSeq)
    expressionEvaluator.model= MetaModel(metaClassProvider.metaObjects.values.toSeq)
  }

  after {
    DalExecutorMock.dalFired clear
  }

  test("Edit fields and fire triggers") {
    val record = entityFactory.createEntityWithData(Map("_StatusID"->"diagrammId-0001","ID"->"recordId-9012312312","_ClassID"->"classId-0001","Name" -> "Init"))
    metaClassProvider.records2classes.put("recordId-9012312312", "classId-0001")
    val result = writerService.editRecord(record,
       fields = Map("Name" -> "Пётр Первый"))

    assert(result._1 && result._2 == Nil,
      "function return incorrect result")
    assert(dbProviderMock.dalFired.size == 1, "should be one call of dalExecutor")

    // проверить редактирование
    val methodEdit = dbProviderMock.dalFired(0)
    assert(methodEdit.dalMethodName == "dbExecNoResultSet" &&
      methodEdit.methodName == "[_Object].[EditRecord]",
      "at first should be [_Object].[EditRecord] call")
    assert(methodEdit.params ==
      List(("RecordID", "recordId-9012312312"), ("PropertyCode", "Name"), ("Value", "Пётр Первый")),
      "edit method params are differnt from what we are expected")

    // вызовы триггеров
    val ft = triggerExecutor.firedTriggers
    assert(ft.size == 2, "must be two fired triggers")
    assert(ft(0).period == TriggerPeriod.Before, "should be before-trigger ")
    assert(ft(1).period == TriggerPeriod.After, "should be before-trigger ")

  }

  test("Edit fields with active validation rules and fire triggers") {
    val record = entityFactory.createEntityWithData(Map("_StatusID"->"diagrammId-0001","ID"->"recordId-9012312312","_ClassID"->"classId-0001","Name" -> "Init"))
    metaClassProvider.records2classes.put("recordId-9012312312", "classId-0001")
    var result: (Boolean,Seq[CompiledValidationRule]) = null
    try{
    result = writerService.editRecord(record,
      fields = Map("Name" -> "Вася"))
    }
    catch {
      case e:ValidationException =>   result = (false,e.results)

      }
    var (isValid, classErrors) = result
    assert(!isValid, "shouldn't be valid")
    // проверить валидаторы-ошибки на класс
    assert(classErrors.size == 1, "should be one class error")
    assert(classErrors(0).validationRule.name == "Name check", "class error name is wrong")

    // проверить, что произошёл rollback
    assert(connectionProvider.methodHistory.contains("rollback"), "operation should be rollbacked")

    assert(dbProviderMock.dalFired.size == 0, "should be zero calls of dalExecutor")

    // вызовы тригеров
    val ft = triggerExecutor.firedTriggers
    assert(ft.size == 0, "should be zero fired triggers")
  }

  test("trigger fields") {
    val ts = new TriggerServiceImpl()
    assert(ts.getWatchFields("a") == Set("a"))
    assert(ts.getWatchFields("a, b") == Set("a", "b"))
    assert(ts.getWatchFields("") == Set.empty, ts.getWatchFields("").size)
    assert(ts.getWatchFields(",") == Set.empty, ts.getWatchFields(",").size)
  }
}
