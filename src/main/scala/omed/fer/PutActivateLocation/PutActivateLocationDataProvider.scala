package omed.fer.PutActivateLocation

import omed.db.{DB, DataAccessSupport, ConnectionProvider}

/**
  *
  */
class PutActivateLocationDataProvider(connectionProvider: ConnectionProvider) extends DataAccessSupport {

  case class TimetableEmployeeInfo(id: String, employeeId: String, employeeIDFER: String)

  val TimetableEmployeesQuery =
    """
      |select te.ID, te.EmployeeID, te.IDFER as EmployeeIDFER
      |from TimeTableEmployee te
      |where TimeTableManagementID = ?
    """.stripMargin

  def getTimetableEmployees(timetableId: String) = {
    val dbResult = connectionProvider withConnection {
      connection =>
        dataOperation {
          val statement = connection.prepareStatement(TimetableEmployeesQuery)
          statement.setString(1, timetableId)
          statement.executeQuery()
        }
    }

    val employeeList = scala.collection.mutable.ArrayBuffer[TimetableEmployeeInfo]()

    while (dbResult.next()) {
      val ID = dbResult.getString("ID")
      val employeeID = dbResult.getString("EmployeeID")
      val employeeIDFER = dbResult.getString("EmployeeIDFER")
      employeeList += TimetableEmployeeInfo(ID, employeeID, employeeIDFER)
    }

    employeeList.toSeq
  }

  def saveTimetablePutMark(sessionId: String, timetableEmployeeId: String, isSuccess: Boolean) = {
    connectionProvider withConnection {
      connection => {
        val valStr = if (isSuccess) "Y" else "N"
        dataOperation {
          DB.dbExecNoResultSet(connection, "[_Object].[EditRecord]", sessionId,
            List("RecordID" -> timetableEmployeeId, "PropertyCode" -> "IsFERActivated", "Value" -> valStr))
        }
      }
    }
  }

 }
