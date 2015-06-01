package omed.auth

import java.util.UUID

import omed.db.DataAccessSupport
import java.sql.{ Connection, ResultSet, Types }

import omed.db.{DBProfiler, ConnectionProvider}
import com.google.inject.Inject

/**
 * Реализация сервиса авторизации.
 */
class AuthBean extends Auth {

  /**
   * Фабрика подключений к источнику данных.
   */
  @Inject
  var connectionProvider: ConnectionProvider = null

  /**
   * Проверить пользователя.
   *
   * @param domain Идентификатор домена
   * @param username Логин пользователя
   * @param author Логин автора, который делегирует права
   * @param password Пароль пользователя
   * @return (<Пользователь>, <Автор>)
   */
  def authenticateByPassword(domain: Int, username: String, author: String, password: String): (Account, Account) = {
    connectionProvider.withConnection {
      connection =>
        // retrieve users from database
        def fetchUsers(username: String, password: String = null) = {
          // transform input strings to escape all non alphanum chars
          def cleanUserName(inp: String) = inp.replaceAll("'", "''")
          def cleanPassword(inp: String) = inp.replaceAll("'", "''")

          if (username != null) {
            val userNativeSql =
              "select u.ID, e.FirstName, e.LastName, u.IsSuperUser, h.ID as hcuId " +
                "from UserAccount.DataAll u " +
                "left join Employee.DataAll e " +
                "on u.EmployeeID = e.ID " +
                "left join _domain.data d on u._Domain = d.Number left join hcu h on d.RootOUID = h.ID " +
                "where u.UserLogin = '" + cleanUserName(username) + "' " +
                "and u._Domain = " + domain

            val nativeSql = if (password != null) {
              val pwdCondition = "u.PwdHash = '" + cleanPassword(password) + "'"
              userNativeSql + " and " + pwdCondition
            } else userNativeSql
            val statement = connection.createStatement()
            statement.executeQuery(nativeSql)
            DBProfiler.profile("query Auth (select from UserAccount.DataAll)") {
              statement.executeQuery(nativeSql)
            }
            val rs = statement.getResultSet()
            if (rs != null && rs.next) rs else null
          } else null

        }

        // extract user data from db answer
        def extractUserData(emp: ResultSet) = {

          if (emp != null) {
            val userId = emp.getObject(1).toString()
            val firstName = emp.getString(2)
            val lastName = emp.getString(3)
            val realName =
              (Option(firstName).getOrElse("") + " " +
                Option(lastName).getOrElse("")).trim()
            val isSuperUser = Option(emp.getString(4))
              .map("y" == _.toLowerCase).getOrElse(false)
            val hcuId = emp.getString(5)

            (userId, realName, isSuperUser, hcuId)
          } else
            (null, null, false, null)

        }

        val users = fetchUsers(username, Option(password).getOrElse(""))
        val authors = fetchUsers(author)

        // extract user and author from db
        val (userId, userRealName, isSuperUser, userHcuId) = extractUserData(users)
        val (authorId, authorRealName, isSuperAuthor, authorHcuId) = extractUserData(authors)

        val result = {
          if (userId == null)
            (null, null)
          else if (author != null && authorId == null)
            (null, null)
          else {
            val userAccount = Account(userId, userRealName, isSuperUser, domain, userHcuId)
            val authAccount = if (authorId != null)
              Account(authorId, authorRealName, isSuperAuthor, domain, authorHcuId)
            else userAccount

            (userAccount, authAccount)
          }
        }

        result
    }
  }

  /**
   * Проверить пациента по документу.
   *
   * @param domain Идентификатор домена
   * @param username ФИО
   * @param number Номер документа
   * @param doctype Тип документа
   * @return (<Пользователь>, <Автор>) В качестве автора используется пользователь
   */
  def authenticateByDocument(domain: Int, username: String, number: String, doctype: DocType): (Account, Account) = {
    connectionProvider.withConnection {
      connection =>

        def cleanUserName(inp: String) = inp.replaceAll("'", "''").replaceAll("\\s", "")

        // fetch user by SNILS
        def fetchBySnils() = {
          val nativeSql = "select u.ID, p.FirstName, p.LastName, u.IsSuperUser, h.ID as hcuId " +
            "from UserAccount.DataAll u " +
            "inner join Patient.DataAll p " +
            "on u.EmployeeID = p.ID " +
            "left join _domain.data d on d.Number = u._Domain left join hcu h on d.RootOUID = h.ID " +
            "where isnull(p.LastName, '') + isnull(p.FirstName, '') + isnull(p.SecondName, '') = ? " +
            "and (replace(replace(u.PwdHash, ' ', ''), '-', '') = ? " +
            "or replace(replace(p.SNILS, ' ', ''), '-', '') = ?) " +
            "and u._Domain = ?"

          val login = Option(username).map(cleanUserName).orNull
          val snils = Option(number).map(_.replaceAll("(\\s|-)+", "")).orNull
          val statement = connection.prepareStatement(nativeSql)
          statement.setString(1, login)
          statement.setString(2, snils)
          statement.setString(3, snils)
          statement.setInt(4, domain)


          DBProfiler.profile("query Auth by SNILS") {
            statement.executeQuery()
          }
          val rs = statement.getResultSet()
          if (rs != null && rs.next) rs else null
        }

        // fetch user by passport number
        def fetchByPassport() = {
          val nativeSql = "select u.ID, p.FirstName, p.LastName, u.IsSuperUser, h.ID as hcuId " +
            "from UserAccount.DataAll u " +
            "inner join Patient.DataAll p " +
            "on u.EmployeeID = p.ID " +
            "left join _domain.data d on d.Number = u._Domain left join hcu h on d.RootOUID = h.ID " +
            "where isnull(p.LastName, '') + isnull(p.FirstName, '') + isnull(p.SecondName, '') = ? " +
            "and ((replace(isnull(p.ActualIdentityDocumentSeries, ''), ' ', '') + " +
            "replace(isnull(p.ActualIdentityDocumentNumber, ''), ' ', '')) = ?) " +
            "and u._Domain = ?"

          val login = Option(username).map(cleanUserName).orNull
          val passport = Option(number).map(_.replaceAll("(\\s|-)+", "")).orNull
          val statement = connection.prepareStatement(nativeSql)
          statement.setString(1, login)
          statement.setString(2, passport)
          statement.setInt(3, domain)

          DBProfiler.profile("query Auth by Passport") {
            statement.executeQuery()
          }
          val rs = statement.getResultSet()
          if (rs != null && rs.next) rs else null
        }

        // fetch user by policy number
        def fetchByPolicy() = {
          val nativeSql = "select u.ID, p.FirstName, p.LastName, u.IsSuperUser, h.ID as hcuId " +
            "from UserAccount.DataAll u " +
            "inner join Patient.DataAll p " +
            "on u.EmployeeID = p.ID " +
            "left join _domain.data d on d.Number = u._Domain left join hcu h on d.RootOUID = h.ID " +
            "where isnull(p.LastName, '') + isnull(p.FirstName, '') + isnull(p.SecondName, '') = ? " +
            "and ((replace(isnull(p.ActualPolisSeries, ''), ' ', '') + " +
            "replace(isnull(p.ActualPolisNumber, ''), ' ', '')) = ?) " +
            "and u._Domain = ?"

          val login = Option(username).map(cleanUserName).orNull
          val policy = Option(number).map(_.replaceAll("(\\s|-)+", "")).orNull
          val statement = connection.prepareStatement(nativeSql)
          statement.setString(1, login)
          statement.setString(2, policy)
          statement.setInt(3, domain)

          DBProfiler.profile("query Auth by Policy") {
            statement.executeQuery()
          }
          val rs = statement.getResultSet()
          if (rs != null && rs.next) rs else null
        }

        // extract user data from db answer
        def extractUserData(emp: ResultSet) = {

          if (emp != null) {
            val userId = emp.getObject(1).toString()
            val firstName = emp.getString(2)
            val lastName = emp.getString(3)
            val realName =
              (Option(firstName).getOrElse("") + " " +
                Option(lastName).getOrElse("")).trim()
            val isSuperUser = Option(emp.getString(4))
              .map("y" == _.toLowerCase).getOrElse(false)
            val hcuId = emp.getString(5)

            (userId, realName, isSuperUser, hcuId)
          } else
            (null, null, false, null)

        }

        val user = doctype match {
          case PatientDocType.Snils => fetchBySnils()
          case PatientDocType.Password => fetchByPassport()
          case PatientDocType.Policy => fetchByPolicy()
        }

        // extract user and author from db
        val (userId, userRealName, isSuperUser, hcuId) = extractUserData(user)

        val result = {
          if (userId == null)
            (null, null)
          else {
            val userAccount = Account(userId, userRealName, isSuperUser, domain, hcuId)
            (userAccount, userAccount)
          }
        }

        result
    }
  }

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
  def login(
    existingSessionId: String,
    domain: Int, userId: String, authorId: String,
    userAgent: String, userIp: String): String = {
    connectionProvider.withConnection {
      connection =>
        if (existingSessionId != null) {
          val reason = "re-login"
          txlogout(connection, existingSessionId, reason)
        }
        txlogin(connection, domain, userId, authorId, userAgent, userIp)
    }
  }

  /**
   * Закрыть сессию.
   *
   * @param sessionId Идентификатор сессии
   */
  def logout(sessionId: String): Unit = {
    connectionProvider.withConnection {
      connection =>
        val reason = "client call"
        txlogout(connection, sessionId, reason)
    }
  }

  /**
   * Логирование неудачных попыток создания новой сессии
   *
   * @param domain Идентификатор домена
   * @param username Пользователь
   * @param userAgent Информация о клиенте (инфо о браузере или любом другом приложении)
   * @param userIp IP-адрес хоста
   */
  def trace(
    domain: Int, username: String,
    userAgent: String, userIp: String): Unit = {
    connectionProvider.withConnection {
      connection =>
        txtrace(connection, domain, username, userAgent, userIp)
    }
  }

  private def txlogin(
    connection: Connection,
    domain: Int, userId: String, authorId: String,
    userAgent: String, userIp: String): String = {
    val s=   connection.getMetaData().getDriverName().split(' ')(0).toLowerCase()
    var  result = "" // Session  (string)uid
    s match {
      case "postgresql" => result =  {
        val statement = connection.prepareCall("{call _session._create(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}")
        //                                                             1  2  3  4  5  6  7  8  9  10

        val resultmess: String = null
        try {

          statement.setObject(1, UUID.fromString(userId))
          statement.setObject(2, UUID.fromString(authorId))
         statement.setObject(3,null)
          statement.setString(4, userIp)
          statement.setString(5, userIp)
          statement.setString(6, userAgent)
          statement.setInt(7, domain)
          statement.setObject(8, "Y", Types.CHAR)
          statement.registerOutParameter(9, Types.OTHER)
          //     statement.setObject(9, personId)
          statement.registerOutParameter(10, Types.VARCHAR)
          //     statement.setString(10, null)
          //statement.registerOutParameter(11, Types.INTEGER)

          statement.execute().toString()
        }
        catch {
          case e  => throw new Exception(
            "txlogin PS: Ошибка при обращении к DAL методу. " +
              "Сообщение: '" + e.getMessage() + "'. ", e)
        }

        statement.getObject(9).toString()  // -> result

      }
      case "microsoft" =>
        result =  {
          val nativeSql = "{ call _Session._Create ( " +
            "@PersonID = ?, " +   //1
            "@AuthorID = ?, " +   //2
                                  //3
            "@HostIP = ?, " +     //4
            "@HostName = ?, " +
            "@ClientDescr = ?, " +
            "@Domain = ?, " +
            "@ID = ?, " +
            "@ResultMess = ? ) }"

          try {
            val statement = connection.prepareCall(nativeSql)
            statement.setString(1, userId.toString)
            statement.setString(2, authorId.toString)
            statement.setString(3, userIp)
            statement.setString(4, userIp)
            statement.setString(5, userAgent)
            statement.setInt(6, domain)
            statement.registerOutParameter(7, Types.VARCHAR)
            statement.registerOutParameter(8, Types.VARCHAR)
            statement.execute()

            // result message for debug
            val msg = statement.getString(8)
            if( msg != null ) throw new RuntimeException( msg)
            statement.getString(7) // uuid of session -> result

          } catch {
            case e => throw new Exception(
              "txlogin MS: Ошибка при обращении к DAL методу. " +
                "Сообщение: '" + e.getMessage() + "'. ", e)
          }
        }
    }

    result
  }

  private def txtrace(
    connection: Connection,
    domain: Int, username: String,
    userAgent: String, userIp: String) {

    val closingReason = String.format(
      "login_fail: login=%s", username)

    val nativeSql = "{ call _Session._Audit ( " +
      "@HostIP = ?, " +
      "@HostName = ?, " +
      "@ClientDescr = ?, " +
      "@Domain = ?, " +
      "@SessionClosingReason = ?, " +
      "@ResultMess = ? ) }"

    try {
      DBProfiler.profile("_Session._Audit") {
        val statement = connection.prepareCall(nativeSql)
        statement.setString(1, userIp)
        statement.setString(2, userIp)
        statement.setString(3, userAgent)
        statement.setInt(4, domain)
        statement.setString(5, closingReason)
        statement.registerOutParameter(6, Types.VARCHAR)
        statement.execute()
      }
    } catch {
      case e @ _ => throw new Exception(
        "txtrace: Ошибка при обращении к DAL методу. " +
          "Сообщение: '" + e.getMessage() + "'. ", e)
    }
  }

  private def txlogout(
    connection: Connection,
    sessionId: String, closingReason: String) {
    val nativeSql = "{ call _Session._Close ( " +
      "@ID = ?, " +
      "@SessionClosingReason = ?, " +
      "@ResultMess = ? ) }"

    try {
      DBProfiler.profile("_Session._Close") {
        val statement = connection.prepareCall(nativeSql)
        statement.setString(1, sessionId)
        statement.setString(2, closingReason)
        statement.registerOutParameter(3, Types.VARCHAR)
        statement.execute()
      }
    } catch {
      case e @ _ => throw new Exception(
        "txlogout: Ошибка при обращении к DAL методу. " +
          "Сообщение: '" + e.getMessage() + "'. ", e)
    }
  }

  def getTimeZone(domainId:Int):String={
    //отлавливаем ошибку получения тайм-зоны для возможности прогрузить метаданные отвечающие за тайм-зону на новых сервисах
    try{
      val sql = "select TimeZone from _Domain where Number=?"
      val dbResult = connectionProvider withConnection {
        connection =>
            val statement = connection.prepareStatement(sql)
            statement.setString(1, domainId.toString)
            statement.executeQuery()
      }

      val timeZone = if (dbResult.next()) {
        val tmp = dbResult.getInt("TimeZone")
        val res= if(dbResult.wasNull()) 4 else tmp
        val sign = if(res>=0) "+" else "-"
        val strZone = if(scala.math.abs(res)<10) "0"+ scala.math.abs(res).toString else  scala.math.abs(res).toString
        sign+ strZone
      } else "+04"

      "GMT"+timeZone
    }
    catch {
      case _ => "GMT+04"
    }
  }

  def createSystemSession(domain: Int):String={
    connectionProvider.withConnection {
      connection =>
        txlogin(connection, domain, null, null, "system", null)
    }
  }
}
