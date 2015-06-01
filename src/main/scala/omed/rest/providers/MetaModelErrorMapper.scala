package omed.rest.providers

import javax.ws.rs.ext.{Provider, ExceptionMapper}
import javax.ws.rs.core.{MediaType, HttpHeaders, Response}

import omed.errors.{MetaModelError, NotFoundError}
import java.util.logging.{Level, Logger}
import java.io.{PrintWriter, StringWriter}
import xml.XML

/**
 * Обработчик исключений возникающих из-за рассогласований в метамодели.
 */
@Provider
class MetaModelErrorMapper extends ExceptionMapper[MetaModelError] {
  /**
   * Формирует сообщение об ошибке в метамодели системы.
   * @param e Перехваченное исключение
   * @return Ответ сервиса с сообщением об ошибке
   */
  def toResponse(e: MetaModelError): Response = {
    val logger = Logger.getLogger(this.getClass.getName)
    logger.log(Level.SEVERE, e.getMessage, e)

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
