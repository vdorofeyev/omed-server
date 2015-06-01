package omed.push

/**
 * Протокол получения уведомлений
 */
trait PushNotificationService {
  def getUserNotifications:(Seq[PushNotification],Int)
}
