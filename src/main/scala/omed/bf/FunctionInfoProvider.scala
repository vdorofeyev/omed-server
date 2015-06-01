package omed.bf

trait FunctionInfoProvider {
  
  /**
   * Получить описание бизнес-функции
   */
  def getFunctionInfo(functionId: String): Option[BusinessFunction]

}