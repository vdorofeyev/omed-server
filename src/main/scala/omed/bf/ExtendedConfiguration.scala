package omed.bf

import omed.lang.eval.{ArgumentSign, FunctionSign, FunctionDecl, Configuration}
import omed.model.DataType
import com.google.inject.Inject
import omed.db.{DBProfiler, ConnectionProvider}
import omed.system.ContextProvider
import java.sql.{Types, Connection}
import omed.data.DataAwareConfiguration

/**
 * Фабрика конфигураций, создающая единый набор функций для использования в выражениях
 */
class ExtendedConfiguration extends ConfigurationProvider {

  @Inject
  var dataConfigProvider: DataAwareConfiguration = null

  def create(): Configuration = {
      val f = Configuration.standard.functions ++
        dataConfigProvider.forCurrentSession().functions ++
        BusinessAwareConfiguration.custom.functions
      new Configuration(f)
  }

}
