package omed.system

import com.google.inject.AbstractModule
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer

/**
 * Created with IntelliJ IDEA.
 * User: naryshkinaa
 * Date: 31.03.14
 * Time: 15:05
 * To change this template use File | Settings | File Templates.
 */
class SingletonServletModule extends AbstractModule {
  protected def configure {
    bind(classOf[omed.db.DataSourceProvider]).in(classOf[com.google.inject.Singleton])
    bind(classOf[omed.bf.BusinessFunctionLogger])
      .to(classOf[omed.bf.BusinessFunctionLoggerImpl]).in(classOf[com.google.inject.Singleton])
    bind(classOf[omed.bf.ProcessStateProvider])
      .to(classOf[omed.bf.ProcessStateProviderImpl]).in(classOf[com.google.inject.Singleton])
  }
}
