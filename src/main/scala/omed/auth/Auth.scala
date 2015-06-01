package omed.auth

/**
 * Интерфейс к сервису авторизации.
 */
trait Auth {

  type DocType = PatientDocType.Value

  /**
   * Проверить пользователя.
   *
   * @param domain Идентификатор домена
   * @param username Логин пользователя
   * @param author Логин автора, который делегирует права
   * @param password Пароль пользователя
   * @return (<Пользователь>, <Автор>)
   */
  def authenticateByPassword(domain: Int, username: String, author: String, password: String): (Account, Account)

  /**
   * Проверить пациента по документу.
   *
   * @param domain Идентификатор домена
   * @param username ФИО
   * @param number Номер документа
   * @param doctype Тип документа
   * @return (<Пользователь>, <Автор>) В качестве автора используется пользователь
   */
  def authenticateByDocument(domain: Int, username: String, number: String, doctype: DocType): (Account, Account)

  /**
   * Создание новой сессии.
   *
   * @param existingSessionId Существующий идентификатор сессии
   * @param domain Идентификатор домена
   * @param userId Идентификатор пользователя
   * @param authorId Идентификатор автора изменений, который делегирует права
   * @param userAgent Информация о клиенте (инфо о браузере или любом другом приложении)
   * @param userIp IP-адрес хоста
   * @return Идентификатор новой сессии
   */
  def login(existingSessionId: String, domain: Int, userId: String, authorId: String, userAgent: String, userIp: String): String

  /**
   * Логирование неудачных попыток создания новой сессии
   *
   * @param domain Идентификатор домена
   * @param username Пользователь
   * @param userAgent Информация о клиенте (инфо о браузере или любом другом приложении)
   * @param userIp IP-адрес хоста
   */
  def trace(domain: Int, username: String, userAgent: String, userIp: String): Unit

  /**
   * Закрыть сессию.
   * 
   * @param sessionId Идентификатор сессии
   */

  def logout(sessionId: String): Unit

  def getTimeZone(domainId:Int):String

  def createSystemSession(domain: Int):String
}