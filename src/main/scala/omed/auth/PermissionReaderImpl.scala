package omed.auth

import com.google.inject.Inject

import omed.db.{DBProfiler, DB, ConnectionProvider, DataAccessSupport}
import omed.system.ContextProvider
import omed.cache.{ExecStatProvider, DomainCacheService}
import omed.model.MetaClassProvider

/**
 * Реализация сервиса чтения прав из хранилища.
 */
class PermissionReaderImpl extends PermissionReader with DataAccessSupport {

  /**
   * Фабрика подключений к источнику данных.
   */
  @Inject
  var connectionProvider: ConnectionProvider = null
  /**
   * Менеджер контекстов, через который можно получить
   * текущий контекст.
   */
  @Inject
  var contextProvider: ContextProvider = null
  /**
   * Кэш менеджер для данных относящихся к домену.
   */
  @Inject
  var domainCacheService: DomainCacheService = null


  @Inject
  var metaClassProvider: MetaClassProvider = null
  @Inject
  var execStatProvider : ExecStatProvider = null
  /**
   * Получить список идентификаторов ролей пользователя.
   *
   * @param userId Идентификатор пользователя
   * @return Список идентификаторов ролей
   */
  def getUserRoles(userId: String): Seq[String] = {
    connectionProvider.withConnection {
      connection =>
        DBProfiler.profile("_Meta.GetUserRoles") {
          val rs = DB.dbExec(connection, "_Meta.GetUserRoles",
            contextProvider.getContext.sessionId, List("UserID" -> userId))
          val result = scala.collection.mutable.ListBuffer[String]()
          while (rs.next()) {
            result append rs.getString("RoleID")
          }
          result.toSeq
        }
    }
  }

  /**
   * Получить все роли.
   *
   * @return Список ролей
   */
  def getAllRoles: Seq[UserRole] = {
    val cached = domainCacheService.get(classOf[UserRoleSeq], "seq")
      .asInstanceOf[UserRoleSeq]
    if (cached == null)
      connectionProvider.withConnection {
        connection =>
          DBProfiler.profile("_Meta.GetRoleMember") {
            val rs = DB.dbExec(connection, "_Meta.GetRoleMember",
              contextProvider.getContext.sessionId, null)
            val result = scala.collection.mutable.ListBuffer[UserRole]()
            while (rs.next()) {
              result append UserRole(
                id = rs.getString("RoleID"),
                parentId = rs.getString("ParentRoleID"),
                name = rs.getString("RoleName"),
                openObjExp = rs.getString("OpenObjectExp"))
            }
            val data = result.toSeq
            domainCacheService.put(classOf[UserRoleSeq], "seq", UserRoleSeq(data))
            data
          }
      }
    else cached.data
  }

  def getObjectPermissions(objectId:String) : Seq[PermissionMeta]={
      loadPermissionFromDB
      val cached = domainCacheService.get(classOf[PermissionMetaSeq], objectId)
      if (cached == null)
        Seq()
      else cached.data
  }
  /**
   * Получить все права.
   *
   * @return Список прав
   */
  def getAllPermissions: Seq[PermissionMeta] = {
    loadPermissionFromDB
    val cached = domainCacheService.get(classOf[PermissionMetaSeq], "seq")
    if (cached == null)
      Seq()
//      connectionProvider.withConnection {
//        connection =>
//          val rs = DB.dbExec(connection, "[_Meta].[GetPermission]",
//            contextProvider.getContext.sessionId, null)
//          val result = scala.collection.mutable.ListBuffer[PermissionMeta]()
//          while (rs.next()) {
//            if (rs.getString("Action") != null) {
//              val perm = PermissionMeta(
//                id = rs.getString("ID"),
//                roleId = rs.getString("RoleID"),
//                objectId = rs.getString("ObjectID"),
//                objectClassId = rs.getString("Object_ClassID"),
//                action = PermissionType.withName(rs.getString("Action")),
//                isAllowed = ("A" == rs.getString("PermissionKind").toUpperCase))
//              result append perm
//            }
//          }
//          val data = result.toSeq
//          domainCacheService.put(classOf[PermissionMetaSeq], "seq", PermissionMetaSeq(data))
//          data
//      }
    else cached.data
  }

  def loadPermissionFromDB {

    if(domainCacheService.get(classOf[PermissionMetaSeq],"seq") == null)
      connectionProvider.withConnection {
        connection =>
          DBProfiler.profile("cache MetaPermission",execStatProvider ,true) {
            val rs = DB.dbExec(connection, "_Meta.GetPermission",
              contextProvider.getContext.sessionId, null)
            val result = scala.collection.mutable.ListBuffer[PermissionMeta]()
            while (rs.next()) {
              if (rs.getString("Action") != null) {
                val perm = PermissionMeta(
                  id = rs.getString("ID"),
                  roleId = rs.getString("RoleID"),
                  objectId = rs.getString("ObjectID"),
                  objectClassId = rs.getString("Object_ClassID"),
                  action = PermissionType.withName(rs.getString("Action")),
                  isAllowed = ("A" == rs.getString("PermissionKind").toUpperCase))
                result append perm
              }
            }
            val data = result.toSeq
            domainCacheService.put(classOf[PermissionMetaSeq], "seq", PermissionMetaSeq(data))
            data.filter(_.objectId!=null).groupBy(_.objectId).foreach(f => domainCacheService.put(classOf[PermissionMetaSeq], f._1, PermissionMetaSeq(f._2)))
            data
          }
      }
  }


  def getDataClassPermissions(classId: String): Seq[PermissionData] = {
    val query =
      """
        |select ClassID,RoleID,Expression,Action,PermissionKind from _PermissionData where _Domain = -1
      """.stripMargin
    if (domainCacheService.map(classOf[PermissionDataSeq]).isEmpty) {
      connectionProvider.withConnection {
        connection =>
          val rs = connection.prepareStatement(query).executeQuery()
          val result = scala.collection.mutable.ListBuffer[PermissionData]()
          while (rs.next()) {
            if (rs.getString("Action") != null) {
              val perm = PermissionData(
                roleId = rs.getString("RoleID"),
                classId = rs.getString("ClassID"),
                action = PermissionType.withName(rs.getString("Action")),
                isAllowed = ("A" == Option(rs.getString("PermissionKind")).map(_.toUpperCase).orNull),
                expression = rs.getString("Expression"))
              result append perm
            }
          }
          result.toSeq.groupBy(_.classId).foreach(f => domainCacheService.put(classOf[PermissionDataSeq], f._1, PermissionDataSeq(f._2)))
      }
    }
    metaClassProvider.getAllParents(classId).toSeq.map(f => Option(domainCacheService.get(classOf[PermissionDataSeq],f)).map(_.data).getOrElse(Seq())).flatten
  }

}
