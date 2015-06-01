package omed.auth

import com.google.inject.Inject
import omed.system.ContextProvider
import collection.mutable
import omed.data.EntityFactory
import omed.model.services.ExpressionEvaluator
import omed.bf.ConfigurationProvider
import omed.model.{DataType, EntityInstance, MetaClassProvider, SimpleValue}
import omed.cache.ExecStatProvider
import omed.db.DBProfiler
import omed.lang.struct.{Constant, LogicalOperationType, LogicalExpression, Expression}
import omed.model.Value
/**
 * Реализация сервиса для работы с пользовательскими правами.
 */
class PermissionProviderImpl extends PermissionProvider {
  /**
   * Менеджер контекстов, через который можно получить
   * текущий контекст.
   */
  @Inject
  var contextProvider: ContextProvider = null
  /**
   * Сервис для получения прав из хранилища.
   */
  @Inject
  var permissionReader: PermissionReader = null

  @Inject
  var entityFactory: EntityFactory = null

  @Inject
  var configProvider: ConfigurationProvider = null

  @Inject
  var expressionEvaluator:ExpressionEvaluator = null

  @Inject
  var execStatProvider:ExecStatProvider = null

  @Inject
  var metaClassProvider: MetaClassProvider = null
  /**
   * Список идентификаторов ролей пользователя.
   */
  lazy val userRoleIds = Seq(getCurrentRole.id) //permissionReader.getUserRoles(contextProvider.getContext.authorId)

  /**
   * Получить права на объект.
   *
   * @param objectId Идентификатор объекта к которому относится разрешение
   * @return Словарь пары ключ:значение `Вид разрешения`:`Да|Нет`
   */
  def getMetaPermission(objectId: String,isSuperUser:Boolean = false): Map[PermissionType.Value, Boolean] = {
    DBProfiler.profile("getMetaPermission",execStatProvider,true) {
      if (contextProvider.getContext.isSuperUser || isSuperUser)
        return PermissionType.values.map(x => x -> true).toMap
      if (objectId == null)
        return PermissionType.values.map(x => x -> false).toMap
      val permissions = permissionReader.getObjectPermissions(objectId)

       val effectiveRoleIds = getEffectiveRoles
      // get permissions for objectId and effective roles
      val effectivePermissions =  permissions.filter(
        p => effectiveRoleIds.contains(p.roleId))
      // group by type
      val readPerm  =  effectivePermissions.filter(_.action== PermissionType.ReadExec)
      val wrPerm =  effectivePermissions.filter(_.action== PermissionType.Write)

      Map(PermissionType.ReadExec -> (readPerm.forall(_.isAllowed) && effectivePermissions.exists(_.isAllowed)),
        PermissionType.Write -> (wrPerm.exists(_.isAllowed) && effectivePermissions.forall(_.isAllowed)) )
    }
  }
  def getOptionMetaPermission(objectId: String,isSuperUser:Boolean = false) : Map[PermissionType.Value, Option[Boolean]]={
    DBProfiler.profile("getMetaPermission",execStatProvider,true) {
      if (contextProvider.getContext.isSuperUser || isSuperUser)
        return PermissionType.values.map(x => x -> Option(true)).toMap
      if (objectId == null)
        return PermissionType.values.map(x => x -> Option(false)).toMap
      val permissions = permissionReader.getObjectPermissions(objectId)

      val effectiveRoleIds = getEffectiveRoles
      // get permissions for objectId and effective roles
      val effectivePermissions =  permissions.filter(
        p => effectiveRoleIds.contains(p.roleId))
      // group by type
      val readPerm  =  effectivePermissions.filter(_.action== PermissionType.ReadExec)
      val wrPerm =  effectivePermissions.filter(_.action== PermissionType.Write)

      Map(PermissionType.ReadExec -> ( if(readPerm.length>0 || wrPerm.exists(_.isAllowed) ) Option(readPerm.forall(_.isAllowed) && effectivePermissions.exists(_.isAllowed )) else None) ,
        PermissionType.Write -> (if(wrPerm.length>0) Option(wrPerm.exists(_.isAllowed) && effectivePermissions.forall(_.isAllowed)) else None) )
    }
  }
  private lazy val getEffectiveRoles:Set[String]={
      val roles = permissionReader.getAllRoles
      val roleMap = roles.groupBy(_.id)

      // bypass roles graph starting with userRoleIds:
      // add all role ids to mutable ordered set,
      // so we get a set of all roles for user in one pass
      val effectiveRoleIds = mutable.LinkedHashSet[String]()
      effectiveRoleIds ++= userRoleIds

      for (id <- effectiveRoleIds) {
        effectiveRoleIds ++= roleMap(id).map(_.parentId).filter(_ != null)
      }
      effectiveRoleIds.toSet
  }

  def getDataPermission(objectId:String):Map[PermissionType.Value,Boolean]={
    DBProfiler.profile("getDataPermission",execStatProvider,true) {
      if (contextProvider.getContext.isSuperUser) return PermissionType.values.map(x => x -> true).toMap
      val obj = entityFactory.createEntity(objectId)
      execDataPermission(obj)
    }
  }
  def getDataPermission(instance:EntityInstance):Map[PermissionType.Value,Boolean]={
    DBProfiler.profile("getDataPermission",execStatProvider,true) {
      if (contextProvider.getContext.isSuperUser) return PermissionType.values.map(x => x -> true).toMap
      execDataPermission(instance)
    }
  }
  private def execDataPermission(instance:EntityInstance):Map[PermissionType.Value,Boolean]={
    val effectiveRoleIds = getEffectiveRoles
    val effectivePermissions =  permissionReader.getDataClassPermissions(instance.getClassId)
      .filter(p => effectiveRoleIds.contains(p.roleId))
    val config = configProvider.create()
    val context = contextProvider.getContext.getSystemVariables
    val rules = effectivePermissions.filter(f =>
    {
      DBProfiler.profile("getDataPermission evaluate",execStatProvider,true) {
        omed.model.DataType.boolValueFromValue(expressionEvaluator.evaluate(f.expression,config,context ++ Map("this"->instance)))
      }
    })
    Map(PermissionType.ReadExec -> (rules.forall(p => p.isAllowed || p.action == PermissionType.Write) && rules.exists(p => p.isAllowed)),
      PermissionType.Write -> (rules.forall(p => p.isAllowed) && rules.exists(p => p.isAllowed && p.action == PermissionType.Write)))
  }
  def getSQLDataPermission(classId:String, filters:Seq[Expression],context:Map[String,Value] ):Map[PermissionType.Value,Any]={
    DBProfiler.profile("getSQLDataPermission",execStatProvider,true) {
      val additionFilters =
        if(filters.length == 0) null
        else {
          var res:Expression = null
          filters.foreach(f => res = (if(res== null) f else LogicalExpression(LogicalOperationType.And,Seq(res,f))))
          res
        }
      val config = configProvider.create()
      val classCode = metaClassProvider.getClassMetadata(classId).code

      if (contextProvider.getContext.isSuperUser) {
        if(additionFilters == null) return PermissionType.values.map(x => x -> true).toMap
        else {
          val res = expressionEvaluator.compileSQL("true",config,classCode,additionFilters,context) match {
            case s:SimpleValue => s.data
            case _ => throw new RuntimeException("результат компиляции не является простым типом")
          }
          return  Map(PermissionType.ReadExec -> res, PermissionType.Write -> res)
        }
      }
      val effectiveRoleIds = getEffectiveRoles
      val effectivePermissions =  permissionReader.getDataClassPermissions(classId)
        .filter(p => effectiveRoleIds.contains(p.roleId)).map(f => f.copy(expression = f.expression.replaceAll("@this","@__var1")))

      val ra = effectivePermissions.filter(_.isAllowed)
      val wa = effectivePermissions.filter(f => f.isAllowed && f.action == PermissionType.Write)
      val rd = effectivePermissions.filter(f => !f.isAllowed && f.action == PermissionType.ReadExec)
      val wd = effectivePermissions.filter(!_.isAllowed)


      val readResult = DBProfiler.profile("compile SQL",execStatProvider,true) {
        if(ra.length == 0) false else {
          val expression ="("+ ra.map(_.expression).mkString(" or ") +")" +  (if(rd.length>0)  " and not (" + rd.map(_.expression).mkString(" or") + ")"  else "")
          expressionEvaluator.compileSQL(expression,config,classCode,additionFilters,context) match {
        case s:SimpleValue => s.data
        case _ => throw new RuntimeException("результат компиляции не является простым типом")
      }
      }
    }
    val writeResult = DBProfiler.profile("compile SQL",execStatProvider,true) {
         if(wa.length == 0) false else {
          val expression ="("+ wa.map(_.expression).mkString(" or ") +")" + (if(wd.length>0) " and not (" + wd.map(_.expression).mkString(" or") + ")" else "")
          expressionEvaluator.compileSQL(expression,config,classCode,additionFilters,context) match {
            case s:SimpleValue => s.data
            case _ => throw new RuntimeException("результат компиляции не является простым типом")
          }
        }
      }
      Map(PermissionType.ReadExec -> readResult, PermissionType.Write -> writeResult)
    }
  }
  def getRole(roleId:String) : UserRole ={
    val roles = permissionReader.getAllRoles
    val role = roles.find(p=> p.id == roleId)
    if(role.isDefined) role.get
    else throw new RuntimeException("не найдена роль с идентификатором:  " + roleId)
  }

  def getCurrentRole : UserRole={
    val roleId =contextProvider.getContext.roleId
    if(roleId==null) throw new RuntimeException("Не установлена текущая роль")
    getRole(roleId)
  }
}
