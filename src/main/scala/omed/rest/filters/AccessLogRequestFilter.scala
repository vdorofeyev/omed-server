package omed.rest.filters

import com.sun.jersey.spi.container.{ContainerRequest, ContainerRequestFilter}
import scala.util.matching.Regex
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Context
import javax.ws.rs.WebApplicationException

import omed.auth.AuthError
import javax.ws.rs.ext.Provider
import java.util.logging.Logger

/**
 * Фильтр входящих запросов для логирования доступа к ресурсам
 */
@Provider
class AccessLogRequestFilter extends ContainerRequestFilter {

  /**
   * Стандартный логгер
   */
  val logger = Logger.getLogger(this.getClass.getName)

  /**
   * Объектное представление HTTP-запроса
   */
  @Context var httpRequest: HttpServletRequest = null

  /**
   * Обрабатывает входящие запросы и для каждого из них сохраняет в лог
   * основные параметры, такие как HTTP-метод обращения, URI и идентификатор
   * сессии (внутренний системный идентификатор ОМед)
   * @param request Объектное представление запроса
   * @return Входящий запрос без изменений
   */
  def filter(request: ContainerRequest): ContainerRequest = {
    val method = request.getMethod
    val uri = request.getRequestUri.toString
    val sessionId = Option(httpRequest.getSession(false))
      .map(_.getAttribute("sessionId"))
      .orNull

    val message = String.format("%s :: %s :: %s", method, uri, sessionId)
    logger.info(message)

    request
  }

}

