package omed.auth

/**
 * Интерфейс сервиса для чтения данных о правах из хранилища.
 */
trait PermissionReader {
  /**
   * Получить список идентификаторов ролей пользователя.
   * 
   * @param userId Идентификатор пользователя
   * @return Список идентификаторов ролей
   */
  def getUserRoles(userId: String): Seq[String]
  
  /**
   * Получить все роли.
   * 
   * @return Список ролей
   */
  def getAllRoles : Seq[UserRole]

  def getDataClassPermissions(classId:String):Seq[PermissionData]

  /**
   * Получить все права.
   * 
   * @return Список прав
   */
  def getAllPermissions : Seq[PermissionMeta]

  def getObjectPermissions(objectId:String) : Seq[PermissionMeta]
}
