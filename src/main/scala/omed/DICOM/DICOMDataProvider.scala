package omed.dicom

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 13.08.13
 * Time: 12:33
 * To change this template use File | Settings | File Templates.
 */

import omed.db.{DataAccessSupport, ConnectionProvider}

class DICOMDataProvider(connectionProvider:ConnectionProvider) extends DataAccessSupport{
  private val AllModalityQuery="select ID,AETitle,IPAddress from Modality"
  private val StudyQuery="""select ID from Patient where ID= ?""".stripMargin
  def getAllModalities:Seq[DICOMModality]={
    val dbResult = connectionProvider withConnection {
      connection =>
        dataOperation {
          val statement = connection.prepareStatement(AllModalityQuery)
          statement.executeQuery()
        }
    }
    val modalities = scala.collection.mutable.Buffer[DICOMModality]()
    while(dbResult.next()) {
      modalities+= DICOMModality(dbResult.getString("ID"),dbResult.getString("AETitle"),dbResult.getString("IPAddress"))//dbResult.getString("IPAddress"))
    }
    modalities.toSeq
  }
  def getStudyDescription(studyId:String):Option[DICOMStudy]={
      val dbResult = connectionProvider withConnection{
        connection=>
          dataOperation{
            val statement = connection.prepareStatement(StudyQuery)
            statement.setString(1,studyId)
            statement.executeQuery()
          }
      }
    if(dbResult.next())
       Option(DICOMStudy(dbResult.getString("ID"),dbResult.getString("AccessionNumber")))
    else None
  }
}
