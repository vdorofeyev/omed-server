package omed.db

import javax.naming.InitialContext
import javax.sql.DataSource

class DataSourceProvider {
  private val DataSourceContext = "jdbc/OmedDevDataSource"

  def getDataSource() = {
    val initialContext = new InitialContext()
    initialContext.lookup(DataSourceContext).asInstanceOf[DataSource]
  }
}
