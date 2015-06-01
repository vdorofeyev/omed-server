package omed.auth

/**
 * Учётная запись пользователя.
 *
 * @param id Идентификатор
 * @param name Логин пользователя
 * @param isSuperUser Признак "Супер пользователь"
 * @param domain Идентификатор домена
 */
case class Account(val id: String, val name: String, val isSuperUser: Boolean, val domain: Int, val hcuId: String)