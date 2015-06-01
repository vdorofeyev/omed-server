package omed.data.write

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{ FunSuite, BeforeAndAfter }

import omed.mocks._
import omed.data._
import ru.atmed.omed.beans.model.meta._
import omed.triggers.TriggerPeriod
import omed.model.MetaModel

@RunWith(classOf[JUnitRunner])
class AddRecordTest extends FunSuite with BeforeAndAfter {

  val dbProviderMock = DalExecutorMock
  var connectionProvider: ConnectionProviderMock = null
  var contextProvider: ContextProviderMock = null
  var dataReaderService: DataReaderServiceMock = null
  var metaClassProvider: MetaClassProviderMock = null
  var triggerExecutor: TriggerExecutorMock = null
  var entityFactory: EntityFactoryImpl = null
  var writerService: DataWriterServiceImpl = null

  before {
    connectionProvider = new ConnectionProviderMock
    contextProvider = new ContextProviderMock
    dataReaderService = new DataReaderServiceMock
    metaClassProvider = new MetaClassProviderMock
    triggerExecutor = new TriggerExecutorMock

    writerService = new DataWriterServiceImpl

    writerService.dbProvider = dbProviderMock
    writerService.connectionProvider = connectionProvider
    writerService.contextProvider = contextProvider
    writerService.metaClassProvider = metaClassProvider
    writerService.triggerExecutor = triggerExecutor
    writerService.dataReaderService = dataReaderService
    writerService.predNotificationProvider = new PredNotifocationProviderMock
    // контекст мета объектов
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
    // Нет привязки к диаграмме статусов
    metaClassProvider.metaObjects += "classId-0002" ->
      omed.model.MetaObject(
        id = "classId-0002",
        aliasPattern = null,
        parentId = null,
        code = "Patient2",
        fields = List(new omed.model.DataField("ID", omed.model.DataType.String),
          new omed.model.DataField("Name", omed.model.DataType.String),
          new omed.model.DataField("_StatusID", omed.model.DataType.String)),
        diagramId = null)
    metaClassProvider.metaObjects += "classId-0003" ->
      omed.model.MetaObject(
        id = "classId-0003",
        aliasPattern = null,
        parentId = null,
        code = "Patient3",
        fields = List(new omed.model.DataField("ID", omed.model.DataType.String),
          new omed.model.DataField("Name", omed.model.DataType.String),
          new omed.model.DataField("_StatusID", omed.model.DataType.String)),
        diagramId = null)
    // у мета объекта Patient есть привязка к диаграмме статусов и этот статус Новый
    metaClassProvider.objectStatuses += ("classId-0001", "diagrammId-0001") ->
      List(new MetaObjectStatus(id = "diagrammId-0001", name = "New", isNew = true,diagrammId = "111",isEditNotAllowed = false, isDeleteNotAllowed = false),
        new MetaObjectStatus(id = "diagrammId-0001", name = "Other", isNew = false,diagrammId = "111",isEditNotAllowed = false, isDeleteNotAllowed = false))
    // диаграмма статусов дгде нет статуса Новая
    metaClassProvider.objectStatuses += ("classId-0003", "diagrammId-0002") ->
      List(new MetaObjectStatus(id = "diagrammId-0002", name = "New", isNew = false ,diagrammId = "111",isEditNotAllowed = false, isDeleteNotAllowed = false),
        new MetaObjectStatus(id = "diagrammId-0002", name = "Other", isNew = false,diagrammId = "111",isEditNotAllowed = false, isDeleteNotAllowed = false))

    entityFactory = new EntityFactoryImpl
    entityFactory.entityDataProvider = new EntityDataProviderMock
    writerService.entityFactory=  entityFactory
    entityFactory.model =  MetaModel(metaClassProvider.metaObjects.values.toSeq)
  }

  after {
    DalExecutorMock.dalFired clear
  }

  /**
   * При создании новой записи, если в метаописании класса
   * есть привязка к диаграмме статусов, а там [в диаграмме статусов] есть
   * статус с признаком "Новая", то после создания записи присваиваем автоматически
   * статус "Новая".
   * При добавлении статуса вызываются тригеры
   */
  test("Add record 1") {

    val result = writerService.addRecord("classId-0001")

    assert(result != null, "function retrun incorrect result")
    assert(dbProviderMock.dalFired.size == 2, "should be two calls of dalExecutor")

    // проверить вызов метода создания записи
    val methodAdd = dbProviderMock.dalFired(0)
    assert(methodAdd.dalMethodName == "dbExecNoResultSet" &&
      methodAdd.methodName == "[_Object].[AddRecord]",
      "at first should be [_Object].[AddRecord] call")
    assert(methodAdd.params == List(("ClassID", "classId-0001"), ("RecordID", result)),
      "add method params are differnt from what we are expected")

    // проверить вызов метода редактирования записи 
    val methodEdit = dbProviderMock.dalFired(1)
    assert(methodEdit.dalMethodName == "dbExecNoResultSet" &&
      methodEdit.methodName == "[_Object].[EditRecord]",
      "at first should be [_Object].[EditRecord] call")
    assert(methodEdit.params ==
      List(("RecordID", result), ("PropertyCode", "_StatusID"), ("Value", "diagrammId-0001")),
      "edit method params are differnt from what we are expected")

    // проверить, что правильно передаётся статус
    val statusId = methodEdit.params.find(_._1 == "Value")
    assert(statusId != None && statusId.get._2 == "diagrammId-0001", "incorrect StatusID")

    // проверяем вызовы тригеров
    val ft = triggerExecutor.firedTriggers
    assert(ft.size == 2, "must be two fired triggers")
    assert(ft(0).period == TriggerPeriod.Before, "should be before-trigger ")
    assert(ft(1).period == TriggerPeriod.After, "should be before-trigger ")
  }

  /**
   * При создании новой записи, в мета описании класса нет привязки
   * к диаграмме статусов или при наличии привязки [к диаграмме статусов]
   * нет статуса с флагом "Новая и соответсвенно после создания записи она
   *  не редактируется автоматически
   */
  test("Add record 2") {

    val result = writerService.addRecord("classId-0002")

    assert(result != null, "function retrun incorrect result")
    assert(dbProviderMock.dalFired.size == 1, "should be one call of dalExecutor")

    val methodAdd = dbProviderMock.dalFired(0)
    assert(methodAdd.dalMethodName == "dbExecNoResultSet" &&
      methodAdd.methodName == "[_Object].[AddRecord]",
      "at first should be [_Object].[AddRecord] call")
    assert(methodAdd.params == List(("ClassID", "classId-0002"), ("RecordID", result)),
      "add method params are differnt from what we are expected")

    // проверяем вызовы тригеров
    val ft = triggerExecutor.firedTriggers
    assert(ft.size == 2, "must be two fired triggers")
    assert(ft(0).period == TriggerPeriod.Before, "should be before-trigger ")
    assert(ft(1).period == TriggerPeriod.After, "should be before-trigger ")
  }

  /**
   * Есть привязка к диаграмме статусов, но в ней нет статуса Новая
   * поэтому запись добавляется без установки статуса новая.
   */
  test("Add record 3") {
    val result = writerService.addRecord("classId-0003")

    assert(result != null, "function retrun incorrect result")
    assert(dbProviderMock.dalFired.size == 1, "should be one call of dalExecutor")

    val methodAdd = dbProviderMock.dalFired(0)
    assert(methodAdd.dalMethodName == "dbExecNoResultSet" &&
      methodAdd.methodName == "[_Object].[AddRecord]",
      "at first should be [_Object].[AddRecord] call")
    assert(methodAdd.params == List(("ClassID", "classId-0003"), ("RecordID", result)),
      "add method params are differnt from what we are expected")

    // проверяем вызовы тригеров
    val ft = triggerExecutor.firedTriggers
    assert(ft.size == 2, "must be two fired triggers")
    assert(ft(0).period == TriggerPeriod.Before, "should be before-trigger ")
    assert(ft(1).period == TriggerPeriod.After, "should be before-trigger ")
  }
}