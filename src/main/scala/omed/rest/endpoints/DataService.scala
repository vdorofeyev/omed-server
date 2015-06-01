package omed.rest.endpoints

import javax.ws.rs._
import core.{Context, MediaType, Response}
import javax.servlet.http.HttpServletRequest

import com.google.inject.Inject

import omed.rest.model2xml.Model2Xml
import omed.data._
import omed.model._
import omed.system.{Context=>OmedContext, ContextProvider}
import omed.cache.{ExecStatProvider, CommonCacheService, DomainCacheService}
import omed.push.{PushNotificationSeq, PushNotificationService}
import omed.bf.{BusinessFunctionThreadPool, ValidationWarningPool}
import omed.db.DBProfiler
import omed.forms.MetaFormProvider
import scala.xml.XML

/**
 * Сервис работы с данными.
 * Методы сервиса позволяют добавлять, изменять и удалять записи,
 * добавлять записи в связанные списки, получать значения отдельных записей,
 * множество записей в виде списка, содержимое справочников и т.п.
 * Успешность выполненной операции можно определить по содержимому
 * ответа и HTTP-коду результата. В случае успеха, ответ предваряется кодом 200.
 */
@Path("/data")
@Produces(Array(MediaType.APPLICATION_XML))
class DataService {

  /**
   * Объектное представление HTTP-запроса, предоставляемое Java Servlet API
   */
  @Context
  var httpRequest: HttpServletRequest = null

  /**
   * Объект провайдера, обеспечивающий чтение данных из хранилища
   */
  @Inject
  var dataReaderService: DataReaderService = null

  /**
   * Объект провайдера, обеспечивающего изменение данных в хранилище
   */
  @Inject
  var dataWriterService: DataWriterService = null

  /**
   * Провайдер метаданных классов
   */
  @Inject
  var metaClassProvider: MetaClassProvider = null

  /**
   * Провайдер контекста предоставляет доступ к контексту,
   * объекту класса [[omed.system.Context]]
   */
  @Inject
  var contextProvider: ContextProvider = null

  /**
   * Менеджер кэша метаданных обеспечивает доступ
   * к ранее выбранным из хранилища описаниям классов и их полей.
   */
  @Inject
  var metaObjectCacheManager: MetaObjectCacheManager = null

  /**
   * Сервис кэша метаданных.
   */
  @Inject
  var domainCacheService: DomainCacheService = null

  @Inject
  var commonCacheService:CommonCacheService = null

  @Inject
  var metaFormProvider:MetaFormProvider = null

  /**
   * Сервис получения уведомлений
   */
  @Inject
  var pushNotificationService:PushNotificationService = null

  @Inject
  var execStatProvider:ExecStatProvider = null

  @Inject
  var entityFactory:EntityFactory = null


  @Inject
  var validationWarningPool:ValidationWarningPool = null
  /**
   * Создает новую запись указанного класса
   * @param classId Идентификатор класса
   * @return Идентификатор нового объекта
   */
  @POST
  @Path("/add/{classId}")
  @Produces(Array(MediaType.APPLICATION_XML))
  def addRecord(@PathParam("classId") classId: String, inputXml: String): Response = {

    val data = {
      val newGuid = DBProfiler.profile("total",execStatProvider,true){
        val newInst = dataWriterService.addRecord(classId)
        try{
          if(inputXml!=null && inputXml.length>0){
            val fields = new Model2Xml().parseEditReq(inputXml)
            dataWriterService.editRecord(newInst,fields)
          }
        }
        catch{
          case _ =>}

        newInst.getId
      }

      if (newGuid != null)  {
        //если были предупреждения на тригеры после создания записи то добавить результат валидации
        val warnings = validationWarningPool.getWarnings.toSeq
        val validationStr = if (warnings.isEmpty) ""
        else "validation>" + new Model2Xml().validationRulesToXml(warnings) +"/validation"

        "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
          "<result><id>" + newGuid + "</id>"+execStatProvider.toXml + "</result>" // + validationStr
      }
      else
        new Model2Xml().standardAnswerToXml(-1, "Новая запись не создана.")
    }

    Response.status(Response.Status.OK).entity(data).build()
  }

  /**
   * Создает запись определенного класса, связанную с указанной записью
   * @param classId Идентификатор класса
   * @param viewCardId Идентификатор формы-карточки
   * @param cardRecordId Идентификатор записи
   * @param windowGridId Идентификатор формы-списка
   * @return Идентификатор нового объекта
   */
  @POST
  @Path("/add-rel/{classId}/{viewCardId}/{cardRecordId}/{windowGridId}")
  @Produces(Array(MediaType.APPLICATION_XML))
  def addRelRecord(
    @PathParam("classId") classId: String,
    @PathParam("viewCardId") viewCardId: String,
    @PathParam("cardRecordId") cardRecordId: String,
    @PathParam("windowGridId") windowGridId: String): Response = {
    val newGuid =DBProfiler.profile("total",execStatProvider,true)    {
      dataWriterService.addRelRecord(classId, viewCardId, cardRecordId, windowGridId).getId
    }

    //если были предупреждения на тригеры после создания записи то добавить результат валидации
    val warnings = validationWarningPool.getWarnings.toSeq
    val validationStr = if (warnings.isEmpty) ""
                       else "validation>" + new Model2Xml().validationRulesToXml(warnings) +"/validation"
    Response.status(Response.Status.OK)
      .entity("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +"<result><id>" + newGuid + "</id>"+execStatProvider.toXml + "</result>")// + validationStr)
      .build()
  }

  @POST
  @Path("/addRelation/{relationId}/{fromObjectId}")
  @Produces(Array(MediaType.APPLICATION_XML))
  def addRelation(
                    @PathParam("relationId") relationId: String,
                    @PathParam("fromObjectId") fromObjectId: String): Response = {
    val objectId =DBProfiler.profile("total",execStatProvider,true)    {
      dataWriterService.addRelation(relationId,fromObjectId)
    }
    Response.status(Response.Status.OK)
      .entity("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +"<result><relation objectId=\"" + objectId + "\"/>"+execStatProvider.toXml + "</result>")// + validationStr)
      .build()
  }

  @POST
  @Path("/editRelation/{relationId}/{objectId}")
  @Consumes(Array(MediaType.APPLICATION_XML))
  @Produces(Array(MediaType.APPLICATION_XML))
  def editRelation(@PathParam("relationId") relationId: String,@PathParam("objectId") objectId: String, inputXml: String): Response = {
      DBProfiler.profile("total",execStatProvider,true){
        val xml = XML.loadString(inputXml)
        val fromObjectId= (xml \\ "from")(0).text
        val toObjectId= (xml \\ "to")(0).text
        dataWriterService.editRelation(relationId,objectId,fromObjectId,toObjectId)
      }

      Response.status(Response.Status.OK)
        .entity(new Model2Xml().standardAnswerToXml(0, null, validationWarningPool.getWarnings.toSeq,execStatProvider))
        .build()

  }

  /**
   * Редактирует координаты объектов для диаграммы в привязке к параметрам фильтрации в дереве фильтре
   * @param objectId
   * @param inputXml
   * @return
   */
  @POST
  @Path("/editPosition/{objectId}")
  @Consumes(Array(MediaType.APPLICATION_XML))
  @Produces(Array(MediaType.APPLICATION_XML))
  def editPosition(@PathParam("objectId") objectId: String, inputXml: String): Response = {
    DBProfiler.profile("total",execStatProvider,true){
      val xml = XML.loadString(inputXml)
      def extractParameter(name:String):String={
        val nodes =  xml \\ name
        if(nodes.length>0)nodes(0).text else null
      }
      val params= Option((xml \\ "treeVar")).map(f => f.toString.replace("treeVar","variables").replace("/treeVar","/variables")).orNull
      val nodeId= extractParameter("nodeId")
      val windowGridId= extractParameter("windowGridId")
      val coordinates = Seq("x","y","width","height").map(f => f-> extractParameter(f) ).filter(f => f._2 != null).toMap
      dataWriterService.editPosition(nodeId,params,objectId,windowGridId,coordinates)
    }

    Response.status(Response.Status.OK)
      .entity(new Model2Xml().standardAnswerToXml(0, null, validationWarningPool.getWarnings.toSeq,execStatProvider))
      .build()

  }
  @POST
  @Path("/getRelationObjects/{relationId}/{objectId}")
  @Consumes(Array(MediaType.APPLICATION_XML))
  @Produces(Array(MediaType.APPLICATION_XML))
  def getRelationObjects( @PathParam("relationId") relationId: String,
                          @PathParam("objectId") objectId: String,
                          @QueryParam("fromObject-id") fromObject:String,
                          @QueryParam("toObject-id") toObject:String): Response = {


    val metaRelation = metaFormProvider.getMetaRelation(relationId)
     val refParam = if(fromObject != null) metaRelation.endReferenceParams
    else metaRelation.startReferenceParams
    val viewFieldId =if(fromObject !=null ) metaRelation.endViewFieldID else metaRelation.startViewFieldId
    val refXml = if(refParam != null) {
      "<variables>" + entityFactory.createEntity(objectId).ClientXML(refParam) + "</variables>"
    }  else null
    getDictionary(viewFieldId,refXml)
  }
  @POST
  @Path("/deleteRelation/{relationId}/{objectId}")
  @Consumes(Array(MediaType.APPLICATION_XML))
  @Produces(Array(MediaType.APPLICATION_XML))
  def getRelationObjects(@PathParam("relationId") relationId: String,
                         @PathParam("objectId") objectId: String): Response = {


    dataWriterService.deleteRelation(relationId,objectId)
    Response.status(Response.Status.OK)
      .entity(new Model2Xml().standardAnswerToXml(0, "Запись успешно удалена."))
      .build()
  }


  /**
   * Изменяет определенное поле указанной записи, В процессе сохранения выполняется проверка
   * входных данных валидаторами, вызываются триггеры, связанные с классом записи,
   * при необходимости меняется статут сохраняемого объекта.
   * @param recordId Идентификатор редактируемой записи
   * @param inputXml указание названия сохраняемого поля и его новое значение в виде XML.
   * @return Сообщение об успешном сохранении данных,
   *         список сообщений с предупреждениями, выявленными на этапе валидации.
   *         В случае ошибки ответ состоит из списка выявленных ошибок в данных.
   */
  @POST
  @Path("/edit/{recordId}")
  @Consumes(Array(MediaType.APPLICATION_XML))
  @Produces(Array(MediaType.APPLICATION_XML))
  def editRecord(@PathParam("recordId") recordId: String, inputXml: String): Response = {
    val (isValid, validationResults) =
      DBProfiler.profile("total",execStatProvider,true){
          val fields = new Model2Xml().parseEditReq(inputXml)
          dataWriterService.editRecord(recordId, fields)
      }
    if (isValid) {
      Response.status(Response.Status.OK)
        .entity(new Model2Xml().standardAnswerToXml(0, null, validationWarningPool.getWarnings.toSeq,execStatProvider))
        .build()
    }
    else
      Response.status(Response.Status.BAD_REQUEST)
        .entity(new Model2Xml().validationErrorsToXml(validationWarningPool.getWarnings.toSeq))
        .build()
  }

  /**
   * Выполняет удаление указанной записи.
   * @param recordId Идентификатор записи.
   * @return Сообщение об успешно удаленной записи.
   */
  @POST
  @Path("/delete/{recordId}")
  @Produces(Array(MediaType.APPLICATION_XML))
  def deleteRecord(
    @PathParam("recordId") recordId: String): Response = {

    dataWriterService.deleteRecord(recordId)

    Response.status(Response.Status.OK)
      .entity(new Model2Xml().standardAnswerToXml(0, "Запись успешно удалена."))
      .build()
  }

  private def selectDictionaryData(viewFieldId: String, objectId: String, variablesXml: String): String = {
    val vars = if (this.clean(variablesXml) != null)
      new Model2Xml().clearXmlFromHeader(variablesXml)
    else null

    val data = dataReaderService.getDictionaryData(viewFieldId, vars, objectId)

    def toXml(id: String, name: String) =
      <object><id>{id}</id> <name>{name}</name></object>

    val result =
      <result>
        {data.keys.map(id => toXml(id, Option(data(id)).map(_.toString).getOrElse(id)))}
      </result>

    "<?xml version='1.0' encoding='utf-8'?>\n" + result.toString()
  }

  /**
   * Получает содержимое указанного справочника
   * @param viewFieldId Идентификатор поля
   * @param variablesXml Фильтр на данные справочника, формат специфицирован в описании БД.
   * @param objectId Идентификатор объекта
   * @return Возвращает простой упорядоченный словарь справочных данных,
   *         представленных как короткие списки без ссылок или вложенных значений.
   */
  @POST
  @Path("/dictionary/{viewFieldId}/{objectId}")
  @Consumes(Array(MediaType.APPLICATION_XML))
  @Produces(Array(MediaType.APPLICATION_XML))
  def getDictionaryByObject(@PathParam("viewFieldId") viewFieldId: String,
                            @PathParam("objectId") objectId: String,
                            variablesXml: String): Response = {

    //TODO: в дальнейшем необходимо кэширование
    val content = selectDictionaryData(viewFieldId, objectId, variablesXml)

    Response.ok().entity(content).build()
  }

  /**
   * Получает содержимое указанного справочника
   * @param viewFieldId Идентификатор поля
   * @param variablesXml Фильтр на данные справочника, формат специфицирован в описании БД.
   * @return Возвращает простой упорядоченный словарь справочных данных,
   *         представленных как короткие списки без ссылок или вложенных значений.
   */
  @POST
  @Path("/dictionary/{viewFieldId}")
  @Consumes(Array(MediaType.APPLICATION_XML))
  @Produces(Array(MediaType.APPLICATION_XML))
  def getDictionary(@PathParam("viewFieldId") viewFieldId: String,
                    variablesXml: String): Response = {

    //TODO: в дальнейшем необходимо кэширование
    val content = selectDictionaryData(viewFieldId, null, variablesXml)

    Response.ok().entity(content).build()
  }

  /**
   * Получает дерево-фильтр
   * @param treeId Идентификатор дерева
   * @return Ответ сервера с описанием дерева-фильтра
   */
  @GET
  @Path("/tree-filter/{treeId}")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getTreeFilterGet(@PathParam("treeId") treeId: String): Response = {
      getTreeFilter(treeId)
  }

  @POST
  @Path("/tree-filter/{treeId}")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getTreeFilterPost(@PathParam("treeId") treeId: String): Response = {
     getTreeFilter(treeId)
  }

  def getTreeFilter(treeId: String): Response = {
    val treeFilter = dataReaderService.getTreeFilter(treeId)

    // сконвертировать в xml для передачи клиенту
    val data =
      new Model2Xml().treeFilterToXml(treeFilter)

    Response.ok().entity(data).build()
  }

  private def clean(v: String) = if (v == "") null else v

  /**
   * Получает данные формы-списка
   * @param gridId Идентификатор списка
   * @param nodeId
   * @param nodeData
   * @param refId
   * @param recordId
   * @param viewCardId
   * @param fieldId
   * @param variablesXml Содержимое фильтра
   * @return Ответ сервера с содержимым множества записей из списка
   */
  @POST
  @Path("/grid/{gridId}")
  @Consumes(Array(MediaType.APPLICATION_XML))
  @Produces(Array(MediaType.APPLICATION_XML))
  def getGridData(
    @PathParam("gridId") gridId: String,
    @QueryParam("node-id") nodeId: String,
    @QueryParam("node-data") nodeData: String,
    @QueryParam("ref-id") refId: String,
    @QueryParam("record-id") recordId: String,
    @QueryParam("card-id") viewCardId: String,
    @QueryParam("field-id") fieldId: String,
    variablesXml: String): Response = {
    val (vars, treeVars) = if (this.clean(variablesXml) != null)
      new Model2Xml().parseTreeVars(variablesXml)
    else (null, null)
    val dataView = DBProfiler.profile("total",execStatProvider,true){
        dataReaderService.getGridDataView(gridId, clean(nodeId), clean(refId), clean(nodeData),
          clean(recordId), clean(viewCardId),
          clean(fieldId), vars, treeVars)
   }
    val data_str = new Model2Xml().convert(dataView,execStatProvider = execStatProvider)

    val content = "<?xml version='1.0' encoding='utf-8'?>\n" + data_str
    Response.ok().entity(content).build()
  }

  @POST
  @Path("/diagram/{gridId}")
  @Consumes(Array(MediaType.APPLICATION_XML))
  @Produces(Array(MediaType.APPLICATION_XML))
  def getDiagramData(
                   @PathParam("gridId") gridId: String,
                   @QueryParam("node-id") nodeId: String,
                   variablesXml: String): Response = {
    val (vars, treeVars) = if (this.clean(variablesXml) != null)
      new Model2Xml().parseTreeVars(variablesXml)
    else (null, null)
    val dataView = DBProfiler.profile("total",execStatProvider,true){
      dataReaderService.getGridDataView(gridId, clean(nodeId), null, null,
        null,null,null, vars, treeVars,true)
    }
    val data_str = new Model2Xml().convert(dataView,execStatProvider = execStatProvider)

    val content = "<?xml version='1.0' encoding='utf-8'?>\n" + data_str
    Response.ok().entity(content).build()
  }

  /**
   * Получает данные формы-карточки для указанной записи
   * @param recordId Идентификатор записи
   * @return Ответ сервера с данными для отображения на форме-карточке
   */
  @GET
  @Path("/card/{recordId}")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getCardDataGet(@PathParam("recordId") recordId: String,
                     @QueryParam("viewCard-id") viewCardId: String): Response = {
    getCardData(recordId,viewCardId)
  }

  @POST
  @Path("/card/{recordId}")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getCardDataPost(@PathParam("recordId") recordId: String,
                      @QueryParam("viewCard-id") viewCardId: String): Response = {
     getCardData(recordId,clean(viewCardId))
  }

  def  getCardData(recordId: String, viewCardId: String): Response = {
    val (dataView, refGridSettings,sectionsSettings) = DBProfiler.profile("total",execStatProvider,true){ dataReaderService.getCardDataView(recordId,viewCardId)     }

    //FIXME: в предыдущем шаге необходимо получать DataViewTable
    val data_str = new Model2Xml().convert(dataView, refGridSettings = refGridSettings,execStatProvider = execStatProvider,sectionSettings = sectionsSettings)
    val content = "<?xml version='1.0' encoding='utf-8'?>\n" + data_str

    Response.ok().entity(content).build()
  }


  /**
   * Сбрасывает кэш метаинформации.
   * @param domain Номер домена. Если номер домена опущен,
   *               кэш сбрасыавается для всех доменов.
   * @return Ответ сервера об успешно выполненной операции.
   */
  @GET
  @Path("/drop-cache")
  @Produces(Array(MediaType.APPLICATION_XML))
  def dropAllCaches(@QueryParam("domain") domain: String): Response = {

    // получаем список доменов
    val targetDomains =  dataReaderService.getDomains()
    metaClassProvider.dropCache(targetDomains)

    val content = "<?xml version='1.0' encoding='utf-8'?>\n" +
    "<?xml-stylesheet type=\"text/css\" href=\"cache.css\"?>" +
      "<result>Кеш сброшен</result>"

    Response.ok().entity(content).build()
  }

  @POST
  @Path("/unlock/{recordId}")
  @Consumes(Array(MediaType.APPLICATION_XML))
  @Produces(Array(MediaType.APPLICATION_XML))
  def unlockObject(@PathParam("recordId") recordId: String): Response = {
    dataWriterService.unlockObject(entityFactory.createEntity(recordId))

    Response.status(Response.Status.OK)
      .entity(new Model2Xml().standardAnswerToXml(0, null, null))
      .build()
  }

  @GET
  @Path("/notification")
  @Consumes(Array(MediaType.APPLICATION_XML))
  @Produces(Array(MediaType.APPLICATION_XML))
  def getNotifications:Response = {

    val (notifications,count) = pushNotificationService.getUserNotifications
    val xml = <result count={count.toString}>
      {notifications.map(n=>n.xml)}
      </result>
    val content = "<?xml version='1.0' encoding='utf-8'?>\n" + xml.toString()
      Response.ok().entity(content).build()
  }
                                                                                                                                                                 1
  @GET
  @Path("card-object/{recordId}/{objectInCardItemId}")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getCardObject(@PathParam("recordId") recordId: String,@PathParam("objectInCardItemId") objectInCardItemId: String):Response = {
    val objectID =  metaFormProvider.getObjectInCard(recordId,objectInCardItemId)
    val result =
      <result>
        { if(objectID!=null) <objectId>{objectID}</objectId> }
    </result>
    val content = "<?xml version='1.0' encoding='utf-8'?>\n" + result.toString
    Response.ok().entity(content).build()
  }


  @POST
  @Path("/isf/{recordId}/{propertyCode}")
  @Consumes(Array(MediaType.APPLICATION_XML))
  @Produces(Array(MediaType.APPLICATION_XML))
  def isfData(@PathParam("recordId") recordId: String,
                   @PathParam("propertyCode") propertyCode: String): Response = {

    val dataView = DBProfiler.profile("total",execStatProvider,true){
        dataReaderService.getISFDataView(recordId,propertyCode)
    }
    val data_str = new Model2Xml().convert(dataView,execStatProvider = execStatProvider)
    val content = "<?xml version='1.0' encoding='utf-8'?>\n" + data_str
    Response.ok().entity(content).build()
  }
}