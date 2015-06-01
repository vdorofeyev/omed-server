package omed.errors

/**
 * Запрашиваемые данные должны быть, но не найдены
 */
class NotFoundError (val message: String) extends Exception(message)