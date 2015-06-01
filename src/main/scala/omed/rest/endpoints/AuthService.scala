/**
 *
 */
package omed.rest.endpoints

import javax.ws.rs._
import javax.ws.rs.core.{Context, MediaType, Response}
import javax.servlet.http.HttpServletRequest
import omed.auth._
import com.google.inject.Inject
import omed.system.ContextProvider
import xml.XML
import java.io.StringWriter
import java.util.TimeZone
import omed.auth.Account
import omed.auth.Account
import omed.rest.model2xml.Model2Xml
import omed.model.services.ExpressionEvaluator
import omed.bf.ConfigurationProvider

/**
 * Сервис аутентификации и авторизации.
 * Содержит основные методы, вызовы которых управляют созданием и удалением сессии.
 */
@Path("/auth")
class AuthService {

  /**
   * Провайдер аутентификации, содержащий основные методы по управлению сессиями на уровне хранилища данных
   * Представляет собой объект, реализующий интерфейс [[omed.auth.Auth]]
   */
  @Inject
  var authProvider: Auth = null

  /**
   * Объектное представление HTTP запроса, формируемое на уровне Servlet API.
   */
  @Context
  var httpRequest: HttpServletRequest = null

  /**
   * Провайдер контекста, предоставляющий доступ к текущему контексту, объекту класса [[omed.system.Context]]
   */
  @Inject
  var contextProvider: ContextProvider = null

  @Inject
  var permissionReader: PermissionReader = null

  @Inject
  var expressionEvaluator:ExpressionEvaluator = null

  @Inject
  var configProvider:ConfigurationProvider = null
  /**
   * Регистрирует сессию в указанном домене системы. Вход в систему будет успешным, если
   * в системе есть зарегистрированный пользователь и автор изменений, и указанный пароль
   * совпадает с паролем пользователя. Автор изменений может быть не указан
   * @param domain Номер домена в терминах системы ОМед, в котором выполняется аутентификация
   * @param username Имя пользователя
   * @param authorname Имя пользователя, от лица которого выполняется работа в системе
   * @param password Пароль пользователя
   * @param userAgent Идентификатор клиентского приложения
   * @return Ответ сервиса с указанием реальных имен пользователя и автора изменений,
   *         признаком "суперпользователь" и идентификатором учреждения, в котором ведется работа
   */
  @POST
  @Path("/login")
  @Consumes(Array(MediaType.APPLICATION_FORM_URLENCODED))
  @Produces(Array(MediaType.APPLICATION_XML))
  def doLoginByPassword(
    @FormParam("domain") domain: Int,
    @FormParam("username") username: String,
    @FormParam("author") authorname: String,
    @FormParam("password") password: String,
    @HeaderParam("User-Agent") userAgent: String): Response = {

    def authFunction(domain: Int, username: String, password: String) =
      authProvider.authenticateByPassword(domain, username, authorname, password)

    doLogin(domain, username, password, userAgent, authFunction)
  }

  /**
   * Регистрирует сессию в указанном домене системы. Вход в систему будет успешным, если
   * в системе есть зарегистрированный пользователь и автор изменений, и указанный документ
   * совпадает с документом пользователя.
   * @param domain Номер домена в терминах системы ОМед, в котором выполняется аутентификация
   * @param username Имя пользователя
   * @param number Идентификатор документа пользователя
   * @param userAgent Идентификатор клиентского приложения
   * @return Ответ сервиса с указанием реальных имен пользователя и автора изменений,
   *         признаком "суперпользователь" и идентификатором учреждения, в котором ведется работа
   */
  @POST
  @Path("/login2")
  @Consumes(Array(MediaType.APPLICATION_FORM_URLENCODED))
  @Produces(Array(MediaType.APPLICATION_XML))
  def doLoginByDocument(
                         @FormParam("domain") domain: Int,
                         @FormParam("username") username: String,
                         @FormParam("number") number: String,
                         @FormParam("doctype") doctype: String,
                         @HeaderParam("User-Agent") userAgent: String): Response = {
    def authFunction(domain: Int, username: String, password: String) = {
      val docTypeVal = try {
        PatientDocType.withName(doctype)
      } catch {
        case _ => throw new RuntimeException(String.format("Неизвестный doctype %s", doctype))
      }
      authProvider.authenticateByDocument(domain, username, password, docTypeVal)
    }

    doLogin(domain, username, number, userAgent, authFunction)
  }

  /**
   * Регистрирует сессию в указанном домене системы. Вход в систему будет успешным, если
   * в системе есть зарегистрированный пользователь и аутентификация выполняется успешно.
   * @param domain Номер домена в терминах системы ОМед, в котором выполняется аутентификация
   * @param username Имя пользователя
   * @param password Пароль пользователя
   * @param userAgent Идентификатор клиентского приложения
   * @param authFunction Функция аутентификации
   * @return Ответ сервиса с указанием реальных имен пользователя и автора изменений,
   *         признаком "суперпользователь" и идентификатором учреждения, в котором ведется работа
   */
  def doLogin(domain: Int, username: String, password: String, userAgent: String,
              authFunction: (Int, String, String) => (Account, Account)) = {
    //restUtil.logAccess()

    // provide authentication and start session throw ejb backend
    val userIp = httpRequest.getRemoteAddr()
    val session = httpRequest.getSession()
    val existingSessionId = session.getAttribute("sessionId").asInstanceOf[String]
    //TODO  get from _Domain


    val timeZone =authProvider.getTimeZone(domain)// authProvider.gettimezoneFromDomain
    val (realUserName, realAuthorName, isSuperUser,roles) = try {

      val (user, author) = authFunction(domain, username, password)
      if (user != null) {
        val sessionId = authProvider.login(
          existingSessionId, domain,
          user.id, author.id,
          userAgent, userIp)

        if (sessionId == null)
          throw new LoginError

        session.setAttribute("domainId", domain)
        session.setAttribute("hcuId", user.hcuId)
        session.setAttribute("userId", user.id)
        session.setAttribute("authorId", author.id)
        session.setAttribute("isSuperUser", author.isSuperUser)
        session.setAttribute("sessionId", sessionId)
        session.setAttribute("timeZone", TimeZone.getTimeZone(timeZone))
        val roles = permissionReader.getUserRoles(user.id)
        if(roles.length==1) session.setAttribute("roleId",roles(0))
        (user.name, author.name, author.isSuperUser,roles)
      } else {
        if (existingSessionId == null) {
          authProvider.trace(domain, username, userAgent, userIp)
        }
        throw new LoginError
      }

    } catch {
      case e@_ =>
        if (session.isNew) {
          session.invalidate()
        }
        throw e
    }
    val allRoles = permissionReader.getAllRoles
    val userRoles = roles.map(f=> allRoles.find(p => p.id == f).get)
    // TODO: подставить ЛПУ
    val config = configProvider.create()
    val xml =
      <auth>
        <username>
          {realUserName}
        </username>
        <author>
          {realAuthorName}
        </author>
        <superUser>
          {isSuperUser}
        </superUser>
        <hcuName>Тестовое ЛПУ</hcuName>
        <timeZone>{timeZone}</timeZone>
        <roleList>
          {
            userRoles.map(f=> <role>
              <id>{f.id}</id>
              <name>{f.name}</name>
              {if(f.openObjExp!=null) <objectId>{expressionEvaluator.evaluate(f.openObjExp,config,contextProvider.getContext.getSystemVariables).getId}</objectId> else null}
            </role>)
          }
        </roleList>
      </auth>

    val sw = new StringWriter()
    XML.write(sw, scala.xml.Utility.trim(xml), "UTF-8", true, null)
    val answer = sw.toString

    Response.ok(answer).build()
  }

  /**
   * Завершает текущую сессию в текущем домене. На момент вызова должна
   * обязательно существовать текущая активная сессия. В противном случае
   * вызов метода будет неуспешным.
   * @return Ответ сервиса, в случае успешного завершения сессии содержит
   *         XML-представление сообщения со строкой "ok"
   */
  @POST
  @Path("/logout")
  @Produces(Array(MediaType.APPLICATION_XML))
  def doLogout(): Response = {

    //restUtil.logAccess()

    //получить идентификатор сессии
    val session = httpRequest.getSession(false)
    val sessionId = httpRequest.getSession(false)
      .getAttribute("sessionId").asInstanceOf[String]
    session.invalidate()
    authProvider.logout(sessionId)

    // form answer in suitable format
    val xml =
      <auth>
        <message>ok</message>
      </auth>
    val sw = new StringWriter()
    XML.write(sw, scala.xml.Utility.trim(xml), "UTF-8", true, null)
    val answer = sw.toString

    Response.ok().entity(answer).build()
  }
  @POST
  @Path("/getCookie")
  @Produces(Array(MediaType.APPLICATION_FORM_URLENCODED))
  def getCookie(): Response = {
    Response.ok().entity(httpRequest.getSession(false).getId).build()
  }

  @POST
  @Path("/change-role/{roleId}")
  @Produces(Array(MediaType.APPLICATION_XML))
  def changeRole(@PathParam("roleId") roleId :String) :Response={
    //получить идентификатор сессии
    val session = httpRequest.getSession(false)
    session.setAttribute("roleId",roleId)

    Response.status(Response.Status.OK)
      .entity(new Model2Xml().standardAnswerToXml(0, "OK"))
      .build()
  }
}
