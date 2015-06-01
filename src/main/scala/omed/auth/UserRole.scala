package omed.auth

/**
 * Пользовательская роль.
 *
 * @param id Идентификатор
 * @param parentId Идентификатор родительской роли
 * @param name Название роли
 */
case class UserRole(id: String, parentId: String, name: String,openObjExp :String = null)
