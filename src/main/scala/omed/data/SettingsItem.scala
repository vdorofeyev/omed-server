package omed.data

/**
 * Элемент множества настроек, представляюий из себя пару ключ-значение и описание.
 */
class SettingsItem(val code: String, val name: String, val description: String, val strValue: String,val classCode:String) extends Serializable
