package omed.roi
import scala.collection.JavaConversions._
import com.sun.jersey.api.client.{Client, ClientResponse, WebResource}
import javax.ws.rs.core.MediaType
import omed.data.{DataReaderService, DataWriterService, SettingsService}
import com.google.inject.Inject

import com.google.gson.GsonBuilder
import java.util
import omed.db._
import omed.system.ContextProvider
import omed.cache.ExecStatProvider
import java.util.{TimeZone, Calendar, Date}
import javax.net.ssl._
import java.security.cert.X509Certificate
import scala.collection.mutable.ArrayBuffer
import java.security.SecureRandom
import omed.lang.eval.DBUtils


/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 02.12.13
 * Time: 19:49
 * To change this template use File | Settings | File Templates.
 */
//test class for json serelization

class ROIProviderImpl  extends ROIProvider with DataAccessSupport{
  @Inject
  var settingsService:SettingsService = null
  @Inject
  var connectionProvider:ConnectionProvider = null
  @Inject
  var dataWriterService: DataWriterService =null
  @Inject
  var dataReaderService: DataReaderService =null
  @Inject
  var dbProvider: DBProvider = null
  @Inject
  var contextProvider: ContextProvider = null
  @Inject
  var execStatPrivider:ExecStatProvider = null
  val ExpertiseQuery =
    """
      | select ID,IdInitiative,NoVoting,ToVoting from Ueoi_Expertise
      |  where _StatusID ='980A5E94-B668-4F15-802D-4E930BAC8B99' and (isExportedToRoi ='N' or isExportedToRoi is null)
    """.stripMargin

//  """
//    | select IdInitiative,NoVoting,ToVoting from Ueoi_Expertise
//    |  where _StatusID in ('91F3B422-6808-468B-BFFB-6CD08E1E4F2F','69746F50-5A22-49AD-BEA4-A1FB5A8E128E','980A5E94-B668-4F15-802D-4E930BAC8B99') and (isExportedToRoi ='N' or isExportedToRoi is null)
//    |
//  """.stripMargin
  val AttachedDocumentQuery  =
    """
      |select * from Ueoi_AttachedLinks
    """.stripMargin

  private implicit def bool2int(b:Boolean) = if (b) 1 else 0
  private  def intToDBBool(b:Int) = if (b==0) "N" else "Y"
  private def getToken:String ={
    val token :String = settingsService.getDomainSettings("ROIToken").map(_.strValue).getOrElse(null) //get From settings
    if(token==null)  throw new RuntimeException("Не задан токен подключения к РОИ")
    token
  }
  private def loadSLL(){
    val trustAllCerts = new ArrayBuffer[TrustManager]()
    trustAllCerts.append(new X509TrustManager(){
      def getAcceptedIssuers():Array[X509Certificate] = { null}
      def checkClientTrusted( certs : Array[X509Certificate], authType:String ){}
      def  checkServerTrusted( certs: Array[X509Certificate], authType:String ){}
    })
    val sc = SSLContext.getInstance("SSL")
    sc.init(null, trustAllCerts.toArray, new SecureRandom())
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())

    val allHostsValid = new HostnameVerifier() {
      def  verify( hostname :String,  session:SSLSession) :Boolean={
        true
      }
    }
    HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)
  }
  def loadExpertiseFromROI(old:Boolean){
    val urlString :String = settingsService.getDomainSettings("ROIExportURL").map(_.strValue).getOrElse(null) //get From settings
    if(urlString==null)  throw new RuntimeException("Не задан адрес подключения к РОИ")
    loadSLL()
    val client = Client.create()
    var resource: WebResource = client.resource(urlString)
    val response: ClientResponse = if(old) resource.header("X_TOKEN",getToken).header("Content-Type", "application/json;charset=UTF-8").header("X_FORCE_STATUS","200").post(classOf[ClientResponse])
    else  resource.header("X_TOKEN",getToken).header("Content-Type", "application/json;charset=UTF-8").post(classOf[ClientResponse])
    if(response.getStatus==304) throw new RuntimeException("Новых данных нет")
    if(response.getStatus!=200) throw new RuntimeException("Ошибка при получении данных " + response.getEntity(classOf[String]))
    val jsonResult = response.getEntity(classOf[String])
    val experties:UeoiImportExpertiseArray = new GsonBuilder().create().fromJson(jsonResult,classOf[UeoiImportExpertiseArray])
    importExpertiesToDB(experties.data)
  }
  def sentExpertiseToROI{
     val (ids,expertiesSeq) = loadExpertiseFromDB(null,null)
     val experties = UeoiExportExpertiseArray(expertiesSeq)
    if (experties.data.length==0) throw new RuntimeException("Отсутствуют экспертизы для выгрузки")
     var jsonString =new GsonBuilder().create().toJson(experties)
     val responce = sendJsonStringToRoi(jsonString)
     if (responce.getStatus ==200) setExpertisesAsExported(ids)
     else throw new RuntimeException("Ошибка при отправке данных " + responce.getEntity(classOf[String]))
  }

  def exportExpertisesToRoi(begin:String,end:String):String={
    val (tmp,experties) = loadExpertiseFromDB(begin,end)
    new GsonBuilder().disableHtmlEscaping().create().toJson(experties.toArray)
  }
  def importExpertisesFromRoi(jsonString:String):Boolean={
    val experties = new GsonBuilder().create().fromJson(jsonString,classOf[UeoiImportExpertiseArray])
    importExpertiesToDB(experties.data)
    true
  }


  private def loadExpertiseFromDB(begin:String,end:String):(Seq[String],util.ArrayList[UeoiExportExpertise])={
    val dbResult = connectionProvider withConnection {
      connection =>
        dataOperation {
          val beginFilter = if(begin!=null) " and Exp.DateExpertise >= '" + begin + "'" else ""
          val endFilter = if(end!=null) " and Exp.DateExpertise <= '" + end + "'" else ""
          val statement = connection.prepareStatement(ExpertiseQuery + beginFilter + endFilter)
          statement.executeQuery()
        }
    }
    val experties =new util.ArrayList[UeoiExportExpertise]()// scala.collection.mutable.ArrayBuffer[UeoiExportExpertise]()
    val ids =new ArrayBuffer[String]()
    while (dbResult.next()) {
      experties+= UeoiExportExpertise(
                id = dbResult.getString("IdInitiative"),
                status = if(DBUtils.fromDbBoolean(dbResult.getString("ToVoting"))) "approve" else "reject",
                text = Option(dbResult.getString("NoVoting")).map(f => f).getOrElse("  ")
      )
      ids+= dbResult.getString("ID")
    }
    (ids.toSeq,experties)
  }

  private def sendJsonStringToRoi(jsonString : String):ClientResponse={
    val urlString :String = settingsService.getDomainSettings("ROIImportURL").map(_.strValue).getOrElse(null)// todo get url from parameters
    if(urlString==null)  throw new RuntimeException("Не задан адрес подключения к РОИ")
    loadSLL()
    val  client = Client.create
    val resource: WebResource = client.resource(urlString)
    val result = resource.header("X_TOKEN",getToken).header("Content-Type", "application/json;charset=UTF-8").post(classOf[ClientResponse], jsonString)
    result
  }
  private def setExpertisesAsExported(expertises:Seq[String]) {
      expertises.foreach(f => dataWriterService.editRecord(f,Map("isExportedToRoi"->"Y")))
  }
  private def importExpertiesToDB(experties:util.ArrayList[UeoiImportExpertise]){
      val expertiesSB = new StringBuilder
      val attachedLinksSB = new StringBuilder
      val levelDictionary = getLevelDictionary
      val cal = Calendar.getInstance()
      cal.setTimeZone(TimeZone.getTimeZone("GMT+0"))
      val importDate=new java.text.SimpleDateFormat(omed.model.DataType.DateTimeFormat).format(cal.getTime)

      val importDateFormat =new java.text.SimpleDateFormat("dd-MM-yyyy")
      val oldExpertises =  getImportedExpertises
      experties.filter(p => !oldExpertises.contains(p.id)).foreach(e=> {
         val guid = java.util.UUID.randomUUID.toString
         expertiesSB.append("<object " )
         appendAttribute(expertiesSB,"ID",guid)
         appendAttribute(expertiesSB,"IdInitiative",e.id)
         appendAttribute(expertiesSB,"SystemNumberInitiative",e.code)
         appendAttribute(expertiesSB,"NameInitiative",e.title)
         appendAttribute(expertiesSB,"Content",e.description)
         appendAttribute(expertiesSB,"Prospective",e.prospective)
         appendAttribute(expertiesSB,"Decision",if(e.decision!=null) e.decision.map(f =>f.text).mkString("\n") else null)
         appendAttribute(expertiesSB,"DateModeration", new java.text.SimpleDateFormat(omed.model.DataType.DateFormat).format(importDateFormat.parse(e.date.moderation.begin)))
         appendAttribute(expertiesSB,"ModeratorFIO",e.moderator.name)
         if(e.level !=null && e.level.id!=null) appendAttribute(expertiesSB,"Ueoi_SprLevelInitiative1ID",levelDictionary(e.level.id))
         if(e.level !=null && e.level.id!=null) appendAttribute(expertiesSB,"Ueoi_SprLevelInitiative2ID",levelDictionary(e.level.id))
         appendAttribute(expertiesSB,"_StatusID","8CA62519-F59F-48A1-BCF5-5646F99DA35B")
         appendAttribute(expertiesSB,"_ChangeDate",importDate.toString)
         appendAttribute(expertiesSB,"_CreateDate",importDate.toString)
         appendAttribute(expertiesSB,"IsSecondExpert","N" )
         appendAttribute(expertiesSB,"IsSecondLevel","N")
         expertiesSB.append("/>" )

         val documnets =Option(e.attachment).map(f => Option(f.document).map(p => p.toSeq).getOrElse((Seq())) ++Option(f.photo).map(p => p.toSeq).getOrElse((Seq()))).getOrElse(Seq())  ++ Option(e.decision).map(f => f.map (p => Option(p.attachment).map(e =>e.toSeq).getOrElse(Seq())).flatten).getOrElse(Seq())
       //  for (attache:UeoiAttachedLinks <- e.attach_links.toArray){
        documnets.foreach( attache => {
           val guid2 = java.util.UUID.randomUUID.toString
           attachedLinksSB.append("<object " )
           appendAttribute(attachedLinksSB,"ID",guid2)
           appendAttribute(attachedLinksSB,"Number",attache.title)
           appendAttribute(attachedLinksSB,"Descr",attache.description)
           appendAttribute(attachedLinksSB,"Link",attache.url)
           appendAttribute(attachedLinksSB,"Ueoi_ExpertiseID",guid)
           appendAttribute(attachedLinksSB,"_ChangeDate",importDate.toString)
           appendAttribute(attachedLinksSB,"_CreateDate",importDate.toString)
           attachedLinksSB.append("/>" )
         })
      })
    val expertiesData = "<?xml version=\"1.0\"?><root><meta MethodCode=\"_Import\"  ClassCode = \"Ueoi_Expertise\" /><data>"+ expertiesSB.toString()   + "</data></root>"
    val attachedLinksData = "<?xml version=\"1.0\"?><root><meta MethodCode=\"_Import\"  ClassCode = \"Ueoi_AttachedLinks\"/><data>"+ attachedLinksSB.toString()   + "</data></root>"
    callImportProcedure(expertiesData)
    callImportProcedure(attachedLinksData)
  }
  private def callImportProcedure(data:String){
    val result =DBProfiler.profile("Import from ROI",execStatPrivider ){
      connectionProvider withConnection {
        connection =>
          dataOperation {
            dbProvider.dbExecNoResultSet(connection, "[core].[Dispatch]",
              contextProvider.getContext.sessionId,
              List("Params"->data),execStatPrivider)}
      }
    }
  }

  private def appendAttribute(sb:StringBuilder,name :String, value:String){
    if(value == null) return
    val newValue = xml.Utility.escape(value)
    sb.append(" "+ name + "=\""+ newValue.toString + "\"")
  }
  private def getLevelDictionary:Map[String,String]={
    val dbResult = connectionProvider withConnection {
      connection =>
        dataOperation {
          val statement = connection.prepareStatement("select ID,CharCode from Ueoi_SprLevelInitiative")
          statement.executeQuery()
        }
    }
    val levelInitiatives = scala.collection.mutable.Map[String,String]()
    while (dbResult.next()) {
      levelInitiatives+=dbResult.getString("CharCode")->dbResult.getString("ID")
    }
    levelInitiatives.toMap
  }
  private def getImportedExpertises:Set[String]={
    val dbResult = connectionProvider withConnection {
      connection =>
        dataOperation {
          val statement = connection.prepareStatement("select IdInitiative from Ueoi_Expertise where IdInitiative is not null")
          statement.executeQuery()
        }
    }
    val expertises = scala.collection.mutable.Set[String]()
    while (dbResult.next()) {
      expertises+=dbResult.getString("IdInitiative")
    }
    expertises.toSet
  }
  val test = "{\n\"data\": [\n{\n\"id\": 9916,\n\"code\": \"77Р9916\",\n\"title\": \"Название первой инициативы\",\n\"category\": [\n\"Транспорт и дороги\",\n\"Инфраструктура города\"\n],\n\"description\": \"Первая строка описания.\\r\\nВтораястрока описания.\",\n\"prospective\": \"Ожидаемый практический результат.\",\n\"attachment\": {\n\"document\": [\n{\n\"title\": \"Имя первого документа.doc\",\n\"url\": \"https://www.roi.ru/upload/file_1.doc\",\n\"description\": \"Описание первого документа\"\n},\n{\n\"title\": \"Имя второго документа.pdf\",\n\"url\": \"https://www.roi.ru/upload/file_2.pdf\",\n\"description\": \"Описание второго документа\"\n}\n]\n},\n\"date\": {\n\"moderation\": {\n\"begin\": \"19-12-2013\"\n}\n},\n\"threshold\": 100000,\n\"level\": {\n\"id\": 2,\n\"title\": \"Региональный\"\n},\n\"location\": {\n\"region\": {\n\"id\": 77,\n\"title\": \"г. Москва\"\n}\n},\n\"moderator\": {\n\"id\": 35,\n\"name\": \"Имя Ответственного Модератора\"\n}\n},\n{\n\"id\": 9910,\n\"code\": \"77М9910\",\n\"title\": \"Название второй инициативы\",\n\"category\": [\n\"Безопасность\"\n],\n\"description\": \"Первая строка описания.\\r\\nВтораястрока описания.\",\n\"prospective\": \"Ожидаемый практический результат.\",\n\"decision\": [\n{\n\"text\": \"Текст первого решения\"\n},\n{\n\"text\": \"Текст второго решения\",\n\"attachment\": [\n{\n\"title\": \"Имя первого файла.doc\",\n\"url\": \"https://www.roi.ru/upload/decision_file_1.doc\",\n\"description\": \"Описание первого файла\"\n}\n]\n}\n],\n\"date\": {\n\"moderation\": {\n\"begin\": \"18-12-2013\"\n}\n},\n\"threshold\": 50000,\n\"level\": {\n\"id\": 3,\n\"title\": \"Муниципальный\"\n},\n\"location\": {\n\"region\": {\n\"id\": 77,\n\"title\": \"г. Москва\"\n},\n\"municipality\": {\n\"id\": 2393,\n\"title\": \"Арбат\"\n}\n},\n\"moderator\": {\n\"id\": 35,\n\"name\": \"Имя Ответственного Модератора\"\n}\n}]\n}"
}
