package omed.rest.providers

import javax.ws.rs.ext.{Provider, ExceptionMapper}
import javax.ws.rs.core.{MediaType, HttpHeaders, Response}

import omed.auth.AuthError

/**
 * Обработчик исключений типа [[omed.auth.AuthError]]
 */
@Provider
class AuthErrorMapper extends ExceptionMapper[AuthError] {
  /**
   * Формирует сообщение об ошибке авторизации
   * @param e Перехваченное исключение
   * @return Ответ сервиса с сообщением об ошибке
   */
  def toResponse(e: AuthError): Response = {
    return Response.status(Response.Status.FORBIDDEN)
      .entity("Доступ к ресурсу ограничен")
      .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN+"; charset=UTF-8")
      .build()
  }
}
