/**
 *
 */
package omed.rest.endpoints

import javax.ws.rs._
import javax.ws.rs.core.{ Context, MediaType, Response }
import javax.servlet.http.HttpServletRequest

import com.google.inject.Inject

import omed.rest.model2xml.Model2Xml
import omed.forms.{MetaFormDescription, MetaFormProvider}
import omed.model.MetaClassProvider
import omed.system.ContextProvider
import omed.cache.DomainCacheService
import java.io.StringWriter
import scala.xml.XML
import omed.data.SettingsService
import scala.xml.{Text, TopScope, Elem}

/**
 * Сервис получения метаданных
 */
@Path("/meta")
@Produces(Array(MediaType.APPLICATION_XML))
class MetaService {

  /**
   * Объектное представление HTTP запроса, формируемое на уровне Servlet API.
   */
  @Context
  var httpRequest: HttpServletRequest = null

  /**
   * Провайдер метаданных о формах
   */
  @Inject
  var metaFormProvider: MetaFormProvider = null

  /**
   * Провайдер метаданных о классах
   */
  @Inject
  var metaClassProvider: MetaClassProvider = null

  /**
   * Провайдер контекста, предоставляющий доступ к текущему контексту, объекту класса [[omed.system.Context]]
   */
  @Inject
  var contextProvider: ContextProvider = null

  /**
   * Сервис кэша метаданных
   */
  @Inject
  var domainCacheService: DomainCacheService = null

  @Inject
  var settingService:SettingsService = null


  /**
   * Получает метаданные о главном меню
   * @return Ответ сервера с описанием главного меню
   */
  @GET
  @Path("/main-menu")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getMainMenuGet(): Response = {
     getMainMenu()
  }

  @POST
  @Path("/main-menu")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getMainMenuPost(): Response = {
    getMainMenu()
  }

  def getMainMenu(): Response = {
    var menu = metaFormProvider.getMainMenu()

    Response.status(Response.Status.OK)
      .entity(new Model2Xml().mainMenuToXml(menu))
      .build()
  }
  /**
   * Получает метаданные о главной форме-списке
   * @return Ответ сервиса с описанием главной формы-списка
   *         с описанием состава полей и их свойствами
   */
  @GET
  @Path("/main-grid")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getMainGridGet(): Response = {
      getMainGrid()
  }

  @POST
  @Path("/main-grid")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getMainGridPost(): Response = {
     getMainGrid()
  }

  def getMainGrid():Response={
    val metaGrid = metaFormProvider.getMainGrid()
    Response.status(Response.Status.OK)
      .entity(new Model2Xml().metaGridToXml(metaGrid))
      .build()
  }

  @GET
  @Path("/main-card")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getMainCard():Response={
     val metacard = metaFormProvider.getMainCard()
    if(metacard==null)    Response.status(Response.Status.OK)
      .entity(new Model2Xml().standardAnswerToXml(0,"false")).build()
     else  Response.status(Response.Status.OK)
      .entity(new Model2Xml().metaCardToXml(metacard))
      .build()
  }
  /**
   * Получает метаданные для указанного меню
   * @param menuId Идентификатор меню
   * @return Ответ сервера с описанием меню, составом пунктов и их свойствами
   */
  @GET
  @Path("/menu/{menuId}")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getMenu(@PathParam("menuId") menuId: String): Response = {
    val cacheKey = "menu/" + menuId

    val cachedData = domainCacheService.get(classOf[MetaFormDescription], cacheKey)
    val content = if (cachedData != null) {
      cachedData.content
    } else {
      val menu = metaFormProvider.getMenu(menuId)

      // сконвертировать в xml для передачи клиенту
      val data =
        new Model2Xml().contextMenuToXml(menu)

      domainCacheService.put(classOf[MetaFormDescription], cacheKey, MetaFormDescription(data))
      data
    }
    Response.status(Response.Status.OK).entity(content).build()
  }

  /**
   * Получает метаописание указанной формы-списка
   * @param viewGridId Идентификатор формы-списка
   * @return Ответ сервиса с описание формы-списка, составом полей и их свойствами
   */
  @GET
  @Path("/grid/{viewGridId}")
  def getGridGet(@PathParam("viewGridId") viewGridId: String ): Response = {
     getGrid(viewGridId)
  }

  @POST
  @Path("/grid/{viewGridId}")
  def getGridPost(@PathParam("viewGridId") viewGridId: String): Response = {
    getGrid(viewGridId)
  }

  def getGrid( viewGridId: String): Response = {
    val metaGrid = metaFormProvider.getMetaFormGrid(viewGridId)

    Response.status(Response.Status.OK)
      .entity(new Model2Xml().metaGridToXml(metaGrid))
      .build()
  }


  /**
   * Получает метаописание указанной формы-карточки
   * @param recordId Идентификатор формы-карточки
   * @return Ответ сервиса с описанием формы-карточки и свойств её полей
   */
  @GET
  @Path("/card/{recordId}")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getCardGet(@PathParam("recordId") recordId: String,
                 @QueryParam("viewCard-id") viewCardId: String): Response = {
      getCard(recordId,viewCardId)
  }
  @POST
  @Path("/card/{recordId}")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getCardPost(@PathParam("recordId") recordId: String,
                  @QueryParam("viewCard-id") viewCardId: String): Response = {
    getCard(recordId,viewCardId)
  }

  def getCard(recordId: String,viewCardId:String): Response = {
    val metaCard = metaFormProvider.getMetaCard(recordId,viewCardId)

    Response.status(Response.Status.OK)
      .entity(new Model2Xml().metaCardToXml(metaCard))
      .build()
  }


  /**
   * Получает описание указанного класса
   * @param classId Идентификатор класса.
   *                В том случае, если идентификатор опущен,
   *                результатом является описание всех классов
   * @return Описание одного или нескольких классов с атрибутным составом
   */
  @GET
  @Path("/meta-classes/{classId}")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getMetaClasses(@PathParam("classId") classId: String): Response = {
    val metaClasses = if (classId != null)
      List(metaClassProvider.getClassMetadata(classId))
    else
      metaClassProvider.getAllClassesMetadata()

    Response.status(Response.Status.OK)
      .entity(new Model2Xml()
        .metaClassesToXml(metaClasses))
      .build()
  }

  /**
   * Получает описания всех классов, эквивалентно вызову
   * метода getMetaClasses без указанного параметра classId
   * @return Описание всех классов
   */
  @GET
  @Path("/meta-classes")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getMetaClasses(): Response = {
    this.getMetaClasses(null)
  }

  /**
   * Получает описание статусного меню для указанной записи
   * @param recordId Идентификатор записи
   * @return Ответ сервиса с описанием статусного меню
   */

  @GET
  @Path("/status-menu/{recordId}")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getStatusMenuGet(@PathParam("recordId") recordId: String): Response = {
       getStatusMenu(recordId)
  }

  @POST
  @Path("/status-menu/{recordId}")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getStatusMenuPost(@PathParam("recordId") recordId: String): Response = {
    getStatusMenu(recordId)
  }

  def getStatusMenu(recordId: String): Response = {
    val statusMenuList = metaClassProvider.getStatusMenu(recordId)

    // сконвертировать в xml для передачи клиенту
    val data =
    <menu>{
        statusMenuList.map(m=>
         <menuItem><name>{m.name}</name>
           <businessFunctionId>{m.businessFunctionId}</businessFunctionId>
           {if(m.alignment!=null) <alignment>{m.alignment}</alignment> }
           {if(m.buttonPosition!=null) <buttonPosition>{m.buttonPosition}</buttonPosition> }
           {if(m.sectionId!=null) <sectionId>{m.sectionId}</sectionId> }
           {if(m.buttonGroupId!=null)  <buttonGroupID>{m.buttonGroupId}</buttonGroupID>}
            <row>{m.row}</row>
            <sortOrder>{m.sortOrder}</sortOrder>
         </menuItem>)
    }
    </menu>
    val sb = new StringWriter()
    XML.write(sb, scala.xml.Utility.trim(data), "UTF-8", true, null)
    Response.status(Response.Status.OK).entity(sb.toString).build()
  }

  @GET
  @Path("/client-settings")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getClientSettings():Response = {
     val settings = settingService.getUserSettings(contextProvider.getContext.userId)
     val xml = <result>
       <clientSettings>
       {
          settings.map(f=> <param><code>{f._1}</code><value>{f._2}</value></param>)
       }
       </clientSettings>
    </result>
    val sb = new StringWriter()
    XML.write(sb, scala.xml.Utility.trim(xml), "UTF-8", true, null)
    Response.status(Response.Status.OK).entity(sb.toString).build()
  }

  @GET
  @Path("/client-theme/{clientThemeId}")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getClientTheme(@PathParam("clientThemeId") clientThemeId: String):Response = {
    val settings = settingService.getUserSettings(contextProvider.getContext.userId)
    val xml = <result>
      <clientSettings>
        {
        settings.map(f=> <param><code>{f._1}</code><value>{f._2}</value></param>)
        }
      </clientSettings>
    </result>
    val sb = new StringWriter()
    XML.write(sb, scala.xml.Utility.trim(xml), "UTF-8", true, null)
    Response.status(Response.Status.OK).entity(sb.toString).build()
  }
}
