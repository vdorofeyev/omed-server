package omed.fer.PostReserve

import omed.db.{DB, DataAccessSupport, ConnectionProvider}
import java.util.Date
import java.sql.Time

/**
 *
 */
class PostReserveDataProvider(connectionProvider: ConnectionProvider) extends DataAccessSupport {

  case class SlaveTimeTableInfo(timetableEmployeeId: String, idFER: String, beginDate: Date, beginTime:Time, specialtyId: String)

  private val SlaveTimeTableQuery =
    """
      |select top 1 st.ID, te.IDFER, st.TimeSlotBegin, st.SpecialtyID
      |from SlaveTimeTable st
      |join MasterTimeTable mt on st.MasterTimeTableID = mt.ID
      |join TimeTableManagement tm on mt.TimeTableManagementID = tm.ID
      |join TimeTableEmployee te on te.TimeTableManagementID = tm.ID and te.EmployeeID = st.EmployeeID
      |where te.IsFERActivated = 'Y' and st.ID = ?
    """.stripMargin

  private val ServiceTypesQuery =
    """
      |select top 1 p.IDFER as PriceListIDFER
      |from PriceListSpeciality pls
      |join PriceList p
      |on p.ID = pls.PriceListID
      |where p.IDFER is not null and pls.SpecialityID = ?
    """.stripMargin


  def getSlaveTimeTable(timetableId: String) = {

    val dbResult = connectionProvider withConnection {
      connection =>
        dataOperation {
          val statement = connection.prepareStatement(SlaveTimeTableQuery)
          statement.setString(1, timetableId)
          statement.executeQuery()
        }
    }

    val timetable = if (dbResult.next()) {
      val id = dbResult.getString("ID")
      val idFER = dbResult.getString("IDFER")
      val dateBegin = dbResult.getDate("TimeSlotBegin")
      val specialtyId = dbResult.getString("SpecialtyID")
      val timeBegin =  dbResult.getTime("TimeSlotBegin")
      SlaveTimeTableInfo(id, idFER, dateBegin,timeBegin, specialtyId)
    } else null

    timetable
  }

  def getServiceType(specialtyId: String) = {

    val dbResult = connectionProvider withConnection {
      connection =>
        dataOperation {
          val statement = connection.prepareStatement(ServiceTypesQuery)
          statement.setString(1, specialtyId)
          statement.executeQuery()
        }
    }

    val serviceTypeId = if (dbResult.next()) {
      dbResult.getString("PriceListIDFER")
    } else null

    serviceTypeId
  }


  def saveSlotIdFromFER(sessionId: String, timetableId: String, idFER: String) = {
    connectionProvider withConnection {
      connection => {
        dataOperation {
          DB.dbExecNoResultSet(connection, "[_Object].[EditRecord]", sessionId,
            List("RecordID" -> timetableId, "PropertyCode" -> "IDFERSlot", "Value" -> idFER))
        }
      }
    }
  }

}
