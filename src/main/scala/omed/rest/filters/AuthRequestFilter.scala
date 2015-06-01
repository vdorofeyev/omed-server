package omed.rest.filters

import com.sun.jersey.spi.container.{ContainerRequest, ContainerRequestFilter}
import scala.util.matching.Regex
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Context
import javax.ws.rs.WebApplicationException

import omed.auth.LoginError
import javax.ws.rs.ext.Provider

/**
 * Фильтр входящих запросов для проверки аутентификации.
 * Проверка аутентификации соответствует проверке наличия сессии.
 * Для некоторых ресурсов проверка не осуществляется.
 */
@Provider
class AuthRequestFilter extends ContainerRequestFilter {

  /**
   * Объектное представление HTTP-запроса
   */
  @Context var httpRequest: HttpServletRequest = null

  /**
   * Обрабатывает входящие запросы и для каждого из них проверяет наличие
   * созданного ранее объекта сессии, управляемого сервлет-контейнером.
   * Если такой объект не существует и URI запроса не входит в перечень
   * общедоступных, такая ситуация является исключительной, формируется
   * исключение типа [[omed.auth.LoginError]]
   * @param request Объектное представление запроса
   * @return Входящий запрос без изменений
   */
  def filter(request: ContainerRequest): ContainerRequest = {
    try {
    // Проверяем входящий запрос на соответствие одной из масок
    val matchExclusion = (rx: Regex) => rx.findPrefixMatchOf(
      request.getPath(false)).isDefined
    // Если доступ к ресурсам не ограничен, пропускаем запрос
    if (AuthRequestFilter.excludedURIs exists matchExclusion)
      request
    else {
      // Иначе, проверяем наличие доступа
      checkAuth()
      request
    }
    } catch {
      case e: RuntimeException =>
        // все Runtime исключения пропускаем без изменения
        throw e
      case e @ _ =>
        // оборачиваем все исключения в RuntimeException для того,
        // чтобы корректно отработал ExceptionMapper
        throw new RuntimeException(e)
    }
  }

  /**
   * Проверяет наличие сессии и в случае отсутствия генерирует исключение
   */
  def checkAuth() {
    val session = Option(httpRequest.getSession(false))
    if (session.isEmpty)
      throw new LoginError
  }
}

/**
 * Объект-компаньон для класса AuthRequestFilter.
 */
object AuthRequestFilter {

  /**
   * Список регулярных выражений, для которых
   * проверка аутентификации не проводится
   */
  val excludedURIs = Seq(
    "test"r,
    "auth"r,
    "data/drop-cache"r,
    "diag"r,
    "gate/fer"r
  )

}
