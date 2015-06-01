package omed.rest.endpoints

import javax.ws.rs._
import javax.ws.rs.core.{Context, MediaType, Response}
import javax.servlet.http.HttpServletRequest
import com.google.inject.Inject

import omed.rest.model2xml.Model2Xml
import omed.system.ContextProvider
import omed.bf.{ProcessStateProvider, ValidationWarningPool, BusinessFunctionExecutor}
import java.io.StringWriter
import xml.{Unparsed, Elem, XML}
import net.iharder.Base64
import omed.model.{SimpleValue, Value, EntityInstance}
import omed.cache.ExecStatProvider
import omed.db.DBProfiler
import omed.auth.{PermissionType, PermissionProvider}
import omed.errors.DataAccessError

/**
 * Сервис выполнения бизнес-функций.
 * Посредством вызовов методов данного сервиса клиентское приложение
 * инициирует выполнение бизнес-функций, получает результаты выполнения шагов
 * и посылает сервису результаты шагов, исполняемых на клиенте.
 */
@Path("/bf")
@Produces(Array(MediaType.APPLICATION_XML))
class BfService {

  /**
   * Служба выполнения бизнес-функций инкапсулирует алгоритмы работы бизнес-функций,
   * обращения к хранилищу данных и метаданных, управляет транзакциями и т.д.
   */
  @Inject
  var businessFunctionExecutor: BusinessFunctionExecutor = null

  /**
   * Провайдер контекста предоставляет доступ к контексту,
   * объекту класса [[omed.system.Context]]
   */
  @Inject
  var contextProvider: ContextProvider = null

  @Inject
  var processStateProvider: ProcessStateProvider = null

  @Inject
  var execStatProvider:ExecStatProvider = null

  @Inject
  var permissionProvider: PermissionProvider = null
  
  @Inject
  var validationWarningPool:ValidationWarningPool = null
  /**
   * Объектное представление HTTP-запроса, предоставляемое Java Servlet API
   */
  @Context
  var httpRequest: HttpServletRequest = null

  /**
   * Инициализация бизнес-функции
   * @param businessFunctionId Идентификатор бизнес-функции
   * @return Идентификатор процесса, соответствующего выполняемой бизнес-функции
   */

  @GET
  @Path("/init/{businessFunctionId}")
  @Produces(Array(MediaType.APPLICATION_XML))
  def initInstanceGet(@PathParam("businessFunctionId") businessFunctionId: String,
                   @QueryParam("object-id") objectId: String,
                   @QueryParam("notificationReceiver-id") notificationReceiverId: String): Response = {
    initInstance(businessFunctionId,objectId,notificationReceiverId)
  }
  @POST
  @Path("/init/{businessFunctionId}")
  @Produces(Array(MediaType.APPLICATION_XML))
  def initInstancePost(@PathParam("businessFunctionId") businessFunctionId: String,
                   @QueryParam("object-id") objectId: String,
                   @QueryParam("notificationReceiver-id") notificationReceiverId: String): Response = {
      initInstance(businessFunctionId,objectId,notificationReceiverId)
  }


  def initInstance( businessFunctionId: String,
                    objectId: String,
                   notificationReceiverId: String): Response = {
    val params = Option(objectId).map(f=>Map("this"->SimpleValue(objectId))).getOrElse(Map()) ++
      Option(notificationReceiverId).map(f=>Map("NotificationReceiverID"->SimpleValue(notificationReceiverId))).getOrElse(Map())

    if(!permissionProvider.getMetaPermission(businessFunctionId)(PermissionType.ReadExec)) throw new DataAccessError(businessFunctionId)
    val instId = DBProfiler.profile("total",execStatProvider,true){
       businessFunctionExecutor.initFunctionInstance(businessFunctionId, params)
    }
    val xml =
      "<?xml version='1.0' encoding='utf-8'?>\n" +
      "<result><bfInstanceId>" + instId +
       "</bfInstanceId>"+
       execStatProvider.toXml +
     "</result>"
    businessFunctionExecutor.setFalseValidations(instId,validationWarningPool.getWarnings)
    Response.status(Response.Status.OK).entity(xml).build()
  }

  /**
   * Получение следующего клиентского шага бизнес функции
   * @param bfInstanceId Идентификатор процесса
   * @return Описание следующего клиентского шага, который требуется
   *         выполнить в клиентском приложении, а также текущие значения
   *         переменных, находящихся в контексте.
   *         В случае окончания выполнения бизнес-функции, ответ будет содержать
   *         соответствующий флаг finishFlag.
   */
  @GET
  @Path("/next-step/{bfInstanceId}")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getNextStepGet(@PathParam("bfInstanceId") bfInstanceId: String): Response = {
    getNextStep(bfInstanceId)
  }

  @POST
  @Path("/next-step/{bfInstanceId}")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getNextStepPost(@PathParam("bfInstanceId") bfInstanceId: String): Response = {
      getNextStep(bfInstanceId)
  }

  def getNextStep(bfInstanceId: String): Response = {

    val (result,resultValue) = businessFunctionExecutor.getNextClientStep(bfInstanceId)
    val context = businessFunctionExecutor.getContext(bfInstanceId)

    def getStringRepresentation(x: Value): String = {
      x match {
        case e: EntityInstance =>
          e.getId
        case s: SimpleValue =>
          s.data match {
            case a: Array[Byte] => Base64.encodeBytes(a)
            case _ => Option(s.toString).getOrElse("")
          }
        case _ =>
          Option(x).map(_.toString).getOrElse("")
      }
    }
    val answer = if(result.isDefined){
      val xml =
        <result>
          {
          val step = result.get
          val stepMeta =
            if (step.xml != null)
              <stepMeta>
                {step.xml}
              </stepMeta>
            else null
          val stepResult = if (!context.isEmpty) {
            <previosServerStepResult>
              {context.map(_ match {
              case (k, v) =>
                val objText = Option(v).map(getStringRepresentation).getOrElse("")
                Elem(null, k, null, scala.xml.TopScope, Unparsed(objText))
            })}
            </previosServerStepResult>
          } else null
          Seq(stepMeta, stepResult).filterNot(_ == null)
          }
        </result>

      val sw = new StringWriter()
      XML.write(sw, scala.xml.Utility.trim(xml), "UTF-8", true, null)
      sw.toString
    } else{
          val falseValidators = businessFunctionExecutor.getFalseValidations(bfInstanceId).toSeq
          val validationStr = if (falseValidators.isEmpty) ""
                              else ""+ new Model2Xml().validationRulesToXml(falseValidators)
          "<result><finishFlag>true</finishFlag><validation>"+validationStr +"</validation></result>"   }
    if(!result.isDefined) processStateProvider.dropProcess(bfInstanceId)
    Response.status(Response.Status.OK).entity(answer).build()
  }

  /**
   * Получает от клиента результат выполнения клиентского шага бизнес функции.
   * @param processId Идентификатор процесса
   * @param clientMessage Строковое представление результатов выполнения шага
   * @return Ответ сервера об успешном получении результатов БФ
   */
  @POST
  @Path("/client-result/{bfInstanceId}")
  @Consumes(Array(MediaType.APPLICATION_XML))
  @Produces(Array(MediaType.APPLICATION_XML))
  def clientResult(@PathParam("bfInstanceId") processId: String, clientMessage: String): Response = {
    // отправить результат клиентского шага
    validationWarningPool.addWarnings(businessFunctionExecutor.getFalseValidations(processId).toSeq)
    businessFunctionExecutor.setClientResult(processId, clientMessage)
    businessFunctionExecutor.setFalseValidations(processId,validationWarningPool.getWarnings)
    Response.status(Response.Status.OK)
      .entity(new Model2Xml().standardAnswerToXml(0, "OK"))
      .build()
  }

}
