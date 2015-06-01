package omed.rest.providers

import javax.ws.rs.ext.{Provider, ExceptionMapper}
import javax.ws.rs.core.{HttpHeaders, MediaType, Response}
import java.util.logging.{Logger, Level}
import java.io.{PrintWriter, StringWriter}
import xml.XML

/**
 * Общий обработчик исключительных ситуаций.
 *
 * Формат:
 * <result>
 * <returnCode>[формальный признак результата]</returnCode>
 * <message>[сообщение_пользователю]</message>
 * <errorStackTrace>[Полный stack trace ошибки]</errorStackTrace>
 * </result>
 */
@Provider
class BaseExceptionMapper extends ExceptionMapper[Throwable] {
  /**
   * Формирует сообщение о возникшем неустановленном исключении
   * @param e Перехваченное исключение
   * @return Ответ сервиса с сообщением об ошибке
   */
  def toResponse(e: Throwable): Response = {
    val logger = Logger.getLogger(this.getClass.getName)
    logger.log(Level.SEVERE, "", e)

    val stw = new StringWriter()
    val pw = new PrintWriter(stw)
    e.printStackTrace(pw)
    val trace = stw.toString

    val message =
      <result>
        <returnCode>-1</returnCode>
        <message>{ Option(e.getMessage).getOrElse("") }</message>
        <errorStackTrace>{ trace }</errorStackTrace>
      </result>

    val sw = new StringWriter()
    XML.write(sw, message, "UTF-8", true, null)
    val xmlMessage = sw.toString

    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
      .entity(xmlMessage)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
      .build()
  }
}
