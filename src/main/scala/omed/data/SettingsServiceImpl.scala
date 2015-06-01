package omed.data

import omed.db._
import com.google.inject.Inject
import omed.cache.{ExecStatProvider, DomainCacheService}
import omed.system.ContextProvider
import omed.errors.DataError

/**
 * Реализация сервиса настроек
 */
class SettingsServiceImpl extends SettingsService with DataAccessSupport {

  @Inject
  var domainCacheService: DomainCacheService = null
  @Inject
  var connectionProvider: ConnectionProvider = null
  @Inject
  var contextProvider: ContextProvider = null
  @Inject
  var execStatProvider :ExecStatProvider = null
  /**
   * Получение значения настройки по ключу
   * @param key ключ (код) настройки
   * @return значение настройки
   */
  def getGlobalSettings(key: String): Option[SettingsItem] = {
    getSettingsWithClassCode(key,"Settings_Node")
  }
  def getDomainSettings(key: String): Option[SettingsItem] = {
    getSettingsWithClassCode(key,"Settings_Domain")
  }
  private def getSettingsWithClassCode(key: String,classCode:String): Option[SettingsItem] = {
    val cached = domainCacheService.get(classOf[SettingsItem], domainCacheService.wrapKey(key,classCode))
    if (cached == null && domainCacheService.map(classOf[SettingsItem]).isEmpty)  {
    cacheSettingsFromDB
    val data = domainCacheService.get(classOf[SettingsItem], domainCacheService.wrapKey(key,classCode))
    Option(data)
  }
    else Option(cached)
  }
  private def cacheSettingsFromDB{
    connectionProvider.withConnection {
      connection =>
        val rs = DB.dbExec(connection, "[_Meta].[GetSettings]",
          contextProvider.getContext.sessionId, null,execStatProvider)

        while (rs.next()) {
          val settingsItem = new SettingsItem(
            code = rs.getString("Code"),
            name = rs.getString("Name"),
            description = rs.getString("Descr"),
            strValue = rs.getString("StrValue") ,
            classCode = rs.getString("_ClassCode")
          )
          domainCacheService.put(classOf[SettingsItem],domainCacheService.wrapKey(settingsItem.code,settingsItem.classCode), settingsItem)
        }
    }
  }

  def getUserSettings(userId:String):Map[String,Any] ={
    connectionProvider.withConnection {
      connection =>
        val rs = DB.dbExec(connection, "[_Meta].[GetUserSettings]",
          contextProvider.getContext.sessionId, List("UserID"->userId),execStatProvider)
        val result =scala.collection.mutable.Map[String,Any] ()
        while (rs.next()) {
          val code = rs.getString("Code")
          val name = rs.getString("Name")
          val description = rs.getString("Descr")
          val classCode = rs.getString("_ClassCode")
          val  strValue = rs.getString("StrValue")
          val intValue = rs.getInt("IntValue")
          val  dateValue = rs.getString("DateValue")
          val objectId = rs.getString("ObjectID")
          val value = if(strValue!=null) strValue  else objectId
          result += code->value
        }
        Map[String,Any]() ++ result
    }
  }
  def getClientTheme(clientThemeId:String): String ={
    val cache = domainCacheService.get(classOf[ClientThemeDescription],clientThemeId)
    if(cache!= null) cache.content
    else {
        connectionProvider.withConnection {
          connection =>

            val statement = dataOperation {
              DB.prepareStatement(connection, "[_Meta].[GetClientTheme]",
                contextProvider.getContext.sessionId,List("ClientThemeID"->clientThemeId))
            }

            val resultAvailable = dataOperation {
              DBProfiler.profile("[_Meta].[GetClientTheme]",execStatProvider) { statement.execute() }
            }

            val theme= if (resultAvailable) {
              dataOperation {
                def nextResultSet() = if (!statement.getMoreResults())
                  throw new DataError("Not enough ResultSets")
                 //Theme description
                var dbResult = statement.getResultSet()
                dbResult.next()
                val id = Option(dbResult.getObject("ID")).map(f=> f.toString).getOrElse(null)
                val name = Option(dbResult.getObject("Name")).map(f=> f.toString).getOrElse(null)
                val code = Option(dbResult.getObject("Code")).map(f=> f.toString).getOrElse(null)
                val excludes = Set("Name","Code","ID")
                val meta = dbResult.getMetaData()
                val columnSeq = (for (i <- 1 to meta.getColumnCount())
                yield meta.getColumnName(i)).filter(p => ! excludes.contains(p))

                val themeParams = columnSeq.map(f=> f -> dbResult.getObject(f).toString).toMap
                val clientThemeGroups =  scala.collection.mutable.ArrayBuffer[ClientThemeGroup]()
                //Group Description
                nextResultSet
                dbResult = statement.getResultSet()
                val metaThemeGroups = dbResult.getMetaData()
                while (dbResult.next()) {
                  val name = Option(dbResult.getObject("ButtonGroupName")).map(f=> f.toString).getOrElse(null)
                  val id =  Option(dbResult.getObject("ButtonGroupID")).map(f=> f.toString).getOrElse(null)
                  val excludeGroupParams = Set("ButtonGroupName","ButtonGroupID")

                  val columnThemeGroups = (for (i <- 1 to metaThemeGroups.getColumnCount())
                  yield metaThemeGroups.getColumnName(i)).filter(p => ! excludeGroupParams.contains(p))

                  val themeGroupParams = columnThemeGroups.map(f=> f -> dbResult.getObject(f).toString).toMap
                  clientThemeGroups append ClientThemeGroup(id,name,themeGroupParams)
                }
                ClientTheme(id,name,code,themeParams,clientThemeGroups.toSeq)
              }

            }
            else throw new DataError("Тема с идентификатором "+ clientThemeId + " не найдена")

           val result =  theme.toXml
           domainCacheService.put(classOf[ClientThemeDescription],clientThemeId,ClientThemeDescription(result))
           result

        }
    }
  }

}
