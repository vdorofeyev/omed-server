package omed.mocks

import omed.predNotification.PredNotificationProvider
import omed.model.EntityInstance

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 19.12.13
 * Time: 16:26
 * To change this template use File | Settings | File Templates.
 */
class PredNotifocationProviderMock extends PredNotificationProvider{
  def createPredNotificationsForObject(entity :EntityInstance) = Nil
  def updatePredNotificationsForObject(entity :EntityInstance) = Nil
}
