package omed.fer

import omed.db.{DB, DataAccessSupport, ConnectionProvider}

import omed.errors.DataError
import omed.fer.PostRules._
import omed.fer.PostReserve._
import omed.fer.PostLocation._
import omed.fer.PutActivateLocation._
import omed.fer.PutLocationSchedule._
import omed.fer.PutSlot._
import omed.fer.GetLocations._
import omed.fer.GetReserve._
import omed.fer.GetTimes._
import omed.fer.DeleteSlot._



object TimetableUtil {

  def apply(connectionProvider: ConnectionProvider, ferAddress: String, ferToken: String) = 
    new TimetableUtil(connectionProvider, ferAddress, ferToken)

}

class TimetableUtil(connectionProvider: ConnectionProvider, serviceWsdlAddress: String, authToken: String)
  extends DataAccessSupport {

  def getLocations (sessionId: String,placeId:String = null) :Seq[String]={
    //от провайдера нужен только метод получения placeId
    val dataProvider = new PostRulesDataProvider(connectionProvider)
    val updatedPlaceId = if(placeId!=null) placeId else  dataProvider.getPlaceID(sessionId)
    val message = GetLocationsMessage(updatedPlaceId,authToken)
    val service = new FERService(serviceWsdlAddress)
    val reply = GetLocationsReply(service.sendMessage(message))
    reply.checkError
    reply.locations
  }

  def getReserve(sessionId: String) {
    //от провайдера нужен только метод получения placeId
    val dataProvider = new PostRulesDataProvider(connectionProvider)
 //   val message = GetReserveMessage(dataProvider.getPlaceID(sessionId), authToken)
    val message = GetReserveMessage("4f880db92bcfa52772040c64", "wVGarppM8a6Vx8QhAqoC")
    val service = new FERService(serviceWsdlAddress)
    val reply = GetReserveReply(service.sendMessage(message))
    reply.checkError
  }

  def getTimes(locationId:String,date:String) ={
    val message = GetTimesMessage(locationId,authToken,date)
    val service = new FERService(serviceWsdlAddress)
    val reply = GetTimesReply(service.sendMessage(message))
    reply.checkError
    reply.slots
  }

  def postRules(sessionId: String, timetableId: String) {
    val dataProvider = new PostRulesDataProvider(connectionProvider)

    val evenTimetable =  dataProvider.getEvenTimeTable(timetableId)
    val oddTimetable = dataProvider.getOddTimeTable(timetableId)

    val employeeName = dataProvider.getEmployeeName(timetableId)
    val placeId = dataProvider.getPlaceID(sessionId)

    val evenTimetableName = employeeName + " Четные недели"
    val oddTimetableName = employeeName + " Нечетные недели"

    val evenTimetableMessage = PostRulesMessage(evenTimetableName, placeId, authToken, evenTimetable)
    val oddTimetableMessage = PostRulesMessage(oddTimetableName, placeId, authToken, oddTimetable)

    val service = new FERService(serviceWsdlAddress)
    var evenFERID: String = null
    var oddFERID: String = null
    if(evenTimetableMessage.isEmpty && oddTimetableMessage.isEmpty){
       throw new DataError("Не задано время работы", -2345)
    }
     if(!evenTimetableMessage.isEmpty){
       val result1 = PostRulesReply( service.sendMessage(evenTimetableMessage))
       evenFERID = result1.id
       result1.checkError
     }
      if(!oddTimetableMessage.isEmpty)  {
        val result2 = PostRulesReply(service.sendMessage(oddTimetableMessage))
        oddFERID=result2.id
        result2.checkError
      }
    dataProvider.saveTimetableIdFromFER(sessionId, timetableId,evenFERID, oddFERID)
  }

  def postLocations(sessionId: String, timetableId: String) {
    val dataProvider = new PostLocationsDataProvider(connectionProvider)

    val placeId = dataProvider.getPlaceID(sessionId)
    val financialSource = dataProvider.getTimetableFinancialSource(timetableId)
    val employees = dataProvider.getTimetableEmployees(timetableId)

    val timetableEmployees = employees.map(_ match {case (id, empId) => dataProvider.getTimetableInfoForEmployee(id, empId)})

    timetableEmployees.foreach(te => {
      if (te.cabinet == null || te.cabinet.trim == "")
        throw new DataError(String.format("Для врача %s не указан кабинет", te.name), -2345)
      if (te.serviceTypes.isEmpty)
        throw new DataError(String.format("В ФЭР нет услуг, связанных со специальностями врача %s", te.name), -2346)
    })

    val messages = timetableEmployees.map(te => PostLocationsMessage(
      placeId, financialSource, authToken, te))

    val service = new FERService(serviceWsdlAddress)

    val replies = messages.map(m => PostLocationsReply(service.sendMessage(m)))

    for {
      (emp, reply) <- (timetableEmployees zip replies)

      empId = emp.timetableEmployeeId
      ferId = reply.id
      ferName = reply.name

    } {
      reply.checkError
      dataProvider.saveTimetableInfoIdFromFER(sessionId, empId, ferId, ferName)
    }
  }

  def putLocationSchedule(sessionId: String, timetableId: String) {
    val dataProvider = new PutLocationScheduleDataProvider(connectionProvider)

    val timetableEmployees = dataProvider.getTimetableEmployees(timetableId)
    val (startDate, endDate) = dataProvider.getTimetableInterval(timetableId)
//    val messages = timetableEmployees.map(te => Seq(
//      te -> PutLocationScheduleMessage(te.employeeIDFER, authToken,
//        te.evenTimetableIDFER, startDate, endDate, true),
//      te -> PutLocationScheduleMessage(te.employeeIDFER, authToken,
//        te.oddTimetableIDFER, startDate, endDate, false)
//    )).flatten.filter(_ match {case (te, message) => message.ruleId != null})
    //у всех графиков один идентификатор расписания
     val employeeIDFER = timetableEmployees.last.employeeIDFER
    val templates = timetableEmployees.map(te=>Seq(LocationSchedulerParameter(te.evenTimetableIDFER,true),LocationSchedulerParameter(te.oddTimetableIDFER,false))).flatten.filter(p=>p.ruleId!=null).toSeq
    val message = PutLocationScheduleMessage(employeeIDFER,authToken,templates,startDate,endDate)
    val service = new FERService(serviceWsdlAddress)

//    val replies = messages.map(_ match {case (te, msg) => te -> PutLocationScheduleReply(service.sendMessage(msg))})
//
//    val groupedReplies = replies
//      .groupBy(_ match {case (te, r) => te})
//      .mapValues(_.forall(_ match {case (te, r) => r.locationId != null && r.ruleId != null}))
     val reply = PutLocationScheduleReply(service.sendMessage(message))
   val isSuccess = !reply.isError
    for {timetableEmployee <-timetableEmployees}  dataProvider.saveTimetablePutMark(sessionId, timetableEmployee.id,isSuccess)
//    for {
//      (emp, isSuccess) <- groupedReplies
//      empId = emp.id
//    } dataProvider.saveTimetablePutMark(sessionId, empId, isSuccess)
  }

  def putActivateLocation(sessionId: String, timetableId: String) {
    val dataProvider = new PutActivateLocationDataProvider(connectionProvider)

    val timetableEmployees = dataProvider.getTimetableEmployees(timetableId)

    val messages = timetableEmployees.map(te => PutActivateLocationMessage(te.employeeIDFER, authToken))

    val service = new FERService(serviceWsdlAddress)

    val replies = messages.map(m => PutActivateLocationReply(service.sendMessage(m)))

    for {
      (emp, reply) <- (timetableEmployees zip replies)
      empId = emp.id
      isSuccess = Option(reply.active).map(_.toLowerCase == "true").getOrElse(false)
    } {
       reply.checkError
       dataProvider.saveTimetablePutMark(sessionId, empId, isSuccess)
    }

  }

  def postReserve(sessionId: String, timetableId: String) {
    val dataProvider = new PostReserveDataProvider(connectionProvider)

    val timetableInfo = dataProvider.getSlaveTimeTable(timetableId)
    if (timetableInfo != null) {
      val serviceType = dataProvider.getServiceType(timetableInfo.specialtyId)

      val message = PostReserveMessage(timetableInfo.idFER, timetableInfo.beginDate,timetableInfo.beginTime, serviceType, authToken)

      val service = new FERService(serviceWsdlAddress)

      val reply = PostReserveReply(service.sendMessage(message))
      reply.checkError
      if (reply.reserved) {
        dataProvider.saveSlotIdFromFER(sessionId, timetableId, reply.uniqueKey)
      }
    }

  }

  def putSlot(sessionId: String, timetableId: String) {
    val dataProvider = new PutSlotDataProvider(connectionProvider)

    val timetableInfo = dataProvider.getSlaveTimeTable(timetableId)
    if(timetableInfo==null) throw new RuntimeException("Не удалось получинные данные таймслота для записи в ФЭР")
    import net.iharder.Base64

    def base64(m: String) = if (m != null && m != "") Base64.encodeBytes(m.getBytes("UTF-8")) else ""
    val message = PutSlotMessage(
      base64(timetableInfo.firstName), base64(timetableInfo.lastName), base64(timetableInfo.secondName),
      timetableInfo.phone, timetableInfo.patientId, timetableInfo.idFER, authToken)

    val service = new FERService(serviceWsdlAddress)

    val reply = PutSlotReply(service.sendMessage(message))
    reply.checkError
    dataProvider.saveSlotIdFromFER(sessionId, timetableId, reply.approved)
  }

  def deleteSlot(sessionId: String, timetableId: String) {
    val dataProvider = new DeleteSlotDataProvider(connectionProvider)

    val slotId = dataProvider.getSlaveTimeTable(timetableId)

    val message = DeleteSlotMessage(slotId, authToken)

    val service = new FERService(serviceWsdlAddress)

    val reply = PutSlotReply(service.sendMessage(message))
    reply.checkError
    dataProvider.unbindSlotIdFromFER(sessionId, timetableId)

  }

}
