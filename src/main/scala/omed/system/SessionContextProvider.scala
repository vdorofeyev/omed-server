package omed.system

import com.google.inject.Inject
import javax.servlet.http.HttpServletRequest
import java.util.TimeZone
import omed.db.DBProfiler
import omed.cache.ExecStatProvider

/**
 * Провайдер контекста, основанный на данных сессии.
 */
class SessionContextProvider extends ContextProvider {
  @Inject
  var req: HttpServletRequest = null
  @Inject
  var execStatProvider : ExecStatProvider = null
  /**
   * Получить текущий контекст.
   */
  def getContext: Context = {
    val session = Option(req.getSession(false))
    if (session.isEmpty)
      throw new SecurityError

    val sessionId = session
      .map(_.getAttribute("sessionId"))
      .map(_.asInstanceOf[String])
      .orNull
    val domainId = session
      .map(_.getAttribute("domainId"))
      .map(_.asInstanceOf[Int])
      .getOrElse(Int.MinValue)
    val hcuId = session
      .map(_.getAttribute("hcuId"))
      .map(_.asInstanceOf[String])
      .orNull
    val userId = session
      .map(_.getAttribute("userId"))
      .map(_.asInstanceOf[String])
      .orNull
    val authorId = session
      .map(_.getAttribute("authorId"))
      .map(_.asInstanceOf[String])
      .orNull
    val isSuperUser = session
      .map(_.getAttribute("isSuperUser"))
      .map(_.asInstanceOf[Boolean])
      .getOrElse(false)
    val timeZone = session
      .map(_.getAttribute("timeZone"))
      .map(_.asInstanceOf[TimeZone])
      .getOrElse(TimeZone.getTimeZone("GMT+04"))
    val roleId = session
      .map(_.getAttribute("roleId"))
      .map(_.asInstanceOf[String])
      .orNull
    val request = getRequestURL()
    new Context(sessionId, domainId, hcuId, userId, authorId, isSuperUser, request,timeZone,roleId)
  }

  private def getRequestURL() = {
    Option(req.getContextPath).getOrElse("") +
      Option(req.getServletPath).getOrElse("") +
      Option(req.getPathInfo).getOrElse("") +
      Option(req.getQueryString).getOrElse("")
  }
}