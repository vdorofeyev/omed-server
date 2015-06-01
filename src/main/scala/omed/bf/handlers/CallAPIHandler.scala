package omed.bf.handlers

import com.google.inject.Inject

import omed.bf._
import omed.model.{DataType,MetaClassProvider, SimpleValue, Value, EntityInstance}
import omed.model.services.{SystemTriggerProvider, ExpressionEvaluator}
import omed.bf.tasks.CallAPI
import omed.data.{EntityFactory, DataReaderService, SettingsService, DataAwareConfiguration}
import java.net.URL
import java.io._
import xml.XML
import omed.db.{DB, ConnectionProvider, DataAccessSupport}
import java.util.Date
import omed.system.ContextProvider
import omed.fer.{TimetableUtil, FERService}
import omed.dicom.DICOMUtil
import omed.errors.DataError
import com.taskadapter.redmineapi.RedmineManager
import com.aragost.javahg.{RepositoryConfiguration, BaseRepository, Repository}
import com.aragost.javahg.commands.{AddCommand, CommitCommand}
import com.aragost.javahg.commands.flags.{PushCommandFlags, AddCommandFlags, CommitCommandFlags}
import omed.bf.BusinessFunctionStepLog
import com.aragost.javahg.internals.AbstractCommand
import scala.collection.mutable
import omed.roi.{ROIProvider}

class CallAPIHandler extends ProcessStepHandler with DataAccessSupport {

  @Inject
  var expressionEvaluator: ExpressionEvaluator = null
  @Inject
  var configProvider: ConfigurationProvider = null
  @Inject
  var connectionProvider: ConnectionProvider = null
  @Inject
  var contextProvider: ContextProvider = null
  @Inject
  var settingsService: SettingsService = null
  @Inject
  var dataReaderService: DataReaderService = null
  @Inject
  var metaClassProvider: MetaClassProvider = null
  @Inject
  var businessFunctionLogger:BusinessFunctionLogger = null
  @Inject
  var ferSyncronizeProvider:FERSyncronizeProvider = null

  @Inject
  var roiProvider:ROIProvider = null
  @Inject
  var systemTriggerProvider:SystemTriggerProvider = null
  @Inject
  var entityFactory:EntityFactory = null
  override val name = "_Meta_BFSCallAPI"

  val functions = Map[String, Map[String, Value] => Map[String, Value]](
    "ERZ74ByPolicy" -> ERZ74ByPolicy,
    "ERZ74ByPassport" -> ERZ74ByPassport,
    "ERZ74ByInsurance" -> ERZ74ByInsurance,
    "ERZ74ByBasicData" -> ERZ74ByBasicData,
    "FER_PostRules" -> postTimetableRulesToFER,
    "FER_PostLocations" -> postLocationsToFER,
    "FER_PutLocationSchedule" -> putLocationScheduleToFER,
    "FER_PutActivateLocation" -> putActivateLocationToFER,
    "FER_PostReserve" -> postReserveToFER,
    "FER_PutSlot" -> putSlotToFER,
    "FER_DeleteSlot" -> deleteSlotFromFER,
    "FER_GetReserve"->getReserveFromFER,
    "FER_GetTimes"->getTimesFromFER,
    "FER_GetLocations"->getLocationsFromFER,
    "FER_LoadSlots" ->getSlotsFromFER,
    "DICOM_SendModalityList"-> sendModalityListToDICOM,
    "RedmineApiUpdateIssue"->redmineIssueUpdate,
    "MercurialCommit"->mercurialCommit,
    "DropCache"->dropCache,
    "ROI_Import" ->importFromROI,
    "ROI_Export"->exportToROI,
    "SQL_compile"->compileSQL,
    "UpdateName"->updateName
)

  def handle(task: ProcessTask, context: Map[String, Value],processId:String): Map[String, Value] = {
    val targetTask = task.asInstanceOf[CallAPI]

    val taskParams = targetTask.params.mapValues(paramSource => {
      expressionEvaluator.evaluate(paramSource, configProvider.create(), context)
    })
    businessFunctionLogger.addLogStep(processId,new BusinessFunctionStepLog("Шаг вызова функции сервера приложений: " + targetTask.functionName,Map(),taskParams))
    val func = functions(targetTask.functionName)

    func(taskParams)
  }

  def updateName (params: Map[String, Value]): Map[String, Value] = {
    val classCode = params("classCode").toString
    val data = dataReaderService.getClassDataFromAllDomain(classCode)
    data.foreach(f => systemTriggerProvider.updateName(entityFactory.createEntityWithData(f)))
    Map()
  }

  def compileSQL (params: Map[String, Value]): Map[String, Value] = {
    val expr = params("expr").toString
    val targetClass = params("class").toString
    val compiled = expressionEvaluator.compileSQL(expr,configProvider.create(),targetClass,null,Map())
    Map("__CompiledSQL"->compiled)
  }
           /* РОИ */

  def importFromROI (params: Map[String, Value]): Map[String, Value] = {
     roiProvider.loadExpertiseFromROI(params("old").asInstanceOf[SimpleValue].data.asInstanceOf[Boolean])
     Map()
  }

  def exportToROI  (params: Map[String, Value]): Map[String, Value] = {
   roiProvider.sentExpertiseToROI
    Map()
  }

          /*  ERZ */

  def getBaseERZ74Url = {
    val dbResult = connectionProvider withConnection {
      connection =>
        dataOperation {
          DB.dbExec(connection, "_Meta.GetSettings", contextProvider.getContext.sessionId, null)
        }
    }
    val settings = scala.collection.mutable.Map[String, String]()
    val meta = dbResult.getMetaData()
    val columnSeq = for (i <- 1 to meta.getColumnCount())
      yield meta.getColumnName(i)

    while (dbResult.next()) {
      val key = dbResult.getString(columnSeq.indexOf("Code") + 1)
      val value = dbResult.getString(columnSeq.indexOf("StrValue") + 1)
      settings.put(key, value)
    }
    val result = settings("URLERZ74Service")
    result
  }
  def getResult(url: String) = {
    val xmlResp = XML.load(new URL(url))

    val err = xmlResp \\ "response" \\ "err"
    val msg = xmlResp \\ "response" \\ "msg"
    val orgCode = xmlResp \\ "response" \\ "org"
    val policy = xmlResp \\ "response" \\ "policy"

    Map("err" -> SimpleValue(err.text.toInt),
      "msg" -> SimpleValue(msg.text),
      "org" -> SimpleValue(orgCode.text),
      "policy" -> SimpleValue(policy.text))
  }
  def formatDate(date: Date) = {
    val DateFormat = "dd.MM.yyyy"
    val format = new java.text.SimpleDateFormat(DateFormat)
    format.format(date)
  }
  def addBirthday(params: Map[String, Value], url: String): String = {
    if (params.contains("Birthday")) {
      val birthDay = params("Birthday").asInstanceOf[SimpleValue].data
      birthDay match {
        case null => url
        case d: java.sql.Date =>
          url + "&Birthday=" + formatDate(d)
      }
    } else {
      url
    }
  }
  def ERZ74ByPolicy(params: Map[String, Value]): Map[String, Value] = {
    var url = getBaseERZ74Url + "/Home/QueryByPolicy?PolicyNumber=" +
      java.net.URLEncoder.encode(params("PolicyNumber").toString, "utf8")
    url = addBirthday(params, url)
    getResult(url)
  }
  def ERZ74ByPassport(params: Map[String, Value]): Map[String, Value] = {
    var url = getBaseERZ74Url + "/Home/QueryByPassport?Passport=" +
      java.net.URLEncoder.encode(params("Passport").toString, "utf8")
    url = addBirthday(params, url)
    getResult(url)
  }
  def ERZ74ByInsurance(params: Map[String, Value]): Map[String, Value] = {
      var url = getBaseERZ74Url + "/Home/QueryByInsurance?Insurance=" +
        java.net.URLEncoder.encode(params("Insurance").toString.replace("-", "").replace(" ", ""), "utf8")
      url = addBirthday(params, url)
      getResult(url)
  }
  def ERZ74ByBasicData(params: Map[String, Value]): Map[String, Value] = {
    var url = getBaseERZ74Url + "/Home/QueryByBasicData?Surname=" +
      java.net.URLEncoder.encode(params("Surname").toString, "utf8") +
      "&Name=" +
      java.net.URLEncoder.encode(params("Name").toString, "utf8") +
      "&Patronymic=" +
      java.net.URLEncoder.encode(params("Patronymic").toString, "utf8")
    url = addBirthday(params, url)
    getResult(url)
  }

           /* FER*/

  def getFERAddress = settingsService.getGlobalSettings("FERAdapterURL").map(_.strValue).getOrElse("default-address")
  def getFERToken = {
    val token = settingsService.getDomainSettings("FERToken").map(_.strValue)
    if(token.isEmpty) throw  new DataError("Не найден токен ФЭР ", -2345)
    token.get
  }
  def getLocationsFromFER(params: Map[String, Value]): Map[String, Value] = {
    TimetableUtil(connectionProvider, getFERAddress, getFERToken).getLocations(contextProvider.getContext.sessionId)
    Map[String, Value]()
  }
  def getSlotsFromFER (params: Map[String, Value]): Map[String, Value] = {
    val date = if(params.contains("date")) params("date") else throw new RuntimeException("Не задан параметр дата у функции getSlotsFromFER")
    val dateStr = date.dataType match {
      case DataType.Date =>  new java.text.SimpleDateFormat("yyyy-MM-dd").format(date.asInstanceOf[SimpleValue].data)
      case DataType.String => date.toString
    }

    val locations = TimetableUtil(connectionProvider, getFERAddress, getFERToken).getLocations(contextProvider.getContext.sessionId)
    val slots = locations.map(f=> f-> TimetableUtil(connectionProvider, getFERAddress, getFERToken).getTimes(f, dateStr)).toMap
    ferSyncronizeProvider.addFerSlots(slots,dateStr)
    Map[String, Value]()
  }
  def getTimesFromFER(params: Map[String, Value]): Map[String, Value] = {
    val locationId = params("locationId").getId
    val date =  params("date").toString
    TimetableUtil(connectionProvider, getFERAddress, getFERToken).getTimes(locationId, date)
    Map[String, Value]()
  }
  def getReserveFromFER(params: Map[String, Value]): Map[String, Value] = {
    TimetableUtil(connectionProvider, getFERAddress, getFERToken).getReserve(contextProvider.getContext.sessionId)
    Map[String, Value]()
  }

  def postTimetableRulesToFER(params: Map[String, Value]): Map[String, Value] = {
    if (params.contains("ID")) {
      val timetableId = params("ID") match {
        case sv: SimpleValue => sv.data.asInstanceOf[String]
        case ev: EntityInstance => ev.getId
      }

      TimetableUtil(connectionProvider, getFERAddress, getFERToken).postRules(contextProvider.getContext.sessionId, timetableId)
    }

    Map[String, Value]()
  }
  def postLocationsToFER(params: Map[String, Value]): Map[String, Value] = {
    if (params.contains("ID")) {
      val timetableId = params("ID") match {
        case sv: SimpleValue => sv.data.asInstanceOf[String]
        case ev: EntityInstance => ev.getId
      }

      TimetableUtil(connectionProvider, getFERAddress, getFERToken).postLocations(contextProvider.getContext.sessionId, timetableId)
    }

    Map[String, Value]()
  }
  def putLocationScheduleToFER(params: Map[String, Value]): Map[String, Value] = {
    if (params.contains("ID")) {
      val timetableId = params("ID") match {
        case sv: SimpleValue => sv.data.asInstanceOf[String]
        case ev: EntityInstance => ev.getId
      }

      TimetableUtil(connectionProvider, getFERAddress, getFERToken).putLocationSchedule(contextProvider.getContext.sessionId, timetableId)
    }

    Map[String, Value]()
  }
  def putActivateLocationToFER(params: Map[String, Value]): Map[String, Value] = {
    if (params.contains("ID")) {
      val timetableId = params("ID") match {
        case sv: SimpleValue => sv.data.asInstanceOf[String]
        case ev: EntityInstance => ev.getId
      }

      TimetableUtil(connectionProvider, getFERAddress, getFERToken).putActivateLocation(contextProvider.getContext.sessionId, timetableId)
    }

    Map[String, Value]()
  }
  def postReserveToFER(params: Map[String, Value]): Map[String, Value] = {
    if (params.contains("SlaveTimeTableID")) {
      val timetableId = params("SlaveTimeTableID") match {
        case sv: SimpleValue => sv.data.asInstanceOf[String]
        case ev: EntityInstance => ev.getId
      }

      TimetableUtil(connectionProvider, getFERAddress, getFERToken).postReserve(contextProvider.getContext.sessionId, timetableId)
    }

    Map[String, Value]()
  }
  def putSlotToFER(params: Map[String, Value]): Map[String, Value] = {
    if (params.contains("SlaveTimeTableID")) {
      val timetableId = params("SlaveTimeTableID") match {
        case sv: SimpleValue => sv.data.asInstanceOf[String]
        case ev: EntityInstance => ev.getId
      }

      TimetableUtil(connectionProvider, getFERAddress, getFERToken).putSlot(contextProvider.getContext.sessionId, timetableId)
    }

    Map[String, Value]()
  }
  def deleteSlotFromFER(params: Map[String, Value]): Map[String, Value] = {
    if (params.contains("SlaveTimeTableID")) {
      val timetableId = params("SlaveTimeTableID") match {
        case sv: SimpleValue => sv.data.asInstanceOf[String]
        case ev: EntityInstance => ev.getId
      }

      TimetableUtil(connectionProvider, getFERAddress, getFERToken).deleteSlot(contextProvider.getContext.sessionId, timetableId)
    }

    Map[String, Value]()
  }

          /* DICOM   */

  def getDICOMServerAddress = settingsService.getGlobalSettings("DICOMServerURL").map(_.strValue).getOrElse(null)
  def sendModalityListToDICOM(params: Map[String, Value]): Map[String, Value] ={
      DICOMUtil(connectionProvider,getDICOMServerAddress).postModalities()
      Map[String, Value]()
  }
  def postStudyToWorklist(params: Map[String, Value]): Map[String, Value] = {
    DICOMUtil
    Map[String,Value]()
  }

        /* Redmine & Mercurial*/

  def redmineIssueUpdate(params: Map[String, Value]): Map[String, Value] ={
          val xml = params("value")
          val host = settingsService.getGlobalSettings("RedmineURL").map(_.strValue).getOrElse(null)
          if(host==null) new DataError("Не найдена строка подключения Redmine (RedmineURL)", -2345)
          val id = params("issueID").toString.toFloat.toInt // params
          val apiAccessKey = params("apiAccessKey").toString
          val mgr = new RedmineManager(host,apiAccessKey.toString)
          val issue = mgr.getIssueById(id)


          val value = scala.xml.XML.loadString(params("value").toString)
          val estimate = (value \\ "estimated_hours").text.toFloat
          val version = mgr.getVersionById((value \\ "fixed_version").text.toInt )
          issue.setEstimatedHours(estimate)
          issue.setTargetVersion(version)
          mgr.update(issue)
          //  mgr.up
          // issue.setTargetVersion( new com.taskadapter.redmineapi.bean.Version())
          Map[String,Value]()
        }
  def mercurialCommit (params: Map[String, Value]): Map[String, Value] = {
    try{
    val address = "\\\\localhost\\metadata"// "\\\\10.109.1.6\\MetaData"
    val data = params("data").toString.replaceAll(">",">\n")
    val filename = params("file").toString
    val transiltName = traslitString(filename)
    val user = params("user").toString
    val repository =  Repository.open(new java.io.File(address))

    val file =  new java.io.File( address+ "\\Export Modules\\"+transiltName+".xml") // new java.io.File("\\\\10.109.1.105\\MetaData\\Export Modules\\"+transiltName+".xml")
    val needAdd = !file.exists()
    var out:PrintStream = null
    try {
      out = new PrintStream(new FileOutputStream(file))
      out.print(data)
    }
    finally {
      if (out != null) out.close()
    }
    if(needAdd) {
      val addCommand = AddCommandFlags.on(repository)
      val list = addCommand.execute(file)
    }

    val commitCmd = CommitCommandFlags.on(repository).message("Модуль "+ filename + " выгружен пользователем "+ user).user(user)

    val changeSet = commitCmd.execute(file)
    val pushCommand = PushCommandFlags.on(repository)
    val res = pushCommand.execute() //"http://10.109.1.6:80/hg/metadata"  //"\\\\10.109.1.6\\hg\\metadata")
    }
    catch{
      case _ => null
    }
    Map()
  }
  def traslitString(string:String):String={
    val charMap = new mutable.HashMap[java.lang.Character, String]()
      charMap.put('А', "A")
      charMap.put('Б', "B")
      charMap.put('В', "V")
      charMap.put('Г', "G")
      charMap.put('Д', "D")
      charMap.put('Е', "E")
      charMap.put('Ё', "E")
      charMap.put('Ж', "Zh")
      charMap.put('З', "Z")
      charMap.put('И', "I")
      charMap.put('Й', "I")
      charMap.put('К', "K")
      charMap.put('Л', "L")
      charMap.put('М', "M")
      charMap.put('Н', "N")
      charMap.put('О', "O")
      charMap.put('П', "P")
      charMap.put('Р', "R")
      charMap.put('С', "S")
      charMap.put('Т', "T")
      charMap.put('У', "U")
      charMap.put('Ф', "F")
      charMap.put('Х', "H")
      charMap.put('Ц', "C")
      charMap.put('Ч', "Ch")
      charMap.put('Ш', "Sh")
      charMap.put('Щ', "Sh")
      charMap.put('Ъ', "'")
      charMap.put('Ы', "Y")
      charMap.put('Ь', "'")
      charMap.put('Э', "E")
      charMap.put('Ю', "U")
      charMap.put('Я', "Ya")
      charMap.put('а', "a")
      charMap.put('б', "b")
      charMap.put('в', "v")
      charMap.put('г', "g")
      charMap.put('д', "d")
      charMap.put('е', "e")
      charMap.put('ё', "e")
      charMap.put('ж', "zh")
      charMap.put('з', "z")
      charMap.put('и', "i")
      charMap.put('й', "i")
      charMap.put('к', "k")
      charMap.put('л', "l")
      charMap.put('м', "m")
      charMap.put('н', "n")
      charMap.put('о', "o")
      charMap.put('п', "p")
      charMap.put('р', "r")
      charMap.put('с', "s")
      charMap.put('т', "t")
      charMap.put('у', "u")
      charMap.put('ф', "f")
      charMap.put('х', "h")
      charMap.put('ц', "c")
      charMap.put('ч', "ch")
      charMap.put('ш', "sh")
      charMap.put('щ', "sh")
      charMap.put('ъ', "'")
      charMap.put('ы', "y")
      charMap.put('ь', "'")
      charMap.put('э', "e")
      charMap.put('ю', "u")
      charMap.put('я', "ya")

      val transliteratedString = new StringBuilder()
      for ( i  <- 0 to string.length()-1) {
        val ch = string.charAt(i)
        val charFromMap = charMap.get(ch)
        if (charFromMap.isEmpty) {
          transliteratedString.append(ch)
        } else {
          transliteratedString.append(charFromMap.get)
        }
      }
       transliteratedString.toString()
  }


  def dropCache (params: Map[String, Value]): Map[String, Value] = {
    metaClassProvider.dropCache(dataReaderService.getDomains())
    Map()
  }


}