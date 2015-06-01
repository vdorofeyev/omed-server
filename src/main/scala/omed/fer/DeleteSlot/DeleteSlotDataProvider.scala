package omed.fer.DeleteSlot

import omed.db.{DB, DataAccessSupport, ConnectionProvider}

/**
 *
 */
class DeleteSlotDataProvider(connectionProvider: ConnectionProvider) extends DataAccessSupport {

  private val SlaveTimeTableQuery =
    """
      |select st.IDFERSlot
      |from SlaveTimeTable st
      |where st.ID = ?
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
      val idFER = dbResult.getString("IDFERSlot")
      idFER
    } else null

    timetable
  }


  def unbindSlotIdFromFER(sessionId: String, timetableId: String) = {
    connectionProvider withConnection {
      connection => {
        dataOperation {
          DB.dbExecNoResultSet(connection, "[_Object].[EditRecord]", sessionId,
            List("RecordID" -> timetableId, "PropertyCode" -> "IsFERSlotTaken", "Value" -> "N" ))
        }

        dataOperation {
          DB.dbExecNoResultSet(connection, "[_Object].[EditRecord]", sessionId,
            List("RecordID" -> timetableId, "PropertyCode" -> "PatientID", "Value" -> { null } ))
        }
      }
    }
  }

}
