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
class DeleteRecordTest extends FunSuite with BeforeAndAfter {

  var dbProviderMock = DalExecutorMock
  var connectionProvider: ConnectionProviderMock = null
  var contextProvider: ContextProviderMock = null
  var metaClassProvider: MetaClassProviderMock = null
  var dataReaderService: DataReaderServiceMock = null
  var triggerExecutor: TriggerExecutorMock = null

  var writerService: DataWriterServiceImpl = null
  var entityFactory: EntityFactoryImpl = null
  before {
    // подготовить mock объекты

    connectionProvider = new ConnectionProviderMock
    contextProvider = new ContextProviderMock
    metaClassProvider = new MetaClassProviderMock
    dataReaderService = new DataReaderServiceMock
    triggerExecutor = new TriggerExecutorMock

    writerService = new DataWriterServiceImpl
    writerService.dbProvider = dbProviderMock
    writerService.connectionProvider = connectionProvider
    writerService.contextProvider = contextProvider
    writerService.metaClassProvider = metaClassProvider
    writerService.triggerExecutor = triggerExecutor
    writerService.dataReaderService = dataReaderService
    writerService.predNotificationProvider = new PredNotifocationProviderMock
    writerService.permissionProvider = new PermissionProviderMock
    // контекст мета объектов
    dataReaderService.objectClass += "recordId-0001"->"Patient"
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
    metaClassProvider.records2classes += "recordId-0001" -> "classId-0001"
    entityFactory = new EntityFactoryImpl
    entityFactory.dataReader = dataReaderService
    entityFactory.entityDataProvider = new EntityDataProviderMock
    writerService.entityFactory=  entityFactory
    entityFactory.model =  MetaModel(metaClassProvider.metaObjects.values.toSeq)
  }

  after {
    DalExecutorMock.dalFired clear
  }

  test("Delete record and fire triggers") {

    writerService.deleteRecord("recordId-0001")

    assert(dbProviderMock.dalFired.size == 1, "should be one call of dalExecutor")
    // проверить удаление 
    val methodEdit = dbProviderMock.dalFired(0)
    assert(methodEdit.dalMethodName == "dbExecNoResultSet" &&
      methodEdit.methodName == "[_Object].[DeleteRecord]",
      "at first should be [_Object].[DeleteRecord] call")
    assert(methodEdit.params == List(("RecordID", "recordId-0001")),
      "edit method params are differnt from what we are expected")

    // проверить вызовы тригерры
    val ft = triggerExecutor.firedTriggers
    assert(ft.size == 2, "must be two fired triggers")
    assert(ft(0).period == TriggerPeriod.Before, "should be before-trigger ")
    assert(ft(1).period == TriggerPeriod.After, "should be before-trigger ")
  }
}