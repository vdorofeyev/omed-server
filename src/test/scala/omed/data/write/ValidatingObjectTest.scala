package omed.data.write

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{ FunSuite, BeforeAndAfter }

import java.io.StringWriter

import omed.mocks._
import omed.data._
import omed.model.{EntityInstance, MetaModel}
import omed.lang.xml.ValidatorExpressionXmlWriter
import ru.atmed.omed.beans.model.meta._
import omed.lang.eval.Configuration
import omed.bf.{ValidationWarningPool, ExtendedConfiguration}
import omed.validation.{ValidationProviderImpl, ValidationProvider}
import omed.model.services.ExpressionEvaluator

@RunWith(classOf[JUnitRunner])
class ValidatingObjectTest extends FunSuite with BeforeAndAfter {

  var dbProviderProvider = DalExecutorMock
  var connectionProvider: ConnectionProviderMock = null
  var contextProvider: ContextProviderMock = null
  var metaClassProvider: MetaClassProviderMock = null
  var dataReaderService: DataReaderServiceMock = null
  var triggerExecutor: TriggerExecutorMock = null
  var entityFactory: EntityFactoryImpl = null
  var validationWarningPool:ValidationWarningPool = null
  var writerService: DataWriterServiceImpl = null
  var validationProvider: ValidationProviderImpl = null
  var expressionEvaluator: ExpressionEvaluator = null
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
    validationProvider.validationWarningPool = validationWarningPool
    expressionEvaluator.contextProvider = contextProvider
    validationProvider.expressionEvaluator=expressionEvaluator

    writerService = new DataWriterServiceImpl
    writerService.dbProvider = dbProviderProvider
    writerService.connectionProvider = connectionProvider
    writerService.contextProvider = contextProvider
    writerService.metaClassProvider = metaClassProvider
    writerService.triggerExecutor = triggerExecutor
    writerService.dataReaderService = dataReaderService
    val configProvider = new ExtendedConfiguration
    configProvider.dataConfigProvider = new DataAwareConfiguration
    writerService.configProvider = configProvider
    writerService.validationProvider = validationProvider
    writerService.validationWarningPool = validationWarningPool
    // контекст мета объектов
    metaClassProvider.metaObjects += "classId-0001" ->
      omed.model.MetaObject(
        id = "classId-0001",
        aliasPattern = null,
        parentId = null,
        code = "Patient",
        fields = List(new omed.model.DataField("ID", omed.model.DataType.String),
          new omed.model.DataField("Name", omed.model.DataType.String),
          new omed.model.DataField("Snils", omed.model.DataType.String),
          new omed.model.DataField("INN", omed.model.DataType.String),
          new omed.model.DataField("_StatusID", omed.model.DataType.String)),
        diagramId = "diagrammId-0001")
    // правила валидации для класса
    metaClassProvider.classValidtionRules += "classId-0001" ->
          Seq(
          ClassValidationRule(
            id = "val-0010",
            classId = "classId-0001",
            name = "Name check",
            condition ="@this.Name != \"Вася\"",
            falseMessage = "Error! Name should'n be Вася",
            isEnabledInDomain = true,
            isUsedInStatus = false,
            validationResultType = ValidationResultType.Error),
          ClassValidationRule(
            id = "val-0011",
            classId = "classId-0001",
            name = "Name check 2. for other domain",
            condition = "@this.Name != \"Вася\"",
            falseMessage = "Error! Name should'n be Вася",
            isEnabledInDomain = false,
            isUsedInStatus = false,
            validationResultType = ValidationResultType.Error),
          ClassValidationRule(
            id = "val-0012",
            classId = "classId-0001",
            name = "Id check",
            condition = "@this.ID != \"recordId-001\"",
            falseMessage = "Warn! Id possibly should'n be recordId-001",
            isEnabledInDomain = true,
            isUsedInStatus = false,
            validationResultType = ValidationResultType.Warning)
      )
    metaClassProvider.fieldValidationRules =    Map()

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

  /**
   * Измененный объект проходит валидацию
   */
  test("Validating object after edit test. Validation is ok") {

    // валидное изменение объекта

    val result = writerService.validate(  new EntityInstance(null,metaClassProvider.getClassMetadata("classId-0001"),"recordId-002",null,Map(
      "ID" -> "recordId-002",
    "_ClassID"->"classId-0001",
      "Name" -> "Пётр",
      "Snils" -> "112-233-445 95",
      "INN" -> "500100732259",
      "_StatusID" -> "5234")),
      fields = Map("Name" -> "Пётр", "Snils" -> "112-233-445 95", "INN" -> "500100732259"))

    assert(result != null, "incorrect result")
    val (isValid, errors) = result
    assert(isValid, "should be valid")
    assert(errors == Nil, "should be no errors")
  }

  /**
   * Измененный объект не проходит валидацию
   */
  test("Validating object after edit test. Validation failed") {
    // срабатывают правила валидации

    val (isValid2, errors) = writerService.validate( new EntityInstance(null,metaClassProvider.getClassMetadata("classId-0001"),"recordId-002",null,Map(
      "ID" -> "recordId-001",
      "_ClassID"->"classId-0001",
      "Name" -> "Вася",
      "Snils" -> "112-233-445 95",
      "_StatusID" -> "5234")),
      fields = Map("ID" -> "recordId-001", "Name" -> "Вася"))

  //  assert(result != null, "incorrect result")
  //  val (isValid2, errors) = result
    assert(!isValid2, "should'n be valid "+errors.length.toString)
    // проверить валидаторы-ошибки на класс  
 //   assert(errors.size == 3, "should be one class error, one class warn and one field error")

    assert(errors.exists(_.validationRule.name == "Name check"), "class error name is wrong")
    // проверить валидаторы-предупреждения на класс
    assert(errors.exists(_.validationRule.name == "Id check"), "class warn name is wrong")
  }
}