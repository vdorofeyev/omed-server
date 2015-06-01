package omed.push

import omed.db.{DataAccessSupport, ConnectionProvider, DB}
import com.google.inject.Inject
import omed.cache.{DomainCacheService, CommonCacheService}
import omed.system.ContextProvider
import javax.ejb.EJB
import java.util.Date
import org.joda.time.DateTime
import omed.data.SettingsService
import omed.errors.DataError


/**
 * Created with IntelliJ IDEA.
 * User: SamoylovaTAl
 * Date: 05.08.13
 * Time: 11:53
 * To change this template use File | Settings | File Templates.
 */
class PushNotificationServiceImpl extends PushNotificationService with DataAccessSupport{

    @Inject
    var connectionProvider: ConnectionProvider = null

   @Inject
    var contextProvider:ContextProvider = null

    @Inject
    var commonCacheService:CommonCacheService = null

   @Inject
    var settingsService: SettingsService = null

    def getTimeUpdate = settingsService.getGlobalSettings("OMEDNotificationUpdateTime").map(_.strValue).getOrElse("60").toInt
    def getUserNotifications:(Seq[PushNotification],Int) = {
      val userId= contextProvider.getContext.userId
      if(userId==null) return (List(),0)
      val keyLastTimeUpdate = "lastTimeUpdate"
      val time = commonCacheService.get(classOf[UpdateTime],keyLastTimeUpdate)
      val updatePeriod = getTimeUpdate * 1000
      if(time==null || System.currentTimeMillis()-time.time>updatePeriod){

        commonCacheService.drop(classOf[PushNotificationSeq])
        val (notifications,counts) = loadNotifications
        notifications.groupBy(_.userId).foreach{case (key,value) =>commonCacheService.put(classOf[PushNotificationSeq],key,PushNotificationSeq(value)) }
        counts.foreach(f=>commonCacheService.put(classOf[PushNotificationCount],f.userId,f))
        commonCacheService.put(classOf[UpdateTime],keyLastTimeUpdate,UpdateTime(System.currentTimeMillis()))

      }
      val notifications = commonCacheService.get(classOf[PushNotificationSeq],userId)
      val count = Option(commonCacheService.get(classOf[PushNotificationCount],userId)).map(f=>f.count).getOrElse(0)
      if(notifications!=null) (notifications.data, count)
      else (List(),count)
    }
    def loadNotifications:(Seq[PushNotification],Seq[PushNotificationCount])={
      connectionProvider.withConnection {
        connection =>
//          val dbResult = dataOperation{
//            DB.dbExec(connection, "[Notification].[GetData]",contextProvider.getContext.sessionId,List())
//          }
          val statement = dataOperation {
            DB.prepareStatement(connection,"[Notification].[GetData]",contextProvider.getContext.sessionId,List())
          }

          val resultAvailable = dataOperation {
            statement.execute()
          }
          if (resultAvailable) {

            dataOperation {
              def getResultSet = if (statement.getMoreResults())
                statement.getResultSet()
              else throw new DataError("Not enough ResultSets")

              //получить уведомления
              var dbResult = statement.getResultSet

              val notifications = scala.collection.mutable.Buffer[PushNotification]()
              val meta = dbResult.getMetaData()
              val columnNameSeq = for (i<-1 to meta.getColumnCount())
              yield meta.getColumnName(i)

              while (dbResult.next()) {
                val userId = dbResult.getString("UserID")
                val parameters = columnNameSeq.filter(p=>p!="UserID").map(f=>f->dbResult.getString(f)).toMap
                if(userId!=null) notifications += PushNotification(parameters,userId)
              }


              // получить кол-во сообщений
              dbResult = getResultSet
              val counts = scala.collection.mutable.Buffer[PushNotificationCount]()
              while (dbResult.next()) {
                val userId = dbResult.getString("UserID")
                val count = dbResult.getInt("Count")
                counts += PushNotificationCount(userId,count)
              }
              (notifications.toList,counts.toList)
            }
          }
          else (Seq(),Seq())


      }
    }
}
case class UpdateTime(time:Long)
