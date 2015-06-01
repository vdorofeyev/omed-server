package omed.server

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import com.google.inject.servlet.{ GuiceServletContextListener, GuiceFilter }
import org.eclipse.jetty.servlet.DefaultServlet
import com.google.inject.{ AbstractModule, Guice, Injector }
import com.google.inject.util.Modules
import omed.cache.CacheContextListner

object Launcher extends App {
  val server = new Server(8080)

  val sch = new ServletContextHandler(server, "/omed-server", ServletContextHandler.SESSIONS)
  sch.addEventListener(new EmbeddedGuiceServletConfig())

  sch.addFilter(classOf[GuiceFilter], "/api/*", null)

  // Must add DefaultServlet for embedded Jetty.
  // Failing to do this will cause 404 errors.
  // This is not needed if web.xml is used instead.
  sch.addServlet(classOf[DefaultServlet], "/")

  sch.getSessionHandler.getSessionManager.getSessionCookieConfig.setName("OMEDSESSIONID")

  // hazelcast
  sch.addEventListener(new omed.cache.CacheContextListner())

  server.start()
  server.join()
}

object EmbeddedDataSourceProvider {
  var ds: javax.sql.DataSource = null
}

class EmbeddedDataSourceProvider extends omed.db.DataSourceProvider {
  override def getDataSource() = {
    if (EmbeddedDataSourceProvider.ds == null) {
      import java.util.logging.Logger
      val log = Logger.getLogger("")
      log.info("EmbeddedDataSourceProvider")

      val ds = new com.microsoft.sqlserver.jdbc.SQLServerDataSource()
      ds.setUser("omed_dev")
      ds.setPassword("omed_dev")
//      ds.setDatabaseName("OmedNode3")
//      ds.setServerName("10.109.1.13")
//      ds.setDatabaseName("alm")
//      ds.setServerName("10.109.1.16")
      ds.setDatabaseName("OmedNode3")
      ds.setServerName("95.131.28.108")
      ds.setPortNumber(9011)

//      // cloud database
//      ds.setDatabaseName("Omed")
//      ds.setServerName("omed-db.cloudapp.net")
//      ds.setPortNumber(3341)

      EmbeddedDataSourceProvider.ds = ds
    }

    EmbeddedDataSourceProvider.ds
  }
}

class EmbeddedModule extends AbstractModule {
  def configure() {
    import java.util.logging.Logger
    val log = Logger.getLogger("")
    log.info("EmbeddedModule")
    bind(classOf[omed.db.DataSourceProvider]).to(classOf[EmbeddedDataSourceProvider])
  }
}

class EmbeddedGuiceServletConfig extends GuiceServletContextListener {
  override protected def getInjector(): Injector = {
    return Guice.createInjector(Modules.`override`(new omed.system.ConfiguredJerseyServletModule()).
      `with`(new EmbeddedModule()))
  }
}
