package omed.bf

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{ FunSuite, BeforeAndAfter }

import omed.bf._
import omed.mocks._
import omed.mocks.bf._
import omed.system.ContextProvider
import omed.db.ConnectionProvider
import tasks.{GetClientValue, SetValue}
import omed.model.{LazyMetaModel, MetaModel, Value, SimpleValue}

@RunWith(classOf[JUnitRunner])
class BfEngineTest extends FunSuite with BeforeAndAfter {

//  var functionInfoProvider: FunctionInfoProvider = null
//  var processStateProvider: ProcessStateProvider = null
//  var contextProvider: ContextProvider = null
//  var connectionProvider: ConnectionProvider = null
//  var serverStepExecutor: ServerStepExecutor = null
//  var clientResultParser: ClientResultParser = null

  var engine: BusinessFunctionExecutorImpl = null

  before {
    val functionInfoProvider = new FunctionInfoProviderMock
    val processStateProvider = new ProcessStateProviderImpl
    val contextProvider = new ContextProviderMock
    val connectionProvider = new ConnectionProviderMock
    val serverStepExecutor = new ServerStepExecutorMock
    val clientResultParser = new ClientResultParserMock
    val businessFunctionLogger = new BusinessFunctionLoggerMock
    engine = new BusinessFunctionExecutorImpl

    engine.functionInfoProvider = functionInfoProvider
    engine.processStateProvider = processStateProvider
    engine.contextProvider = contextProvider
    engine.connectionProvider = connectionProvider
    engine.serverStepExecutor = serverStepExecutor
    engine.clientResultParser = clientResultParser
    engine.businessFunctionLogger = businessFunctionLogger
    engine.businessFunctionThreadPool = new BusinessFunctionThreadPool
    engine.expressionEvaluator = new omed.model.services.ExpressionEvaluator () {
        override def convertGuidsToObject(  variables: Map[String, Value])={
        variables
      }

    }
    // Контекст
    // бизнес фукнции
    functionInfoProvider.functions += "functionId-001" ->
      new BusinessFunction(
        id = "functionId-001",
        name = "Test function",
        steps = Seq(
          new FunctionStep(
            id = "stepId-001",
            stepType = "_Meta_BFSSetValue",
            name = "clientStep. Give me A number",
            description =
              <_Meta_BFSSetValue
                ID="stepId-001"
                BusinessFunctionID="functionId-001"
                Name="clientStep. Give me A number"
                StepNumber="1"
                Destination="@A"
                SourceExp="#CURRENT(A)"/>),
          new FunctionStep(
            id = "stepId-002",
            stepType = "_Meta_BFSSetValue",
            name = "clientStep. Give me B number",
            description =
              <_Meta_BFSSetValue
                ID="stepId-002"
                BusinessFunctionID="functionId-001"
                Name="clientStep. Give me B number"
                StepNumber="2"
                Destination="@B"
                SourceExp="#CURRENT(B)"/>),
          new FunctionStep(
            id = "stepId-003",
            stepType = "_Meta_BFSSetValue",
            name = "serverStep. Calc A concat B",
            description =
              <_Meta_BFSSetValue
                ID="stepId-003"
                BusinessFunctionID="functionId-001"
                Name="serverStep. Calc A concat B"
                StepNumber="3"
                Destination="@C"
                SourceExp="@A + @B"/>),
          new FunctionStep(
            id = "_Meta_BFSSetValue",
            stepType = "_Meta_BFSSetValue",
            name = "",
            description = <_Meta_BFSSetValue
              ID="stepId-004"
              BusinessFunctionID="functionId-001"
              Name="clientStep. Explore result"
              StepNumber="4"
              Destination="@D"
              SourceExp="#CURRENT(D)"/>)))
//    // обработчики серверного шага
//    serverStepExecutor.asInstanceOf[ServerStepExecutorMock].canHandleList +=
//      "serverStep. Calc A + B" ->
//      (procInfo => {
//
//        val tryA = procInfo.context.get("A")
//        val tryB = procInfo.context.get("B")
//
//        val A = tryA.get.toString.toInt
//        val B = tryB.get.toString.toInt
//
//        val sumAplusB = Map("A+B" -> (A + B).asInstanceOf[Object])
//        val newContext = procInfo.context ++ sumAplusB
//        val newProcInfo = procInfo.copy(context = newContext, message = null)
//
//        newProcInfo
//      })
  }

  /**
   * Распарсить xml от клиента в map
   */
  class ClientResultParserMock extends ClientResultParser {
    override def parse(task: ProcessTask, clientMessage: String) =
      new ClientStep().parseResults(clientMessage)
  }

  /**
   * Преобразовать клиентский шаг в ответ и передать
   * в движок
   */
  def getClientAnswer(clientTask: ProcessTask) = {
    val pattern = """#CURRENT\((.+)\)"""r
    val pattern(data) = clientTask.asInstanceOf[GetClientValue].expression
    val dest = clientTask.asInstanceOf[GetClientValue].destination.replaceFirst("\\@", "")
    String.format("<data><%1$s>%2$s</%1$s></data>", dest, data)
  }

  /**
   * Выполняем бизнес фукцию, которая требует от клиента два числа A=5 и B=7,
   * выполняет серверный шаг сложения A+B, а после печатает на клиенте результат
   */
  test("Business function engine test") {

    val processId = engine.initFunctionInstance("functionId-001", Map())
    assert(processId != null, "business process not initialized")

    // step 1
    val (task1,result1) = engine.getNextClientStep(processId)
    assert(task1.isDefined, "process task should be defined")
    assert(task1.get.isInstanceOf[GetClientValue], "getting client value expexted")
    engine.setClientResult(processId, getClientAnswer(task1.get.asInstanceOf[GetClientValue]))
    assert(engine.getContext(processId)("A").asInstanceOf[SimpleValue].data == "A",
      "incorrect context after client step 1")

    // step 2
    val (task2,result2) = engine.getNextClientStep(processId)
    assert(task2.isDefined, "process task should be defined")
    assert(task2.get.isInstanceOf[GetClientValue], "getting client value expexted")
    engine.setClientResult(processId, getClientAnswer(task2.get.asInstanceOf[GetClientValue]))
    assert(engine.getContext(processId)("B").asInstanceOf[SimpleValue].data == "B",
      "incorrect context after client step 2")

    // step 4
    val (task4,result4)  = engine.getNextClientStep(processId)
    assert(task4.isDefined, "process should be defined")
    assert(task4.get.isInstanceOf[GetClientValue], "in correct client step")
    engine.setClientResult(processId, getClientAnswer(task4.get.asInstanceOf[GetClientValue]))
    assert(engine.getContext(processId)("C").asInstanceOf[SimpleValue].data == "AB",
      "incorrect context after steps 3 and 4")

    // process is finished
    val (taskN,resultN)  = engine.getNextClientStep(processId)
    assert(!taskN.isDefined, "process should be already finished")
  }

  test("Test system variables") {
    val processId = engine.initFunctionInstance("functionId-001", Map())
    assert(engine.getContext(processId)("UserID").asInstanceOf[SimpleValue].data == "testAgent")
    assert(engine.getContext(processId)("AuthorID").asInstanceOf[SimpleValue].data == "Test Agent")
    assert(engine.getContext(processId)("DomainID").asInstanceOf[SimpleValue].data == 1)
  }

  test("Test system variables in steps") {
    engine.functionInfoProvider.asInstanceOf[FunctionInfoProviderMock].functions += "functionId-getSysVars" ->
      new BusinessFunction(
        id = "unctionId-getSysVars",
        name = "Test function",
        steps = Seq(
          new FunctionStep(
            id = "stepId-001",
            stepType = "_Meta_BFSSetValue",
            name = "",
            description =
                <_Meta_BFSSetValue
              ID="stepId-001"
              BusinessFunctionID="unctionId-getSysVars"
              Name=""
              StepNumber="1"
              Destination="@A"
              SourceExp="@UserID"/>)))

    val processId = engine.initFunctionInstance("functionId-getSysVars", Map())
    assert(engine.getContext(processId)("A").asInstanceOf[SimpleValue].data == "testAgent")
  }
}