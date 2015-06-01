package omed.fer

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 21.10.13
 * Time: 16:24
 * To change this template use File | Settings | File Templates.
 */
class HL7PatientRecordMessage (val lastName:String,val firstName:String, val secondName:String,
                                val date:String, val timeSlot:String,val locationId:String,var timeSlotId :String = null) {

}

object HL7PatientRecordMessage{
  def apply(message:String): HL7PatientRecordMessage={
      val lines = message.split('\r')
      def findString(prefix:String):String={
        lines.find(p=>p.startsWith(prefix)).getOrElse(null)
      }
      val fioLine = findString("PID").split(Seq('|','^').toArray)
      val lastName = fioLine(5)
      val firstName=fioLine(6)
      val secondName=fioLine(7)
      if(lastName=="") return null
      val dateLine =   findString("ARQ").split(Seq('|','^').toArray)
      val date = dateLine(12).substring(0,4)+'-' +  dateLine(12).substring(4,6) + '-'+dateLine(12).substring(6,8)
      val time =  dateLine(12).substring(8,10)+':' +  dateLine(12).substring(10,12)
      val locationLine =   findString("AIP").split(Seq('|','^').toArray)
      val locationId = locationLine(3)
      new HL7PatientRecordMessage(lastName,firstName,secondName,date,time,locationId)
  }
}