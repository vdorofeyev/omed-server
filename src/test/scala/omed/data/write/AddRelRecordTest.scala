package omed.data.write

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{ FunSuite, BeforeAndAfter }

import omed.data._
import omed.mocks._
import omed.triggers.TriggerPeriod
import omed.bf.ExtendedConfiguration
import omed.model.MetaModel

@RunWith(classOf[JUnitRunner])
class AddRelRecordTest extends FunSuite with BeforeAndAfter {

  // подготовить mock объекты
  var dbProviderMock = DalExecutorMock
  var connectionProvider: ConnectionProviderMock = null
  var contextProvider: ContextProviderMock = null
  var metaClassProvider: MetaClassProviderMock = null
  var dataReaderService: DataReaderServiceMock = null
  var triggerExecutor: TriggerExecutorMock = null
  var entityFactory: EntityFactoryImpl = null
  /**
   * Мок объект. Переопределяет настойщий DataWriterServiceImpl
   * и подменяет там только метод возвращающий связывание полей
   */
  class DataWriterMock2 extends DataWriterServiceImpl {

    this.dbProvider = dbProviderMock

    override def getRelationInfo(viewCardId: String, windowGridId: String): (String, String) = {
      ("ID", "PatientID")
    }
  }

  var writerService: DataWriterMock2 = null

  before {
    // подготовить mock объекты

    connectionProvider = new ConnectionProviderMock
    contextProvider = new ContextProviderMock
    metaClassProvider = new MetaClassProviderMock
    dataReaderService = new DataReaderServiceMock
    triggerExecutor = new TriggerExecutorMock

    writerService = new DataWriterMock2
    writerService.connectionProvider = connectionProvider
    writerService.contextProvider = contextProvider
    writerService.metaClassProvider = metaClassProvider
    writerService.triggerExecutor = triggerExecutor
    writerService.dataReaderService = dataReaderService
    writerService.predNotificationProvider = new PredNotifocationProviderMock
    val configProvider = new ExtendedConfiguration
    configProvider.dataConfigProvider = new DataAwareConfiguration
    writerService.configProvider = configProvider

    // Контекст для Пациента
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
    dataReaderService.cardData += "recordId-0001" ->
      new DataTable(
        columns = Seq("ID", "Name", "_StatusID"),
        binaryItems = Seq.empty[Int],
        data = Seq(Array("recordId-0001", "Пётр", "5234")),
        perm = Map())
    metaClassProvider.records2classes += "recordId-0001" -> "classId-0001"
    // Контекст для Эпиздов
    metaClassProvider.metaObjects += "classId-0002" ->
      omed.model.MetaObject(
        id = "classId-0002",
        aliasPattern = null,
        parentId = null,
        code = "Episode",
        fields = List(new omed.model.DataField("ID", omed.model.DataType.String),
          new omed.model.DataField("PatientID", omed.model.DataType.String),
          new omed.model.DataField("_StatusID", omed.model.DataType.String)),
        diagramId = "diagrammId-0001")
    dataReaderService.cardData += "recordId-0002" ->
      new DataTable(
        columns = Seq("ID", "PatientID", "_StatusID"),
        binaryItems = Seq.empty[Int],
        data = Seq(Array("recordId-0002", "recordId-0001", "5234")),
        perm = Map())
    metaClassProvider.records2classes += "recordId-0002" -> "classId-0002"

    entityFactory = new EntityFactoryImpl
    entityFactory.entityDataProvider = new EntityDataProviderMock
    writerService.entityFactory=  entityFactory
    entityFactory.model =  MetaModel(metaClassProvider.metaObjects.values.toSeq)
  }

  after {
    DalExecutorMock.dalFired clear
  }

  /**
   * Добавление связанной записи происходит в два этапа:
   * создаём новую запись, а потом редактируем её, подставляя
   * ссылку в поле-референс на главный объект с которым данная запись связана.
   *
   * В данном примере у Пациента (главный объект) есть список Эпизодов (связанные записи).
   */
  test("Add relation record and fire triggers") {

    val result = writerService.addRelRecord(
      "classId-0002", "viewCardId-12345", "recordId-0001", "windowGridId-1234")

    // Проверка результатов
    assert(result != null,
      "function retrun incorrect result")
    // в контекст не добавлена диаграмма статусов, поэтому два вызова dal методов
    assert(dbProviderMock.dalFired.size == 2, "should be two calls of dalExecutor calls:"+ dbProviderMock.dalFired.size)

    // проверить создание новой записи
    val methodAdd = dbProviderMock.dalFired(0)
    assert(methodAdd.dalMethodName == "dbExecNoResultSet" &&
      methodAdd.methodName == "[_Object].[AddRecord]",
      "at first should be [_Object].[AddRecord] call")
    assert(methodAdd.params == List(("ClassID", "classId-0002"), ("RecordID", result)),
      "add method params are differnt from what we are expected")

    // проверить добавление референса на главный объект (Пациент)
    val methodEdit = dbProviderMock.dalFired(1)
    assert(methodEdit.dalMethodName == "dbExecNoResultSet" &&
      methodEdit.methodName == "[_Object].[EditRecord]",
      "at first should be [_Object].[EditRecord] call")
    assert(methodEdit.params ==
      List(("RecordID", result), ("PropertyCode", "PatientID"), ("Value", "recordId-0001")),
      "edit method params are differnt from what we are expected")

    // проверить вызовы триггеров
    val ft = triggerExecutor.firedTriggers
    assert(ft.size == 2, "must be four fired triggers, but:" + ft.size)
    assert(ft(0).period == TriggerPeriod.Before, "should be before-trigger ")
    assert(ft(1).period == TriggerPeriod.After, "should be after-trigger ")


  }
}