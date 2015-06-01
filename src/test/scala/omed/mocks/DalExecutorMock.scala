package omed.mocks

import sun.reflect.generics.reflectiveObjects.NotImplementedException
import scala.collection.mutable.ArrayBuffer
import omed.db.DBProvider
import omed.cache.ExecStatProvider

class DalMethodInfo(
  val dalMethodName: String,
  val methodName: String,
  val sessionId: String,
  val params: List[(String, Object)])

object DalExecutorMock extends DBProvider {

  var dalFired = new ArrayBuffer[DalMethodInfo]

  def dbExec(connection: java.sql.Connection,
    methodName: String,
    sessionId: String,
    params: List[(String, Object)],execStatProvider:ExecStatProvider =null,timeOut:Int =0): java.sql.ResultSet = {

    this.dalFired +=
      new DalMethodInfo("dbExecNoResultSet", methodName, sessionId, params)

    throw new NotImplementedException
  }

  def dbExecNoResultSet(connection: java.sql.Connection,
    methodName: String,
    sessionId: String,
    params: List[(String, Object)],execStatProvider:ExecStatProvider =null,timeOut:Int =0) {

    this.dalFired +=
      new DalMethodInfo("dbExecNoResultSet", methodName, sessionId, params)
  }

}