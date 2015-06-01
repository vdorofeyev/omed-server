package omed.rest.providers

import javax.ws.rs.ext.{Provider, ExceptionMapper}
import javax.ws.rs.core.{MediaType, HttpHeaders, Response}

import omed.errors.{ObjectLockedException, MetaModelError}
import java.util.logging.{Level, Logger}
import java.io.{PrintWriter, StringWriter}
import xml.XML

/**
 * Обработчик исключений возникающих из-за рассогласований в метамодели.
 */
@Provider
class ObjectLockedExceptionMapper extends ExceptionMapper[ObjectLockedException] {
  /**
   *
   * @param e Перехваченное исключение
   * @return Ответ сервиса с сообщением об ошибке
   */
  def toResponse(e: ObjectLockedException): Response = {
    val message =
      <result>
        <returnCode>-6</returnCode>
        <message>{e.getMessage}</message>
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
