package omed.db

import omed.cache.ExecStatProvider

/**
 * Интерфейс уровня доступа к данным
 */
trait DBProvider {

  /**
   * Выполнить хранимую процедуру и получить `ResultSet` с данными.
   *
   * Проверяется наличие выборки.
   * Если выборки нет, то выбрасывает исключение `java.lang.Exception`.
   *
   * @param connection Соединение с базой данных
   * @param methodName Имя хранимой процедуры
   * @param sessionId Идентификатор сессии
   * @param params Параметры хранимой процедуры
   * @return Выборка
   * @throws java.lang.Exception если нет выборки или произошла любая другая ошибка
   */
  def dbExec(connection: java.sql.Connection,
    methodName: String,
    sessionId: String,
    params: List[(String, Object)],execStatProvider :ExecStatProvider = null,timeOut:Int =0): java.sql.ResultSet

  /**
   * Выполнить храниму процедуру.
   *
   * @param connection Соединение с базой данных
   * @param methodName Имя хранимой процедуры
   * @param sessionId Идентификатор сессии
   * @param params Параметры хранимой процедуры
   * @throws java.lang.Exception Любая ошибка
   */
  def dbExecNoResultSet(connection: java.sql.Connection,
    methodName: String,
    sessionId: String,
    params: List[(String, Object)],execStatProvider :ExecStatProvider = null,timeOut:Int =0)
}