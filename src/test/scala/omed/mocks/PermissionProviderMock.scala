package omed.mocks

import omed.auth.{UserRole, PermissionType, PermissionProvider}
import omed.model.{EntityInstance,Value}

/**
 * Created by andrejnaryskin on 18.03.14.
 */
class PermissionProviderMock extends PermissionProvider{
   def getDataPermission(objectId: String): Map[PermissionType.Value, Boolean] = Map(PermissionType.ReadExec -> true, PermissionType.Write -> true)
   def getDataPermission(obj: EntityInstance): Map[PermissionType.Value, Boolean] = Map(PermissionType.ReadExec -> true, PermissionType.Write -> true)

  /**
   * Получить одну роль
   *
   * @return Список ролей
   */
   def getRole(roleId: String): UserRole = null

  /**
   * Получить текущую роль
   *
   * @return Список ролей
   */
   def getCurrentRole: UserRole = null

  /**
   * Получить права на объект.
   *
   * @param objectId Идентификатор объекта к которому относится разрешение
   * @return Словарь пары ключ:значение `Вид разрешения`:`Да|Нет`
   */
   def getMetaPermission(objectId: String, isSuperUser: Boolean): Map[PermissionType.Value, Boolean] = null
      Map(PermissionType.ReadExec -> true, PermissionType.Write -> true)


  /**
   * получить права на получение данных класса.
   * @param classId
   * @return  Либо bool есть права на все или прав нет совсем, иначе SQL выражение (String)
   */
   def getSQLDataPermission(classId: String,filters:Seq[omed.lang.struct.Expression],context:Map[String,Value]): Map[PermissionType.Value, Any] = Map()

  def getOptionMetaPermission(objectId: String, isSuperUser: Boolean) =  Map(PermissionType.ReadExec -> Option(true), PermissionType.Write -> Option(true))
}
