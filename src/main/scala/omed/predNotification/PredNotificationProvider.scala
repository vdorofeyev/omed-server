package omed.predNotification

import omed.model.EntityInstance
import ru.atmed.omed.beans.model.meta.MetaObjectStatus

/**
 * Протокол создания предуведомлений
 */
trait PredNotificationProvider {
   def createPredNotificationsForObject(entity :EntityInstance)
   def updatePredNotificationsForObject(entity :EntityInstance)
}
