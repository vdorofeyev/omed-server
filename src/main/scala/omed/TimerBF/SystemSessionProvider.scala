package omed.TimerBF

/**
 * Возвращает список прикладных доменов и системные сессии для этих доменов
 */
trait SystemSessionProvider {
  def getProductionDomains:Seq[Int]
  def getSystemSessionForDomain(domain:Int):String
}
