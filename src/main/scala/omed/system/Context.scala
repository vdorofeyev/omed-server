package omed.system

import omed.model.{SimpleValue, Value}
import java.util.TimeZone

/**
 * Используется для хранения данных контекста.
 * 
 * @param sessionId Идентификатор сессии
 * @param domainId Номер домена, в котором работает пользователь
 * @param userId Идентификатор пользователя (Guid)
 * @param authorId Должен быть заполнен
 * @param isSuperUser Пользователь суперюзер
 * @param request URL запроса
 */
class Context(
  val sessionId: String,
  val domainId: Int,
  val hcuId: String,
  val userId: String,
  val authorId: String,
  val isSuperUser: Boolean,
  val request: String,
  val timeZone:TimeZone,
  val roleId :String
)    {
  def getSystemVariables:Map[String,Value]={
     Map("UserID"->SimpleValue(userId),"AuthorID"->SimpleValue(authorId),"DomainID"->SimpleValue(domainId),"HCUID"->SimpleValue(hcuId),"RoleID"->SimpleValue(roleId),"__SessionID"->SimpleValue(sessionId))
  }
}
