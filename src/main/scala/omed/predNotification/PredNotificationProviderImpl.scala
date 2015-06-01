package omed.predNotification

import omed.model.{SimpleValue, MetaClassProvider, EntityInstance, DataType}
import com.google.inject.Inject
import omed.data.{DataReaderService, DataWriterService}
import java.util.{TimeZone, Calendar}
import ru.atmed.omed.beans.model.meta.MetaObjectStatus
import omed.db.ConnectionProvider
import omed.cache.ExecStatProvider
import omed.system.ContextProvider
import omed.bf.BusinessFunctionExecutor

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 18.12.13
 * Time: 16:53
 * To change this template use File | Settings | File Templates.
 */
class PredNotificationProviderImpl extends PredNotificationProvider{

  @Inject
  var metaClassProvider:MetaClassProvider = null
  @Inject
  var dataWriterService:DataWriterService = null
  @Inject
  var connectionProvider: ConnectionProvider = null
  @Inject
  var execStatProvider : ExecStatProvider = null
  @Inject
  var contextProvider : ContextProvider = null
  @Inject
  var businessFunctionExecutor :BusinessFunctionExecutor = null
  val predNotificationQuery =
    """
      |select ID,ObjectID,MaxTransitionTime from PredNotification where
      |	  ObjectID = ? and
      |	 _StatusID = 'B8D05562-6AC1-4259-85CB-96A9080E1FF5'
    """.stripMargin
  def createPredNotificationsForObject(entity :EntityInstance){
    val status = metaClassProvider.getStatusDescription(entity.getStatusId)
    if(status.isEmpty) return
    status.get.predNotificationDesriptions.foreach( f => {
      val recordId = dataWriterService.addRecord("E3B97C91-A32A-453F-8CE1-1E3555083C59")
      val cal = Calendar.getInstance()
      cal.setTimeZone(TimeZone.getTimeZone("GMT+0"))
      cal.add(Calendar.MINUTE,f.maxTransitionTime)
      val format = new java.text.SimpleDateFormat(DataType.DateTimeFormat)
      val maxProcessingTime =  format.format(cal.getTime)
      dataWriterService.editRecord(recordId,Map("ObjectID"->entity.getId,"PredNotificationDescriptionID"->f.id,"MaxTransitionTime"->maxProcessingTime,"ObjectClassID"->entity.data("_ClassID").toString,"NotificationGroupID"->f.notificationGroupId))
    })
  }
  def updatePredNotificationsForObject(entity :EntityInstance){
    val status = metaClassProvider.getStatusDescription(Option(entity.data("_StatusID")).map(f => f.toString).getOrElse(null))
    if(status.isEmpty || status.get.predNotificationDesriptions.length ==0) return
    connectionProvider.withConnection {
      connection =>
        var statement = connection.prepareStatement(predNotificationQuery)
        statement.setString(1,entity.getId)
        var dbResult =  statement.executeQuery()
        val cal = Calendar.getInstance()
        cal.setTimeZone(TimeZone.getTimeZone("GMT+0"))
        val now = cal.getTime
  while (dbResult.next()){
    val date = dbResult.getObject("MaxTransitionTime").asInstanceOf[java.sql.Timestamp]
    val objectId = dbResult.getString("ObjectID")
    val id = dbResult.getString("ID")
    // если время больше максимального то переводим предуведомление в статус выполнено с нарушениями иначе удаляем
    if (now.after(date)) businessFunctionExecutor.initFunctionInstance("FBAF473B-8BA8-4228-AABB-D99F81B01972",Map("this"->SimpleValue(id)))
    else  dataWriterService.deleteRecord(id)
  }
}
}
}
