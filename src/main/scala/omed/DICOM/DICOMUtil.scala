package omed.dicom

import omed.db.{DataAccessSupport ,ConnectionProvider}
import java.net.{HttpURLConnection, URL}
import javax.xml.ws.Service
import java.io.{InputStreamReader, BufferedReader, DataOutputStream}
import scala.xml.Elem

object DICOMUtil{
  def apply(connectionProvider:ConnectionProvider,dicomIPAddress:String)={
    new DICOMUtil(connectionProvider,dicomIPAddress)
  }
}
class DICOMUtil(connectionProvider:ConnectionProvider, dicomIPAddress:String) extends  DataAccessSupport{
  val dicomDataProvider = new DICOMDataProvider(connectionProvider)
  def postModalities(){
   // val dicomDataProvider = new DICOMDataProvider(connectionProvider)
    val modalities = dicomDataProvider.getAllModalities
    val message = <list>{
     modalities.map(f => <modality><AETitle>{f.AETitle}</AETitle><comment>comment</comment><IP>{f.ip}</IP></modality>)     //
      } </list>
    val url =dicomIPAddress+ "/Mvc2WL/Modality/create"
    val (code,result) = sendPostRequest(url,message.toString())
    val t = 5
 }
  def postStudyToWorklist(studyId:String){
      val study = dicomDataProvider.getStudyDescription(studyId)
     val message = if(study.isDefined) study.get.xml else null
  }
  def sendPostRequest(urlStr:String,body:String):(Int,String)={
    val url = new URL(urlStr)
    val conn = url.openConnection().asInstanceOf[HttpURLConnection]
    conn.setRequestMethod("POST")
    conn.setDoOutput(true)
    conn.connect()
    val wr = new DataOutputStream(conn.getOutputStream())
    wr.writeBytes(body)
    wr.flush()
    wr.close()

    (conn.getResponseCode,scala.io.Source.fromInputStream(conn.getInputStream()).getLines().mkString("\n"))
  }
}


