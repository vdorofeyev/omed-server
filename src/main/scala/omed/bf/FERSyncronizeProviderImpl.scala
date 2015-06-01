package omed.bf

import com.google.inject.Inject
import omed.data.DataWriterService
import omed.db.{DB, DataAccessSupport, ConnectionProvider}
import omed.fer.HL7PatientRecordMessage

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 02.10.13
 * Time: 18:05
 * To change this template use File | Settings | File Templates.
 */
class FERSyncronizeProviderImpl extends FERSyncronizeProvider  with DataAccessSupport{

   @Inject
  var dataWriterService :DataWriterService = null

  @Inject
  var connectionProvider :ConnectionProvider = null

  val slotQuery =
    """
      |select STT.ID,STT.timeSlotStr from SlaveTimeTable STT
      |inner join TimeTableEmployee TTE
      |on TTE.employeeID  = STT.employeeID
      |where TTE.IDFER = ?
      |and STT.dateTimeTable = ?
      |and STT._StatusID !='E72116B2-E6BC-4043-9B45-C56FEE42D30B'
    """.stripMargin
  val patientQuery =
    """
      |select ID from Patient
      |where Lastname = ?
      |and Firstname = ?
      |and SecondName = ?
    """.stripMargin
       //and substring(STT.timeslotStr,1,5) in (?)
  def addFerSlots(slots:Map[String,Map[String,Boolean]],date:String){
      slots.foreach(p=> {
          val slotIds =  getSlot(p._1,p._2.filter(p => p._2).map(f=> f._1).toSet,date)
          slotIds.foreach(addSlot)
      })
  }
  def addRecord(hl7Message:HL7PatientRecordMessage){
  //  val slot = getSlot(hl7Message.locationId,Set(hl7Message.timeSlot),hl7Message.date)
  //  if(slot.length!=1) return
    val recordId = getPatient(hl7Message.lastName,hl7Message.firstName,hl7Message.secondName)
    addSlot(hl7Message.timeSlotId)
    dataWriterService.directSaveField("4DD3BAE6-8486-4CB2-928A-EAF718C843FB",hl7Message.timeSlotId,"PatientID",recordId)
  }
  private def getPatient (lastName:String,firstName:String,secondName:String) :String={
    val dbResult = connectionProvider withConnection {
      connection =>
        dataOperation {
          val statement = connection.prepareStatement(patientQuery)
          statement.setString(1, lastName)
          statement.setString(2, firstName)
          statement.setString(3, secondName)
          statement.executeQuery()
        }
    }
    val rowCount = try {
      dbResult.last()
      val size = dbResult.getRow()
      dbResult.beforeFirst()
      size
    }
    catch {
      case _ => 0
    }
    // если запись ровно одна то берем этого пациента, иначе создаем нового
    if( dbResult.next()) {
      val recordId =  dbResult.getString("ID")
      if(!dbResult.next()) return recordId
    }
    val record = dataWriterService.addRecord("A4E58C1C-E849-4A4A-8C79-A1B58AAD75FF")
    dataWriterService.editRecord(record,Map("LastName"->lastName,"FirstName"->firstName,"SecondName"->secondName))
    record.getId
  }
   def getSlot(locationId:String, times: Set[String],date:String) :Seq[String]={
    val dbResult = connectionProvider withConnection {
      connection =>
        dataOperation {
          val statement = connection.prepareStatement(slotQuery)
          statement.setString(1, locationId)
          statement.setString(2, date)
          statement.executeQuery()
        }
    }
    val slots = scala.collection.mutable.ArrayBuffer[String]()
    while (dbResult.next()) {
      val id = dbResult.getString("ID")
      val timeSlotStr = Option(dbResult.getString("timeslotstr")).map(f=> f.substring(0,5)).getOrElse(null)
      if(times.contains(timeSlotStr)) slots+=id
    }
    slots.toSeq
  }
  private def addSlot(id:String){
     dataWriterService.directSaveField("4DD3BAE6-8486-4CB2-928A-EAF718C843FB",id,"_StatusID","E72116B2-E6BC-4043-9B45-C56FEE42D30B")
  }
}
