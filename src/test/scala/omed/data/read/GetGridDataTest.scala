package omed.data.read

import sun.reflect.generics.reflectiveObjects.NotImplementedException
import scala.collection.mutable.HashMap
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite}
import omed.data._
import omed.mocks._
import ru.atmed.omed.beans.model.meta._
import omed.bf.ConfigurationProvider
import omed.lang.eval.Configuration
import omed.forms.StatusFieldRedefinition
import omed.auth.{UserRole, PermissionType, PermissionProvider}
import omed.model.{MetaModel, Value}
@RunWith(classOf[JUnitRunner])
class GetGridDataTest extends FunSuite with BeforeAndAfter {

  var connectionProvider = new ConnectionProviderMock
  var contextProvider = new ContextProviderMock
  var metaClassProvider = new MetaClassProviderMock
  var metaFormProvider = new MetaFormProviderMock
  var entityFactory = new EntityFactoryImpl()
  var configProvider = new ConfigurationProvider {
    def create(): Configuration = Configuration.standard
  }
  var permissionProvider = new PermissionProviderMock
  before {
    // контекст мета объектов
    metaClassProvider.metaObjects += "classId-0001" ->
      omed.model.MetaObject(
        id = "classId-0001",
        aliasPattern = null,
        parentId = null,
        code = "Patient",
        fields = List(new omed.model.DataField("ID", omed.model.DataType.String),
          new omed.model.DataField("Name", omed.model.DataType.String),
          new omed.model.DataField("_StatusID", omed.model.DataType.String),
          new omed.model.DataField("_ClassID", omed.model.DataType.String)
        ),
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
          new omed.model.DataField("_StatusID", omed.model.DataType.String),
          new omed.model.DataField("_ClassID", omed.model.DataType.String)
        ),
        diagramId = null)
    metaClassProvider.metaObjects += "classId-0003" ->
      omed.model.MetaObject(
        id = "classId-0003",
        aliasPattern = null,
        parentId = null,
        code = "Patient3",
        fields = List(new omed.model.DataField("ID", omed.model.DataType.String),
          new omed.model.DataField("Name", omed.model.DataType.String),
          new omed.model.DataField("_StatusID", omed.model.DataType.String),
          new omed.model.DataField("_ClassID", omed.model.DataType.String)
        ),
        diagramId = null)
    // у мета объекта Patient есть привязка к диаграмме статусов и этот статус Новый
    metaClassProvider.objectStatuses += ("classId-0001", "diagrammId-0001") ->
      List(new MetaObjectStatus(id = "diagrammId-0001", name = "New", isNew = true,diagrammId = "111",isEditNotAllowed = false, isDeleteNotAllowed = false),
        new MetaObjectStatus(id = "diagrammId-0001", name = "Other", isNew = false,diagrammId = "111",isEditNotAllowed = false, isDeleteNotAllowed = false))
    // диаграмма статусов дгде нет статуса Новая
    metaClassProvider.objectStatuses += ("classId-0003", "diagrammId-0002") ->
      List(new MetaObjectStatus(id = "diagrammId-0002", name = "New", isNew = false,diagrammId = "111",isEditNotAllowed = false, isDeleteNotAllowed = false),
        new MetaObjectStatus(id = "diagrammId-0002", name = "Other", isNew = false,diagrammId = "111",isEditNotAllowed = false, isDeleteNotAllowed = false))
  }

  test("get grid data with overrides") {

    class DataReaderServiceMock2 extends DataReaderServiceImpl {
      val gridData = new HashMap[String, DataTable]

      override def getGridData(gridId: String, nodeId: String, refId: String,
        nodeData: String, recordId: String, viewCardId: String, fieldId: String,
        variablesXml: String, treeVariablesXml: String,filters:Seq[omed.lang.struct.Expression] = Seq(),
        context:Map[String,Value] = Map(),isFull:Boolean = false): DataTable =
        if (this.gridData.contains(gridId))
          this.gridData(gridId)
        else
          throw new NotImplementedException
    }

    val dataReader = new DataReaderServiceMock2
    dataReader.connectionProvider = connectionProvider
    dataReader.contextProvider = contextProvider
    dataReader.metaClassProvider = metaClassProvider
    dataReader.metaFormProvider = metaFormProvider
    dataReader.configProvider = configProvider
     dataReader.permissionProvider = permissionProvider
    dataReader.entityFactory = entityFactory
    entityFactory.model =  MetaModel(metaClassProvider.metaObjects.values.toSeq)
    // Контекст
    // данные
    dataReader.gridData += "windowGridId-0001" ->
      new DataTable(
        columns = Seq("ID", "Name", "_StatusID", "_ClassID","_Domain"),
        binaryItems = Seq.empty[Int],
        data = Seq(
          Array("recordId-0001", "Пётр", "diagrammId-0003", "classId-0001",1),
          Array("recordId-0002", "Иван", "diagrammId-0001", "classId-0002",1),
          Array("recordId-0003", "Сидор", "diagrammId-0002", "classId-0003",1)),
        perm = Map("recordId-0001"->true,"recordId-0002"->true,"recordId-0003"->true))
    // переопределения
    metaFormProvider.gridFieldProperties += "windowGridId-0001" ->
      Seq(
        StatusFieldRedefinition(id = "gfpId-0001", statusId = "diagrammId-0001",
          viewFieldId = "viewFieldId-001",  propertyId = "propertyId-0001", viewFieldCode = "Name", viewId = "windowGridId-0001" ,
        redefinitions = Map("isVisible" ->false,"isReadOnly" ->true,"isDropDownNotAllowed"->true)
         ),
        StatusFieldRedefinition(id = "gfpId-0002", statusId = "diagrammId-0002",
          viewFieldId = "viewFieldId-002",  propertyId = "propertyId-0001", viewFieldCode = "Name", viewId = "windowGridId-0001" ,
          redefinitions = Map("isVisible" ->true,"isReadOnly" ->true,"isMasked"->true, "isDropDownNotAllowed"->true)
        )
      )

    val result = dataReader.getGridDataView(windowGridId = "windowGridId-0001", nodeId = null,
      refId = null, nodeData = null, recordId = null, viewCardId = null,
      fieldId = null, variablesXml = null, treeVariablesXml = null)

    assert(result != null, "result shouldn't be null")
    assert(result.columns.size === 5,"columns")
    assert(result.data.size === 3,"size")

    assert(result.data(0).fieldOverrides == Map("ID" -> Map(), "Name" -> Map(),
      "_StatusID" -> Map(), "_ClassID" -> Map(),"_Domain"->Map()),"first")
//    assert(result.data(1).fieldOverrides == Map("ID" -> Map(),
//      "Name" -> Map("isVisible" -> Some(false), "isReadOnly" -> Some(true)),
//      "_StatusID" -> Map(), "_ClassID" -> Map()))
//    assert(result.data(2).fieldOverrides ==
//      Map("ID" -> Map(), "Name" -> Map("isVisible" -> Some(true), "isReadOnly" -> Some(true)),
//        "_StatusID" -> Map(), "_ClassID" -> Map()))

    // для поля Name во второй строке задан атрибут isVisible=true, необходимо проверить, что 
    // данные для этого поля затираются
    val nameColumnIndex = result.columns.indexWhere(_ == "Name")
    assert(result.data(1).data(nameColumnIndex) == "","null data")
    // а в третьей строке isVisible=false
    assert(result.data(2).data(nameColumnIndex) == "Сидор","test data")
  }

}