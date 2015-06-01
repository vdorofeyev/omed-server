package omed.auth

import omed.model.{Value, EntityInstance}
import omed.lang.struct.Expression

/**
 * Интерфейс сервиса для работы с пользовательскими правами.
 */
trait PermissionProvider {
  /**
   * Получить права на МетаОбъект. Если прав нет то возвращает false
   * 
   * @param objectId Идентификатор объекта к которому относится разрешение
   * @return Словарь пары ключ:значение `Вид разрешения`:`Да|Нет`
   */
  def getMetaPermission(objectId: String,isSuperUser:Boolean = false) : Map[PermissionType.Value, Boolean]

    /**
     * Получить права на МетаОбъект. Если прав нет то возвращает неопределено
     *
     * @param objectId Идентификатор объекта к которому относится разрешение
     * @return Словарь пары ключ:значение `Вид разрешения`:`Да|Нет`
     */
  def getOptionMetaPermission(objectId: String,isSuperUser:Boolean = false) : Map[PermissionType.Value, Option[Boolean]]

  /**
   * Получить права на объект
   * @param objectId
   * @return
   */
  def getDataPermission(objectId:String):Map[PermissionType.Value,Boolean]
  def getDataPermission(instance:EntityInstance):Map[PermissionType.Value,Boolean]

  /**
   *  получить права на получение данных класса.
   * @param classId
   * @return  Либо bool есть права на все или прав нет совсем, иначе SQL выражение (String)
   */

  def getSQLDataPermission(classId:String, filters:Seq[Expression],context:Map[String,Value] ):Map[PermissionType.Value,Any]
  /**
   * Получить одну роль
   *
   * @return Список ролей
   */
  def getRole(roleId:String) : UserRole

  /**
   * Получить текущую роль
   *
   * @return Список ролей
   */
  def getCurrentRole : UserRole
}
