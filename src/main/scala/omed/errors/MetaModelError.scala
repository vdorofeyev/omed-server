package omed.errors

/**
 * Ошибка в метаданных
 */
class MetaModelError(val message: String, e: Throwable) extends Exception(message, e) {
  def this() = this(null, null)
  def this(message: String) = this(message, null)
}