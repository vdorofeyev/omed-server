package omed.bf

import tasks._
import util.matching.Regex
import util.matching.Regex.Match
import omed.model.Value


object ProcessFactory {

  def createProcess(
    bf: BusinessFunction,
    processId: String, sessionId: String,
    params: Map[String, Value] = Map()): ProcessState = {

    val processTasks = (new StepAnalyser).analyseSteps(bf.steps,params)

    val process = new ProcessState(
      id = processId,
      sessionId = sessionId,
      functionId = bf.id,
      context = params,
      tasks = processTasks,
      state = if (processTasks.isEmpty)
        ProcessStateType.Finished
      else ProcessStateType.Running)

    process
  }

  private class StepAnalyser {
    val varNum = (1 to Int.MaxValue).iterator

    private def nextVarName() = {
      "@var__" + varNum.next()
    }

    /**
     * Преобразование шагов БФ в инструкции
     * @param steps Шаги бизнес-функции
     * @return Клиенские и серверные инструкции
     */
    def analyseSteps(steps: Seq[FunctionStep], params: Map[String, Value]): Seq[ProcessTask] = {
      // преобразуем шаги выполнения бизнес-функции
      // в клиентские и серверные задачи
      val processTasks = steps.map(p=>analyseStep(p,params)).flatten
      //val splittedTasks = processTasks.map(splitTask).flatten
      processTasks
    }

    /**
     * Преобразование шагов бизнес-функции в серию инструкций
     * @param step Шаг бизнес-функции
     * @return Одна или несколько инструкций
     */
    private def analyseStep(step: FunctionStep,params: Map[String, Value]): Seq[ProcessTask] = {
      step.stepType match {
        case "_Meta_BFSCreateObject" => convertCreateObject(step)
        case "_Meta_BFSExecSP" => convertExecSP(step)
        case "_Meta_BFSCallAPI" => convertCallAPI(step)
        case "_Meta_BFSSetValue" => convertSetValue(step,params)
        case "_Meta_BFSSetAttributeValue" => convertSetAttrValue(step)
        case "_Meta_BFSTransition" => convertStateTransition(step)

        case "_Meta_BFSOpenFileDialog" => convertOpenFileDialog(step)
        case "_Meta_BFSOpenFolderDialog" => convertOpenFolderDialog(step)
        case "_Meta_BFSSaveFileDialog" => convertSaveFileDialog(step)
        case "_Meta_BFSReadFile" => convertReadFile(step)
        case "_Meta_BFSSaveFile" => convertSaveFile(step)
        case "_Meta_BFSPrintFormLite" => convertPrintFormLite(step)
        case "_Meta_BFSArchiveFiles" => convertArchiveFiles(step)
        case "_Meta_BFSPrint" => convertPrint(step)

        case "_Meta_BFSOpenCard" => convertOpenCard(step)
        case "_Meta_BFSOpenGrid" => convertOpenGrid(step)
        case "_Meta_BFSCloseForm" => convertCloseForm(step)
        case "_Meta_BFSjs" => convertExecJS(step)
        case "_Meta_BFSCloneObject" =>convertCloneObject(step)
        case "_Meta_BFSCloneArray" =>convertCloneArray(step)
        case "_Meta_BFSCreateECP" =>convertCreateECP(step)
        case "_Meta_BFSArrayToDBF" =>convertCreateDBF(step)
        case "_Meta_BFSCheckECP" =>convertCheckECP(step)
        case "_Meta_BFSCallBF" => convertCallBF(step)
        case "_Meta_BFSCreateByClassTemplate" => convertCreateByClassTemplate(step)
        case "_Meta_BFSUpdateName" => convertUpdateName(step)
        case _ => defaultTask(step)
      }
    }
    private def convertUpdateName(step:FunctionStep):Seq[ProcessTask]={
      val updateName = UpdateNameTask(step.description)
      Seq(updateName)
    }
    private def convertCreateByClassTemplate(step:FunctionStep):Seq[ProcessTask]={
      val createByTemplate = CreateByClassTemplate(step.description)
      Seq(createByTemplate)
    }
    private def convertCallBF(step:FunctionStep):Seq[ProcessTask]={
      val callBF = CallBF(step.description)
      Seq(callBF)
    }
    private def convertCheckECP(step:FunctionStep):Seq[ProcessTask]={
      val checkECPStep = CheckECP(step.description)
      val resultVar = nextVarName()
      val fioVar = nextVarName()
      val dataVar = nextVarName()
      checkECPStep.destination = resultVar
      checkECPStep.fioVar = fioVar
      checkECPStep.dataVar = dataVar
      val clientCheckECP = ClientCheckECP(resultVar,fioVar,dataVar)
      Seq(checkECPStep,clientCheckECP)
    }
    private def convertCreateDBF(step:FunctionStep):Seq[ProcessTask]={
       val createDBFStep = CreateDBF(step.description)
       Seq(createDBFStep)
    }
    private def convertCreateECP(step:FunctionStep):Seq[ProcessTask]={
        val objectExpr=  step.description.attribute("Object").map(_.text).orNull
        val dataVar = nextVarName()
        val dataSignVar = nextVarName()
        val signRecordVar = nextVarName()
        val dataSetTask = ECPDataSet(objectExpr,dataVar)
        val clientECPTask =  ClientECP(dataVar,dataSignVar)
        val createObjectTask = CreateObject(null,"CC947029-B264-4DEC-B1D2-83B4D88B29D5",signRecordVar)
        val setParentTask = new SetAttributeValue(objectExpr,signRecordVar+".ObjectID")
        val setData = new  SetAttributeValue(dataVar,signRecordVar+".Document")
        val setSign = new  SetAttributeValue(dataSignVar,signRecordVar+".Sign")
        Seq(dataSetTask,clientECPTask,createObjectTask,setParentTask,setData,setSign)
    }
    private def convertCloseForm(step:FunctionStep):Seq[ProcessTask] ={
      val CloseTask = new CloseForm()
      Seq(CloseTask)
    }
    private def convertCloneArray(step:FunctionStep):Seq[ProcessTask] ={
       val cloneArrayTask = CloneArray(step.description)
       Seq(cloneArrayTask)
    }


    private def convertCloneObject(step:FunctionStep):Seq[ProcessTask]={
        val  cloneTask = CloneObject(step.description)
       Seq(cloneTask)
    }

    private def convertCreateObject(step: FunctionStep): Seq[ProcessTask] = {
      val sourceTask = CreateObject(step.description)
      if (sourceTask.sourceClassID == null) {
        // выделяем клиентские шаги из выражения
        val (newExp, subst) = extractClientExpressions(sourceTask.source)

        // формируем шаги для получения данных с клиента
        val clientValueTasks = getClientValueTasks(subst)

        // формируем дополнительный серверный шаг при необходимости
        val resultTask = if (clientValueTasks.isEmpty)
          sourceTask
        else {
          new CreateObject(newExp, sourceTask.sourceClassID, sourceTask.destination,sourceTask.codeExpressionMap)
        }

        clientValueTasks ++ Seq(resultTask)
      }
      else Seq(sourceTask)
    }

    private def convertExecSP(step: FunctionStep): Seq[ProcessTask] = {
      val singleTask = ExecStoredProc(step.description)
      Seq(singleTask)
    }

    private def convertExecJS(step: FunctionStep): Seq[ProcessTask] = {
      val singleTask = ExecJs(step.description)
      Seq(singleTask)
    }

    private def convertCallAPI(step: FunctionStep): Seq[ProcessTask] = {
      val sourceTask = CallAPI(step.description)
      Seq(sourceTask)
    }

    private def convertSetAttrValue(step: FunctionStep): Seq[ProcessTask] = {
      val singleTask = SetAttributeValue(step.description)
      Seq(singleTask)
    }

    private def convertStateTransition(step: FunctionStep): Seq[ProcessTask] = {
      val singleTask = StateTransition(step.description)
      Seq(singleTask)
    }


    private def convertArchiveFiles(step: FunctionStep): Seq[ProcessTask] = {
      val fullNameVar =   nextVarName()
      val zipFileVar = nextVarName()

      val archiveTask = ArchiveFiles (step.description,zipFileVar)
      val setValueTask = new SetValue(fullNameVar, archiveTask.fullPathExp)
      val saveFileTask = SaveFile(fullNameVar,zipFileVar,"Y")

      //Save file step
     // val singleTask = StateTransition(step.description)
      Seq(archiveTask,setValueTask,saveFileTask)
    }
    private def convertSetValue(step: FunctionStep,params: Map[String, Value]): Seq[ProcessTask] = {
      val sourceTask = SetValue(step.description)

      val (newExp, subst) = extractClientExpressions(sourceTask.sourceExp)

      // формируем шаги для получения данных с клиента
      val clientValueTasks = getClientValueTasks(subst)
      //если переменная запрпашиваемая с клиента уже есть в контексте то пропустить текущий шаг
      if(!params.get(sourceTask.destination.replaceFirst("\\@", "")).isEmpty && !clientValueTasks.isEmpty) return Seq()
      // формируем дополнительный серверный шаг при необходимости
      val setValueTask = if (clientValueTasks.isEmpty)
        sourceTask
      else {
        new SetValue(sourceTask.destination, newExp)
      }

      clientValueTasks ++ Seq(setValueTask)
    }

    private def convertOpenFileDialog(step: FunctionStep): Seq[ProcessTask] = {
      val singleTask = OpenFileDialog(step.description)
      Seq(singleTask)
    }

    private def convertOpenFolderDialog(step: FunctionStep): Seq[ProcessTask] = {
      val singleTask = OpenFolderDialog(step.description)
      Seq(singleTask)
    }


    private def convertSaveFileDialog(step: FunctionStep): Seq[ProcessTask] = {
      val singleTask = SaveFileDialog(step.description)
      Seq(singleTask)
    }


    private def convertReadFile(step: FunctionStep): Seq[ProcessTask] = {
      val singleTask = ReadFile(step.description)
      Seq(singleTask)
    }

    private def convertSaveFile(step: FunctionStep): Seq[ProcessTask] = {
      val singleTask = SaveFile(step.description)
      Seq(singleTask)
    }

    private def convertPrintFormLite(step: FunctionStep): Seq[ProcessTask] = {
      val templateId = step.description.attribute("TemplateID").map(_.text).orNull
//      val datasetvar = nextVarName()
//      val selectVar = nextVarName()
//      val getSelectionStep = new GetClientValue(selectVar, "#SELECTION(ID)")
//      val serverDateSet = GetFormGridDataSet(selectVar,datasetvar)
      val dataVar = nextVarName()
      val treeIdVar = nextVarName()
      val selectVar = nextVarName()
      val treeDataVar = nextVarName()
      val getSelectionStep = new GetClientValue(selectVar, "#SELECTION(ID)")
      val getTreeIdStep = new GetClientValue(treeIdVar,"#TREEID")
      val getTreeDataStep = new GetClientValue(treeDataVar,"#TREEPARAMETER")
      val serverDateSet = GetFormGridDataSet(selectVar,treeIdVar,treeDataVar,dataVar,templateId)
      val dataSteps = Seq(getTreeIdStep,getTreeDataStep,getSelectionStep,serverDateSet)

      val singleTask = PrintFormLite(step.description,dataVar)
      dataSteps++ Seq(singleTask)
    }

    private def convertPrint(step: FunctionStep): Seq[ProcessTask] = {
      val templateId = step.description.attribute("TemplateID").map(_.text).orNull
      val isGridDesigned =  step.description.attribute("IsGridDesigned").map(_.text=='Y').getOrElse(false)
      val contentVar = nextVarName()
      val dataVar = nextVarName()
      val treeIdVar = nextVarName()
      val selectVar = nextVarName()
      val treeDataVar = nextVarName()
      val getSelectionStep = new GetClientValue(selectVar, "#SELECTION(ID)")
      val getTreeIdStep = new GetClientValue(treeIdVar,"#TREEID")
      val getTreeDataStep = new GetClientValue(treeDataVar,"#TREEPARAMETER")
      val serverDateSet = GetFormGridDataSet(selectVar,treeIdVar,treeDataVar,dataVar,templateId)
      val dataSteps = Seq(getTreeIdStep,getTreeDataStep,getSelectionStep,serverDateSet)  // ( if(isGridDesigned) Seq(getTreeIdStep,getTreeDataStep) else Seq()) ++ Seq(getSelectionStep,serverDateSet)


      val execSPTask = new ExecStoredProc(
        procedureName = "_Report_PFCQueue.GenerateDoc",
        params = Map(
          "ObjectID" -> null,
          "ObjectData" -> dataVar,
          "TemplateID" -> templateId,
          "ResultType" -> "pdf"
        ),
        outParams = Map(
          contentVar.replaceFirst("\\@", "") -> "#CELL(1,1,@Result)",
          "lastPrintResult" -> "#CELL(1,1,@Status_Name)"
        ),
        independent = true,
        timeOut = 120
      )
      val clientPrintTask = new ClientPrint(contentVar)

      dataSteps++ Seq( execSPTask, clientPrintTask)
    }

    private def convertOpenCard(step: FunctionStep): Seq[ProcessTask] = {
      val sourceTask = OpenCard(step.description)
      val (newExp, subst) = extractClientExpressions(sourceTask.target)

      // формируем шаги для получения данных с клиента
      val clientValueTasks = getClientValueTasks(subst)

      // формируем серверный шаг
      val objVarName = nextVarName()
      val setValueTask = new SetValue(objVarName, newExp)

      // формиоруем клиентский шаг получения карты
      val clientTask = new OpenCard(sourceTask.name, objVarName)

      clientValueTasks ++ Seq(setValueTask, clientTask)
    }

    private def convertOpenGrid(step: FunctionStep): Seq[ProcessTask] = {
      val sourceTask = OpenGrid(step.description)
      if (sourceTask.target != null) {
        val (newExp, subst) = extractClientExpressions(sourceTask.target)

        // формируем шаги для получения данных с клиента
        val clientValueTasks = getClientValueTasks(subst)

        // формируем серверный шаг
        val objVarName = nextVarName()
        val setValueTask = new SetValue(objVarName, newExp)

        // формируем клиентский шаг получения карты
        val clientTask = new OpenGrid(sourceTask.name, objVarName, sourceTask.field)

        clientValueTasks ++ Seq(setValueTask, clientTask)
      } else {
        // формируем клиентский шаг получения карты
        val clientTask = new OpenGrid(sourceTask.name, sourceTask.target, sourceTask.field)

        Seq(clientTask)
      }
    }

    private def defaultTask(step: FunctionStep): Seq[ProcessTask] = {
      val singleTask = new ProcessTask(step.stepType)
      Seq(singleTask)
    }

    val ClientFunctionTemplate = """#[\p{Lu}\p{Ll}_][\p{Lu}\p{Ll}_0-9]*(\(\s*([^\s]+\s*(,\s*[^\s]+\s*)*)?\))?"""r
    private class MemoReplacer extends (Regex.Match => String) {
      private val substMap = scala.collection.mutable.Map[String, String]()
      def subst = substMap.toMap
      def apply(v: Match) = {
        val name = nextVarName()
        substMap += (name -> v.matched)
        name
      }
    }

    private def extractClientExpressions(exp: String): (String, Map[String, String]) = {
      val replacer = new MemoReplacer
      val resultExp = ClientFunctionTemplate.replaceAllIn(exp, replacer)
      resultExp -> replacer.subst
    }

    private def getClientValueTasks(variables: Map[String, String]): Seq[GetClientValue] = {
      if (variables.isEmpty) {
        Seq()
      } else {
        variables.map(_ match {case (k, v) => new GetClientValue(k, v)}).toSeq
      }
    }
  }

}
