package omed.db

import java.sql.{SQLException, Types, ResultSet}
import java.util.logging.Logger
import omed.errors.DataError
import omed.cache.ExecStatProvider
import com.google.inject.Inject

/**
  *
  */
class DBProviderImpl extends DBProvider {

   private val logger = Logger.getLogger(DB.getClass.toString)

   private def params2String(params: List[(String, Object)]) =
     if (params != null)
       params.foldLeft(new StringBuilder())((sb, param) => {
         // привести значение параметра к строке и обрезать её если она[строка]
         // больше 1000 символов
         val paramValue = {
           val p = Option(param._2).getOrElse("null").toString
           p.take(1000)
         }
         sb.append(param._1 + "= " + paramValue + "\n")
       }).toString
     else "null"

   private def logParams(params: List[(String, Object)]) {
     this.logger.severe("Params: \n" + this.params2String(params))
   }

   /**
    * Подготовить выражение `java.sql.CallableStatement` для дальнейшей работы.
    *
    * Пример 1:
    * {{{
    *     val statement =
    *         this.prepareStatement(connection, methodName, sessionId, params)
    *     statement.execute()
    * }}}
    *
    * Пример 2:
    * {{{
    *     val statement =
    *         this.prepareStatement(connection, methodName, sessionId, params)
    *     val resultAvailable = statement.execute()
    * }}}
    *
    * @param connection Соединение с базой данных
    * @param methodName Имя хранимой процедуры
    * @param sessionId Идентификатор сессии
    * @param params Параметры хранимой процедуры
    * @return Подготовленный к выполненению statement
    * @throws java.lang.Exception Любая ошибка
    */
   def prepareStatement(connection: java.sql.Connection,
     methodName: String,
     sessionId: String,
     params: List[(String, Object)]) = {

     val result = try {

       // шаблон для параметров
       val paramsTemplates = if (params != null && params.count(_ => true) != 0)
         params.foldLeft(new StringBuilder())(
           (sb, param) => sb.append("@" + param._1 + " = ?, "))
       else ""

       // шаблон вызова хранимой процедуры
       val callTemplate = "{ ? = call " + methodName + "(@SessionID = ?, " + paramsTemplates + "@ResultMess = ?) }"

       val statement = connection.prepareCall(callTemplate)
       statement.registerOutParameter(1, Types.INTEGER)
       //statement.setString(2, methodName) // имя метода
       statement.setString(2, sessionId)
       // добавить параметры
       if (params != null) {
         var i = 1
         for (param <- params) {
           statement.setObject(2 + i, param._2)
           i += 1
         }
       }
       // вычислить позицию параметра для возврата результата
       val resMessPos = if (params != null && params.size != 0)
         (3 + params.size)
       else 3
       statement.registerOutParameter(resMessPos, Types.VARCHAR)

       statement
     } catch {
       case e @ _ => {
         throw new DataError("DB prepareStatement failed. Params: \n" + this.params2String(params), code=0, error=e)
       }
     }

     result
   }

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
     params: List[(String, Object)], execStatProvider :ExecStatProvider = null,timeOut:Int =0): ResultSet = {

     try {
       val statement = prepareStatement(connection, methodName, sessionId, params)
       if(timeOut!=0) statement.setQueryTimeout(timeOut)
       //statement.setQ
       val resultAvailable = DBProfiler.profile(methodName,execStatProvider) { statement.execute() }
       val result = if (resultAvailable) statement.getResultSet()
       else {
         val resMessPos = if (params != null && params.size != 0) (3 + params.size) else 3

         val retCode = statement.getInt(1)

      //   statement.getRe
         val retMessage = Option(statement.getString(resMessPos)).getOrElse("Запрос не вернул ожидаемый набор данных")

         throw new DataError(retMessage + "\n" + "DB exec failed " +  methodName +  "\n Params: \n" +  this.params2String(params), code=retCode)
       }

         result
     } catch {
       case f : SQLException => throw new DataError(f.getMessage + "\n" + "DB exec failed "+ methodName + "\n Params: \n" +  this.params2String(params))
       case e : DataError => throw e
       case e @ _ => {
         throw new DataError("DB exec failed."+ methodName+ " Params: \n" + this.params2String(params), code = -1, error=e)
       }
     }
   }

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
     params: List[(String, Object)],execStatProvider:ExecStatProvider = null,timeOut:Int =0) {

     try {
       val statement =
         prepareStatement(connection, methodName, sessionId, params)
       if(timeOut!=0) statement.setQueryTimeout(timeOut)
       DBProfiler.profile(methodName,execStatProvider) { statement.execute() }

       val resMessPos = if (params != null && params.size != 0) (3 + params.size) else 3

       val retCode = statement.getInt(1)
       val retMessage = statement.getString(resMessPos)

       if (retCode != 0)
         throw new DataError(retMessage, code=retCode)

     } catch {
       case f : SQLException => throw new DataError(f.getMessage + "\n" + "DB exec failed "+ methodName + "\n Params: \n" +  this.params2String(params))
       case e : DataError => throw e
       case e @ _ => {
         throw new DataError("DB exec for no result failed. Params: \n" + this.params2String(params), code = -1, error=e)
       }
     }

   }
   // -------------------------------------------------------------------------------------
 }
