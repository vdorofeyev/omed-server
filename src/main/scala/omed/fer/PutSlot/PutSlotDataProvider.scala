package omed.fer.PutSlot

import omed.db.{DB, DataAccessSupport, ConnectionProvider}
import java.util.Date

/**
 *
 */
class PutSlotDataProvider(connectionProvider: ConnectionProvider) extends DataAccessSupport {

  case class SlaveTimeTableInfo(firstName: String, secondName: String, lastName: String,
                                phone: String, patientId: String, idFER: String)

  private val SlaveTimeTableQuery =
    """
      |select st.IDFERSlot, p.FirstName, p.SecondName, p.LastName, p.Phone, st.PatientID
      |from SlaveTimeTable st
      |join Patient p on st.PatientID = p.ID
      |where st.ID = ?
    """.stripMargin

  def getSlaveTimeTable(timetableId: String) = {

    val dbResult = connectionProvider withConnection {
      connection =>
        dataOperation {
          val statement = connection.prepareStatement(SlaveTimeTableQuery)
          statement.setString(1, timetableId)
       //   val select =statement.
          statement.executeQuery()
        }
    }

    val timetable = if (dbResult.next()) {
      val idFER = dbResult.getString("IDFERSlot")
      val firstName = dbResult.getString("FirstName")
      val secondName = dbResult.getString("SecondName")
      val lastName = dbResult.getString("LastName")
      val phone = dbResult.getString("Phone")
      val patientId = dbResult.getString("PatientID")

      SlaveTimeTableInfo(firstName, secondName, lastName, phone, patientId, idFER)
    } else null

    timetable
  }


  def saveSlotIdFromFER(sessionId: String, timetableId: String, success: Boolean) = {
    connectionProvider withConnection {
      connection => {
        dataOperation {
          DB.dbExecNoResultSet(connection, "[_Object].[EditRecord]", sessionId,
            List("RecordID" -> timetableId, "PropertyCode" -> "IsFERSlotTaken", "Value" -> {if (success) "Y" else "N"} ))
        }
      }
    }
  }

}
