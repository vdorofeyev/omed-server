package omed.fer.PostLocation

import omed.db.{DB, DataAccessSupport, ConnectionProvider}
import xml.{Text, TopScope, Null, Elem}

/**
  *
  */
class PostLocationsDataProvider(connectionProvider: ConnectionProvider) extends DataAccessSupport {

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

  val TimetableInfoQuery =
    """
      |select top 1 e.LastName,
      | e.FirstName,
      | e.SecondName,
      | cab.CabinetNum,
      | isnull(cs.IDFER, '4f882b982bcfa5145a00036d') as SpecialtyIDFER,
      | ts.Duration,
      | '4f8805b52bcfa52299000011' as ReservationTypeIDFER
      |from TimeTableEmployee TTE
      |inner join Employee e on e.id = TTE.EmployeeID
      |left join Sertificate s
      | on s.EmployeeID = e.ID
      |--  and s.ПризнакОсновной = 'Y'
      |left join Specialty cs
      | on cs.id = s.specialtyid
      |inner join EmployeeTimetableTemplate et
      | on et.ID = TTE.EmployeeTimetableTemplateID
      |inner join TimeSlotClassificator ts
      | on et.TimeSlotClassificatorID = ts.ID
      |inner join Cabinet cab
      | on et.CabinetID = cab.ID
      |where TTE.ID = ?
    """.stripMargin

  val TimetableEmployeesQuery =
    """
      |select te.ID, te.EmployeeID
      |from TimeTableEmployee te
      |where TimeTableManagementID = ?
    """.stripMargin

  val TimeTableFinancialSourceQuery =
    """
      |select fs.IDFER
      |from TimeTableManagement tm
      |inner join FinancialSource fs
      |on tm.FinancialSourceID = fs.ID
      |where tm.ID = ?
    """.stripMargin


  val ServiceTypesQuery =
    """
      |select p.IDFER as PriceListIDFER
      |from Sertificate s
      |inner join PriceListSpeciality pls
      |on pls.SpecialityID = s.SpecialtyID
      |inner join PriceList p
      |on p.ID = pls.PriceListID
      |where s.EmployeeID = ?
    """.stripMargin

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

  def getTimetableEmployees(timetableId: String) = {
    val dbResult = connectionProvider withConnection {
      connection =>
        dataOperation {
          val statement = connection.prepareStatement(TimetableEmployeesQuery)
          statement.setString(1, timetableId)
          statement.executeQuery()
        }
    }

    val employeeList = scala.collection.mutable.ArrayBuffer[(String, String)]()

    while (dbResult.next()) {
      val ID = dbResult.getString("ID")
      val employeeID = dbResult.getString("EmployeeID")
      employeeList += (ID -> employeeID)
    }

    employeeList.toSeq
  }



  def getServiceTypesForEmployee(employeeId: String) = {

    val dbResult = connectionProvider withConnection {
      connection =>
        dataOperation {
          val statement = connection.prepareStatement(ServiceTypesQuery)
          statement.setString(1, employeeId)
          statement.executeQuery()
        }
    }

    val employeeList = scala.collection.mutable.ArrayBuffer[String]()

    while (dbResult.next()) {
      val employeeID = dbResult.getString("PriceListIDFER")
      employeeList += employeeID
    }

    employeeList.toSeq
  }

  def getTimetableInfoForEmployee(timetableEmployeeId: String, employeeId: String) = {
    val serviceTypes = getServiceTypesForEmployee(employeeId)

    val dbResult = connectionProvider withConnection {
      connection =>
        dataOperation {
          val statement = connection.prepareStatement(TimetableInfoQuery)
          statement.setString(1, timetableEmployeeId)
          statement.executeQuery()
        }
    }

    if (dbResult.next()) {
      val firstName = dbResult.getString("FirstName")
      val lastName = dbResult.getString("LastName")
      val secondName = dbResult.getString("SecondName")
      val cabinet = dbResult.getString("CabinetNum")
      val specialty = dbResult.getString("SpecialtyIDFER")
      val duration = dbResult.getInt("Duration")
      val reservationType = dbResult.getString("ReservationTypeIDFER")

      val name = Option(lastName).getOrElse("") +
        Option(firstName).map(" " + _).getOrElse("") +
        Option(secondName).map(" " + _).getOrElse("")

      PostLocationsMessage.TimetableEmployeeInfo(
        timetableEmployeeId, name, cabinet, specialty,
        duration,reservationType, serviceTypes)
    } else null
  }

  def getTimetableFinancialSource(timetableId: String) = {
    val dbResult = connectionProvider withConnection {
      connection =>
        dataOperation {
          val statement = connection.prepareStatement(TimeTableFinancialSourceQuery)
          statement.setString(1, timetableId)
          statement.executeQuery()
        }
    }

    val financialSource = if (dbResult.next()) {
      dbResult.getString("IDFER")
    } else null

    financialSource
  }

  def saveTimetableInfoIdFromFER(sessionId: String, timetableEmployeeId: String, ferId: String, ferName: String) = {
    connectionProvider withConnection {
      connection => {
        dataOperation {
          DB.dbExecNoResultSet(connection, "[_Object].[EditRecord]", sessionId,
            List("RecordID" -> timetableEmployeeId, "PropertyCode" -> "IDFER", "Value" -> ferId))
        }
        dataOperation {
          DB.dbExecNoResultSet(connection, "[_Object].[EditRecord]", sessionId,
            List("RecordID" -> timetableEmployeeId, "PropertyCode" -> "IDFERName", "Value" -> ferName))
        }
      }
    }
  }

 }
