package omed.rest.providers

import javax.ws.rs.ext.{Provider, ExceptionMapper}
import javax.ws.rs.core.{MediaType, HttpHeaders, Response}

import omed.errors.DataError
import xml.XML
import java.io.{PrintWriter, StringWriter}
import java.util.logging.{Level, Logger}

/**
 * Формат сообщения об ошибке в данных
 * <pre>
 * <result>
 *   <returnCode>[формальный признак результата]</returnCode>
 *   <message>[сообщение_пользователю]</message>
 *   <errorStackTrace>[Полный stack trace ошибки]</errorStackTrace>
 * </result>
 * </pre>
 */
@Provider
class DataErrorMapper extends ExceptionMapper[DataError] {
  /**
   * Формирует сообщение об ошибке работы с хранилищем данных
   * @param e Перехваченное исключение
   * @return Ответ сервиса с сообщением об ошибке
   */
  def toResponse(e: DataError): Response = {
    val logger = Logger.getLogger(this.getClass.getName)
    logger.log(Level.SEVERE, String.format("[%s] %s", e.code.asInstanceOf[Object], e.getMessage), e)

    val stw = new StringWriter()
    val pw = new PrintWriter(stw)
    e.printStackTrace(pw)
    val trace = stw.toString

    val message =
      <result>
        <returnCode>{ e.code }</returnCode>
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
