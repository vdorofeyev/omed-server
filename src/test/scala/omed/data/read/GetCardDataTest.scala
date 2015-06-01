package omed.data.read

import sun.reflect.generics.reflectiveObjects.NotImplementedException
import scala.collection.mutable.HashMap
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import omed.data._
import omed.mocks._
import ru.atmed.omed.beans.model.meta._
import omed.model.{EntityInstance, MetaModel, MetaObject}
import omed.bf.ConfigurationProvider
import omed.lang.eval.Configuration
import omed.forms.StatusFieldRedefinition
import omed.auth.PermissionType

@RunWith(classOf[JUnitRunner])
class GetCardDataTest extends FunSuite {

  test("get card data with overrides") {

    val connectionProvider = new ConnectionProviderMock
    val contextProvider = new ContextProviderMock
    val metaClassProvider = new MetaClassProviderMock
    val metaFormProvider = new MetaFormProviderMock
    val entityFactory = new EntityFactoryImpl

    val configProvider = new ConfigurationProvider {
      def create(): Configuration = Configuration.standard
    }

    class DataWriterServiceMock2 extends DataWriterServiceImpl {
      override def lockObject(objectId: EntityInstance): MetaObject = null
    }

    class DataReaderServiceMock2 extends DataReaderServiceImpl {
      val cardData = new HashMap[String, DataTable]

      override def getCardData(recordId: String,isFull:Boolean): DataTable =
        if (this.cardData.contains(recordId))
          this.cardData(recordId)
        else
          throw new NotImplementedException
      override def getObjectData(classCode: String = null, objectId: String): Map[String, Any] =   {
        val t =getCardData(objectId)
        (t.columns zip t.data(0)).toMap
      }
    }

    val dataReader = new DataReaderServiceMock2
    dataReader.connectionProvider = connectionProvider
    dataReader.contextProvider = contextProvider
    dataReader.metaClassProvider = metaClassProvider
    dataReader.metaFormProvider = metaFormProvider
    dataReader.dataWriterService = new DataWriterServiceMock2
    dataReader.configProvider = configProvider
    dataReader.entityFactory = entityFactory
      // Контекст
    metaFormProvider.metaCards += "recordId-0001" ->
      MetaCard(classId = "classId-0001", viewCardId = "viewCardId-001",
        caption = null, glyph = null,
        width = 10, height = 20,
        fieldsPanelHeight = 30,
        isReadOnly = false,
        contextMenu = Seq(),
        fields = Seq(),
        groups = Seq(),
        refGrids = Seq())
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

    entityFactory.model =  MetaModel(metaClassProvider.metaObjects.values.toSeq)
    // у мета объекта Patient есть привязка к диаграмме статусов и этот статус Новый
    metaClassProvider.objectStatuses += ("classId-0001", "diagrammId-0001") ->
      List(new MetaObjectStatus(id = "diagrammId-0001", name = "New", isNew = false,diagrammId = "111",isEditNotAllowed = false, isDeleteNotAllowed = false),
        new MetaObjectStatus(id = "diagrammId-0002", name = "Other", isNew = false,diagrammId = "111",isEditNotAllowed = false, isDeleteNotAllowed = false))
    // данные
    dataReader.cardData += "recordId-0001" ->
      new DataTable(
        columns = Seq("ID", "Name", "_StatusID","_Domain","_ClassID"),
        binaryItems = Seq.empty[Int],
        data = Seq(Array("recordId-0001", "Пётр", "diagrammId-0001",1,"classId-0001")),
        perm = Map("recordId-0001"->true))
    // соответсвие между ид записью и ид класса
    metaClassProvider.records2classes += "recordId-0001" -> "classId-0001"
    // переопределения
    metaFormProvider.cardFieldProperties += "viewCardId-001" ->
      Seq(
        StatusFieldRedefinition(id = "cfpId-0001", statusId = "diagrammId-0001",
          viewFieldId = "viewFieldId-0001",propertyId = "propertyId-0001",viewId= "viewCardId-001",viewFieldCode = "Name",
          redefinitions = Map ("caption"-> "New Cap",
          "sortOrder" -> "10",
          "isVisible" -> false, "isReadOnly" -> true,
          "isMasked" -> true,
          "isDropDownNotAllowed" -> true,
          "groupId" -> "tabId-001"))
      )

    val (result, grids,sects) = dataReader.getCardDataView("recordId-0001")
    assert(result != null, "result shouldn't be null")
    assert(result.columns.size == 5,
      "incorrect columns count, should be 5 instead " + result.columns.size.toString)
    assert(result.data.size == 1,
      "incorrect rows count, should be 1 instead " + result.data.size.toString)
    assert(result.data(0).fieldOverrides.size == 5,
      "incorrect field ovverides maps, should be 5 instead " + result.data(0).fieldOverrides.size.toString +  result.data(0).fieldOverrides.toString)

    // проверить значение атрибут
    val nameFieldOverrides = result.data(0).fieldOverrides.get("Name")
    assert(nameFieldOverrides != None && nameFieldOverrides.get.size == 7,
      "should be 7 attributes" +  result.data(0).fieldOverrides.get("Name").toString)
//    assert(nameFieldOverrides.get("isVisible") == Option(false) &&
//      nameFieldOverrides.get("isReadOnly") == Option(true) &&
//      nameFieldOverrides.get("caption") == "New Cap" &&
//      nameFieldOverrides.get("sortOrder") == 10 &&
//      nameFieldOverrides.get("groupId") == "tabId-001",
//      "incorrect field overrides")

    // для поля Name задан атрибут isVisible=true, необходимо проверить, что 
    // данные для этого поля затираются
    val nameColumnIndex = result.columns.indexWhere(_ == "Name")
    assert(result.data(0).data(nameColumnIndex) == "","should be empty")
  }
}