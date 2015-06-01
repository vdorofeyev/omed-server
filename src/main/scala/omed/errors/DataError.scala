
package omed.errors

/**
 * Ошибка при работе с хранилищем данных
 */
class DataError(val message: String, val code: Int = 0, val error: Throwable = null)
  extends RuntimeException(message, error)