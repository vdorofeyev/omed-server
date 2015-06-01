package omed.data

/**
 * Сервис для получения настроек системы
 */

trait SettingsService {

  /**
   * Получение значения настройки по ключу
   * @param key ключ (код) настройки
   * @return значение настройки
   */
  def getUserSettings(userId:String):Map[String,Any]
  def getGlobalSettings(key: String): Option[SettingsItem]
  def getDomainSettings (key: String): Option[SettingsItem]
  def getClientTheme(clientThemeId:String): String
}

