package omed.db

import java.sql.ResultSet
import omed.cache.ExecStatProvider

/**
 *
 */
object DB extends DBProvider {

  def prepareStatement(connection: java.sql.Connection,
                       methodName: String,
                       sessionId: String,
                       params: List[(String, Object)]) =
    (new DBProviderImpl).prepareStatement(connection, methodName, sessionId, params)

  // DBProvider ----------------------------------------------------------------
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
             params: List[(String, Object)],execStatProvider :ExecStatProvider = null,timeOut:Int =0): ResultSet =
    (new DBProviderImpl).dbExec(connection, methodName, sessionId, params,execStatProvider,timeOut)

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
                        params: List[(String, Object)],execStatProvider :ExecStatProvider = null,timeOut:Int =0) =
    (new DBProviderImpl).dbExecNoResultSet(connection, methodName, sessionId, params,execStatProvider,timeOut)

}
