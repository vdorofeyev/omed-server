package omed.TimerBF

import omed.db.{ConnectionProvider, DBProfiler}
import java.sql.Types
import java.lang.RuntimeException
import com.google.inject.Inject

/**
 * Created with IntelliJ IDEA.
 * User: naryshkinaa
 * Date: 28.03.14
 * Time: 17:44
 * To change this template use File | Settings | File Templates.
 */
class SystemSessionProviderImpl extends SystemSessionProvider {
  @Inject
  var connectionProvider: ConnectionProvider = null

  val sessions :scala.collection.mutable.Map [Int,String] = scala.collection.mutable.Map[Int,String]()

  def getProductionDomains:Seq[Int]={
    //todo Get ALL Domains
    Seq(1)
  }
  def getSystemSessionForDomain(domain:Int):String  ={
    if(!sessions.contains(domain)) {
       sessions+= (domain->createSystemSession(domain))
    }
    sessions(domain)
  }
  def createSystemSession(domain:Int):String={
     connectionProvider.withConnection {
        connection => {
          val nativeSql = "{ call _Session._Create ( " +
            "@PersonID = ?, " +
            "@AuthorID = ?, " +
            "@HostIP = ?, " +
            "@HostName = ?, " +
            "@ClientDescr = ?, " +
            "@Domain = ?, " +
            "@ID = ?, " +
            "@ResultMess = ? ) }"

          try {
            DBProfiler.profile("_Session._Create") {
              val statement = connection.prepareCall(nativeSql)
              statement.setString(1, null)
              statement.setString(2, null)
              statement.setString(3, "localhost")
              statement.setString(4, "system session")
              statement.setString(5, "system session")
              statement.setInt(6, domain)
              statement.registerOutParameter(7, Types.VARCHAR)
              statement.registerOutParameter(8, Types.VARCHAR)
              statement.execute()

              // result message for debug
              val msg = statement.getString(8)
              if( msg != null ) throw new RuntimeException( msg)
              statement.getString(7) // uuid of session
            }
          } catch {
            case e @ _ => throw new Exception(
              "Ошибка при обращении к DAL методу. " +
                "Сообщение: '" + e.getMessage() + "'. ", e)
          }
        }
     }
  }
}
