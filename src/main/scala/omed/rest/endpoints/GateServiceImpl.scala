package omed.rest.endpoints

import javax.ws.rs._
import scala.Array
import javax.ws.rs.core.{Context, Response, MediaType}
import com.google.inject.Inject
import omed.bf.FERSyncronizeProvider
import omed.fer.HL7PatientRecordMessage
import omed.auth.Auth
import javax.servlet.http.HttpServletRequest
import omed.data.DataReaderService
import omed.rest.model2xml.Model2Xml
import omed.roi.ROIProvider

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 15.10.13
 * Time: 15:48
 * To change this template use File | Settings | File Templates.
 */
@Path("/gate")
class GateServiceImpl{

  @Inject
  var ferSyncronizeProvider:FERSyncronizeProvider = null
  @Inject
  var authProvider :Auth = null
  @Context
  var httpRequest: HttpServletRequest = null
  @Inject
  var dataProvider:DataReaderService = null

  @Inject
  var roiProvider:ROIProvider = null

  @Path("/fer")
  @POST
  def postFerData(ferData: String){       // ferData: String
    try{
     val session = httpRequest.getSession()
     val hl7Message = HL7PatientRecordMessage(ferData)
     val slot =ferSyncronizeProvider.getSlot(hl7Message.locationId,Set(hl7Message.timeSlot),hl7Message.date)
     if(slot.length!=1) return
     hl7Message.timeSlotId = slot(0)
    // получение домена в котором расположены данные
     val domainId  =  dataProvider.getObjectData("SlaveTimeTable", hl7Message.timeSlotId)("_Domain").asInstanceOf[Int]
     val sessionId= authProvider.createSystemSession(domainId)
     session.setAttribute("sessionId", sessionId)
     session.setAttribute("isSuperUser", true)
     ferSyncronizeProvider.addRecord(hl7Message )
    }
    finally {
      val session = httpRequest.getSession()
      authProvider.logout(session.getAttribute("sessionId").asInstanceOf[String])
    }
  }

  @POST
  @Path("/roi/import")
  @Consumes(Array(MediaType.APPLICATION_JSON))
  @Produces(Array(MediaType.APPLICATION_XML))
  def importFromRoi( inputXml: String):Response={
    roiProvider.importExpertisesFromRoi(inputXml)
    Response.status(Response.Status.OK)
      .entity(new Model2Xml().standardAnswerToXml(0, "Импорт успешно произведен."))
      .build()
  }
  @GET
  @Path("/roi/export")
  @Produces(Array(MediaType.APPLICATION_JSON))
  def exportToRoi(@QueryParam("begin") begin: String,
                  @QueryParam("end") end: String):Response={

    Response.status(Response.Status.OK)
      .entity(roiProvider.exportExpertisesToRoi(begin,end))
      .build()
  }
//  @GET
//  @Path("/roi/importShablon/")
//  @Produces(Array(MediaType.APPLICATION_JSON))
//  def importFromRoiShablon(@PathParam("begin") begin: String,
//                  @PathParam("end") end: String):Response={
//    Response.status(Response.Status.OK)
//      .entity(roiProvider.importShablon)
//      .build()
//  }
}
