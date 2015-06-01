package omed.model.services

import omed.model.EntityInstance

/**
 * Класс для обработки системных тригеров
 */
trait SystemTriggerProvider {
  /**
   * Обновление _Name объекта
   * @param entity
   */
  def updateName(entity:EntityInstance)
}
