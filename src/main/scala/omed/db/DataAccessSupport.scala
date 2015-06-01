package omed.db

import omed.errors.{NotFoundError, MetaModelError, DataError}
import omed.system.SecurityError
import omed.auth.AuthError

/**
 * Вспомогательный trait для перехвата исключений в слое данных
 * с целью конвертирования типа ошибки и выдачи понятного сообщения об ошибке.
 */
trait DataAccessSupport {
  /**
   * Обертка над кодом для работы с данными из хранилища.
   * Перехватывает исключения и генерирует новое исключение типа DataError
   */
  def dataOperation[RType](f: => RType): RType = {
    dataOperation("Ошибка при работе с хранилищем данных")(f)
  }

  /**
   * Обертка над кодом для работы с данными из хранилища.
   * Перехватывает исключения и генерирует новое исключение типа DataError
   * @param message Operation description
   */
  def dataOperation[RType](message: String)(f: => RType): RType = {
    try {
      f
    } catch {
      case e: MetaModelError => throw e
      case e: NotFoundError => throw e
      case e: DataError => throw e
      case e: SecurityError => throw new AuthError
      case e @ _ => throw new DataError(message, code=0, error=e)
    }
  }
}
