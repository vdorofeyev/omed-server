package omed.cache

import javax.servlet.{ServletContextListener, ServletContextEvent}

import com.hazelcast.core.Hazelcast

class CacheContextListner extends ServletContextListener {

  def contextInitialized(contextEvent: ServletContextEvent) {
    Hazelcast.getDefaultInstance()
  }

  def contextDestroyed(contextEvent: ServletContextEvent) {
    Hazelcast.getLifecycleService.shutdown()
  }

}
