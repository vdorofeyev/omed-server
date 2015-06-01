package omed.auth

/**
 * Тип разрешения.
 */
object PermissionType extends Enumeration {
  /**
   * Читать/Выполнять.
   */
  val ReadExec = Value("ReadExec")
  /**
   * Писать.
   */
  val Write = Value("Write")
}

/**
 * Резрешение.
 *
 * @param id GUID ИД разрешения
 * @param roleId GUID ИД роли
 * @param objectId GUID ИД объекта, к которому относится разрешение
 * @param objectClassId Ид класса объекта
 * @param action String Действия. Возможные значения:-ReadExec -Write
 * @param isAllowed Разрешено/Запрещено. Возможные значения в БД:-A (Allowed) -F (Forbidden)
 */
case class PermissionMeta(id: String,
  roleId: String,
  objectId: String,
  objectClassId: String,
  action: PermissionType.Value,
  isAllowed: Boolean)

/**
 * Список разрешений.
 *
 * @param data Список разрешений
 */
case class PermissionMetaSeq(data: Seq[PermissionMeta])