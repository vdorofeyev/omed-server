package omed.system

/**
 * Используется для получения контекста исполнения.
 */
trait ContextProvider {
  /**
   * Получить текущий контекст.
   */
  def getContext: Context
}
