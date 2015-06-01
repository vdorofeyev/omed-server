package omed.rest.providers

import javax.ws.rs.ext.{Provider, ExceptionMapper}
import javax.ws.rs.core.Response
import java.util.logging.{Logger, Level}
import javax.ws.rs.WebApplicationException

/**
 * Общий обработчик исключительных ситуаций,
 * возникающих в процессе обработки REST-запросов.
 */
@Provider
class WebAppExceptionMapper extends ExceptionMapper[WebApplicationException] {
  /**
   * Формирует сообщение о возникшем неустановленном исключении
   * @param e Перехваченное исключение
   * @return Ответ сервиса с сообщением об ошибке
   */
  def toResponse(e: WebApplicationException): Response = {

    if (e.getResponse.getStatus == Response.Status.NOT_FOUND.getStatusCode) {
      val logger = Logger.getLogger(this.getClass.getName)
      logger.log(Level.SEVERE, "", e)
      return e.getResponse
    }
    else
      new BaseExceptionMapper().toResponse(e)
  }
}
