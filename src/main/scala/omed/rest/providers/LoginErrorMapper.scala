package omed.rest.providers

import javax.ws.rs.ext.{Provider, ExceptionMapper}
import javax.ws.rs.core.{MediaType, HttpHeaders, Response}

import omed.auth.LoginError

/**
 * Обработчик исключений в процесса аутентификации
 */
@Provider
class LoginErrorMapper extends ExceptionMapper[LoginError] {
  /**
   * Формирует сообщение об ошибке аутентификации
   * @param e Перехваченное исключение
   * @return Ответ сервиса с сообщением об ошибке
   */
  def toResponse(e: LoginError): Response = {
    return Response.status(Response.Status.UNAUTHORIZED)
      .entity("Пользователь не аутентифицирован")
      .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN + "; charset=UTF-8")
      .build()
  }
}
