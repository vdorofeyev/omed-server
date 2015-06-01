package omed.fer.PostRules

import omed.db.{DB, DataAccessSupport, ConnectionProvider}

/**
 *
 */
class PostRulesDataProvider(connectionProvider: ConnectionProvider) extends DataAccessSupport {

  private val TimetableQuery =
    """
      |with
      |WorkTimeDictionary as (
      |	select c.*, case when Name = '-' then null else Name end TimeString from WorkTimeClassificator c
      |),
      |-- Even weeks begin work time
      |EvenWorkBeginTimeIdTable as (
      |	select ID, EmployeeID, OrganizationUnitID, cast(DayNum as integer) as DayNum, TimeID from (
      |		select
      |			ID,
      |			EmployeeID,
      |			OrganizationUnitID,
      |			MondayEvenWorkBeginTimeID as [1],
      |			TuesdayEvenWorkBeginTimeID as [2],
      |			WednesdayEvenWorkBeginTimeID as [3],
      |			ThursdayEvenWorkBeginTimeID as [4],
      |			FridayEvenWorkBeginTimeID as [5],
      |			SaturdayEvenWorkBeginTimeID as [6],
      |			SundayEvenWorkBeginTimeID as [7]
      |		from EmployeeTimetableTemplate.DataAll
      |	) d unpivot (
      |		TimeID for DayNum in (
      |			[1], [2], [3], [4], [5], [6], [7]
      |		)
      |	) as upvt
      |), EvenWorkBeginTimeTable as (
      |	select t.ID, t.EmployeeID, t.OrganizationUnitID, t.DayNum, w.TimeString
      |	from EvenWorkBeginTimeIdTable t
      |	left join WorkTimeDictionary w
      |	on t.TimeID = w.id
      |),
      |-- Even weeks end work time
      |EvenWorkEndTimeIdTable as (
      |	select ID, EmployeeID, OrganizationUnitID, cast(DayNum as integer) as DayNum, TimeID from (
      |		select
      |			ID,
      |			EmployeeID,
      |			OrganizationUnitID,
      |			MondayEvenWorkEndTimeID as [1],
      |			TuesdayEvenWorkEndTimeID as [2],
      |			WednesdayEvenWorkEndTimeID as [3],
      |			ThursdayEvenWorkEndTimeID as [4],
      |			FridayEvenWorkEndTimeID as [5],
      |			SaturdayEvenWorkEndTimeID as [6],
      |			SundayEvenWorkEndTimeID as [7]
      |		from EmployeeTimetableTemplate.DataAll
      |	) d unpivot (
      |		TimeID for DayNum in (
      |			[1], [2], [3], [4], [5], [6], [7]
      |		)
      |	) as upvt
      |), EvenWorkEndTimeTable as (
      |	select t.ID, t.EmployeeID, t.OrganizationUnitID, t.DayNum, w.TimeString
      |	from EvenWorkEndTimeIdTable t
      |	left join WorkTimeDictionary w
      |	on t.TimeID = w.id
      |),
      |-- Even weeks begin break time
      |EvenBreakBeginTimeIdTable as (
      |	select ID, EmployeeID, OrganizationUnitID, cast(DayNum as integer) as DayNum, TimeID from (
      |		select
      |			ID,
      |			EmployeeID,
      |			OrganizationUnitID,
      |			MondayEvenBreakBeginTimeID as [1],
      |			TuesdayEvenBreakBeginTimeID as [2],
      |			WednesdayEvenBreakBeginTimeID as [3],
      |			ThursdayEvenBreakBeginTimeID as [4],
      |			FridayEvenBreakBeginTimeID as [5],
      |			SaturdayEvenBreakBeginTimeID as [6],
      |			SundayEvenBreakBeginTimeID as [7]
      |		from EmployeeTimetableTemplate.DataAll
      |	) d unpivot (
      |		TimeID for DayNum in (
      |			[1], [2], [3], [4], [5], [6], [7]
      |		)
      |	) as upvt
      |), EvenBreakBeginTimeTable as (
      |	select t.ID, t.EmployeeID, t.OrganizationUnitID, t.DayNum, w.TimeString
      |	from EvenBreakBeginTimeIdTable t
      |	left join WorkTimeDictionary w
      |	on t.TimeID = w.id
      |),
      |-- Even weeks end break time
      |EvenBreakEndTimeIdTable as (
      |	select ID, EmployeeID, OrganizationUnitID, cast(DayNum as integer) as DayNum, TimeID from (
      |		select
      |			ID,
      |			EmployeeID,
      |			OrganizationUnitID,
      |			MondayEvenBreakEndTimeID as [1],
      |			TuesdayEvenBreakEndTimeID as [2],
      |			WednesdayEvenBreakEndTimeID as [3],
      |			ThursdayEvenBreakEndTimeID as [4],
      |			FridayEvenBreakEndTimeID as [5],
      |			SaturdayEvenBreakEndTimeID as [6],
      |			SundayEvenBreakEndTimeID as [7]
      |		from EmployeeTimetableTemplate.DataAll
      |	) d unpivot (
      |		TimeID for DayNum in (
      |			[1], [2], [3], [4], [5], [6], [7]
      |		)
      |	) as upvt
      |), EvenBreakEndTimeTable as (
      |	select t.ID, t.EmployeeID, t.OrganizationUnitID, t.DayNum, w.TimeString
      |	from EvenBreakEndTimeIdTable t
      |	left join WorkTimeDictionary w
      |	on t.TimeID = w.id
      |)
      |select wb.DayNum,
      |	wb.TimeString as WorkBeginTime,
      |	bb.TimeString as BreakBeginTime,
      |	be.TimeString as BreakEndTime,
      |	we.TimeString as WorkEndTime
      |from EvenWorkBeginTimeTable wb
      |inner join EvenBreakBeginTimeTable bb
      |on wb.ID = bb.ID and wb.EmployeeID = bb.EmployeeID and wb.DayNum = bb.DayNum
      |inner join EvenBreakEndTimeTable be
      |on bb.ID = be.ID and bb.EmployeeID = be.EmployeeID and bb.DayNum = be.DayNum
      |inner join EvenWorkEndTimeTable we
      |on be.ID = we.ID and be.EmployeeID = we.EmployeeID and be.DayNum = we.DayNum
      |where wb.ID = ?
    """.stripMargin

  val EmployeeNameQuery =
    """
      |select e.FirstName, e.LastName, e.SecondName
      |from Employee.DataAll e
      |inner join EmployeeTimetableTemplate.DataAll t
      |on e.ID = t.EmployeeID
      |where t.ID = ?
    """.stripMargin

  val PlaceIdQuery =
    """
      |select HCU.IDFER
      |from HCU
      |join _domain.data d
      |on d.RootOUID = hcu.id
      |join _session s
      |on s._domain = d.number
      |where s.id = ?
    """.stripMargin

  private def getTimeTable(query: String, timetableId: String) = {

    val dbResult = connectionProvider withConnection {
      connection =>
        dataOperation {
          val statement = connection.prepareStatement(query)
          statement.setString(1, timetableId)
          statement.executeQuery()
        }
    }

    val timetable = scala.collection.mutable.ArrayBuffer[PostRulesMessage.DailyTimetable]()

    while (dbResult.next()) {
      val dayNumber = dbResult.getInt("DayNum")
      val workBeginTime = dbResult.getString("WorkBeginTime")
      val breakBeginTime = dbResult.getString("BreakBeginTime")
      val breakEndTime = dbResult.getString("BreakEndTime")
      val workEndTime = dbResult.getString("WorkEndTime")

      val dailyTimetable = PostRulesMessage.DailyTimetable(dayNumber,
        workBeginTime, breakBeginTime, breakEndTime, workEndTime)

      timetable += dailyTimetable
    }

    timetable.toSeq
  }

  def getEvenTimeTable(timetableId: String) = {
    getTimeTable(TimetableQuery, timetableId)
  }

  def getOddTimeTable(timetableId: String) = {
    val oddTimetableQuery = TimetableQuery.replaceAll("Even", "Odd")
    getTimeTable(oddTimetableQuery, timetableId)
  }

  def getEmployeeName(timetableId: String) = {
    val dbResult = connectionProvider withConnection {
      connection =>
        dataOperation {
          val statement = connection.prepareStatement(EmployeeNameQuery)
          statement.setString(1, timetableId)
          statement.executeQuery()
        }
    }

    val name = if (dbResult.next()) {
      val firstName = dbResult.getString("FirstName")
      val lastName = dbResult.getString("LastName")
      val secondName = dbResult.getString("SecondName")

      Option(lastName).getOrElse("") +
        Option(firstName).map(" " + _).getOrElse("") +
        Option(secondName).map(" " + _).getOrElse("")
    } else null

    name
  }

  def getPlaceID(sessionId: String) = {
    val dbResult = connectionProvider withConnection {
      connection =>
        dataOperation {
          val statement = connection.prepareStatement(PlaceIdQuery)
          statement.setString(1, sessionId)
          statement.executeQuery()
        }
    }

    val idFER = if (dbResult.next()) {
      dbResult.getString("IDFER")
    } else null

    idFER
  }

  def saveTimetableIdFromFER(sessionId: String, timetableId: String,
                             evenId: String, oddId: String) = {
    connectionProvider withConnection {
      connection => {
        dataOperation {
          DB.dbExecNoResultSet(connection, "[_Object].[EditRecord]", sessionId,
            List("RecordID" -> timetableId, "PropertyCode" -> "IDFEREven", "Value" -> evenId))
        }
        dataOperation {
          DB.dbExecNoResultSet(connection, "[_Object].[EditRecord]", sessionId,
            List("RecordID" -> timetableId, "PropertyCode" -> "IDFEROdd", "Value" -> oddId))
        }
      }
    }
  }

}
