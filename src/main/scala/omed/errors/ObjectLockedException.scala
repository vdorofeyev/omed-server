package omed.errors

/**
 *
 * User: Alexander Kolesnikov
 * Date: 12.04.13
 */
class ObjectLockedException(val userFio: String)
  extends Exception("Объект редактируется другим пользователем (" + userFio + ")") {}
