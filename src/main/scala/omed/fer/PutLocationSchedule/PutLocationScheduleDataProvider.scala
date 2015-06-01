package omed.fer.PutLocationSchedule

import omed.db.{DB, DataAccessSupport, ConnectionProvider}

/**
  *
  */
class PutLocationScheduleDataProvider(connectionProvider: ConnectionProvider) extends DataAccessSupport {

  case class TimetableEmployeeInfo(id: String, employeeId: String, employeeIDFER: String,
                                   evenTimetableIDFER: String, oddTimetableIDFER: String)

  val TimetableEmployeesQuery =
    """
      |select te.ID, te.EmployeeID, te.IDFER as EmployeeIDFER, et.IDFEREven as EvenTimetableIDFER, et.IDFEROdd as OddTimetableIDFER
      |from TimeTableEmployee te
      |inner join EmployeeTimetableTemplate et
      |on te.EmployeeTimetableTemplateID = et.ID
      |where (te.IDFER is not null)
      |and ((et.IDFEREven is not null) or (et.IDFEROdd is not null))
      |and te.TimeTableManagementID = ?
    """.stripMargin
    ////on te.EmployeeID = et. EmployeeID
  val TimetableIntervalQuery =
    """
      |select tm.IntervalBegin, tm.IntervalEnd
      |from TimeTableManagement tm
      |where tm.ID = ?
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
      val evenTimetableIDFER = dbResult.getString("EvenTimetableIDFER")
      val oddTimetableIDFER = dbResult.getString("OddTimetableIDFER")
      employeeList += TimetableEmployeeInfo(ID, employeeID, employeeIDFER, evenTimetableIDFER, oddTimetableIDFER)
    }

    employeeList.toSeq
  }

  def getTimetableInterval(timetableId: String) = {
    val dbResult = connectionProvider withConnection {
      connection =>
        dataOperation {
          val statement = connection.prepareStatement(TimetableIntervalQuery)
          statement.setString(1, timetableId)
          statement.executeQuery()
        }
    }

    val (startDate, endDate) = if (dbResult.next()) {
      val startDate = dbResult.getDate("IntervalBegin")
      val endDate = dbResult.getDate("IntervalEnd")
      (startDate, endDate)
    } else (null, null)

    (startDate, endDate)
  }

  def saveTimetablePutMark(sessionId: String, timetableEmployeeId: String, isSuccess: Boolean) = {
    connectionProvider withConnection {
      connection => {
        val valStr = if (isSuccess) "Y" else "N"
        dataOperation {
          DB.dbExecNoResultSet(connection, "[_Object].[EditRecord]", sessionId,
            List("RecordID" -> timetableEmployeeId, "PropertyCode" -> "IsFERPutLocationSchedule", "Value" -> valStr))
        }
      }
    }
  }

 }
