package omed.db

import java.sql.Connection
import javax.sql.DataSource
import com.google.inject.Inject
import java.util.logging.Logger

class ConnectionProvider {
  @Inject
  var dataSourceProvider: DataSourceProvider = null

  val logger = Logger.getLogger(classOf[ConnectionProvider].getName())

  protected var connection: Connection = null
  /**
   * Counts all connection-getting operations
   */
  protected var referenceCounter = 0

  protected def getCurrentConnection(): Connection = {
    referenceCounter += 1

    if (connection == null || connection.isClosed) {
      if (transactionCounter > 0) {
        throw new RuntimeException("Transaction is interrupted because of broken connection")
      } else {
        connection = createConnection
        referenceCounter = 1
      }
    }

    connection
  }

  protected def createConnection(): Connection = {
    logger.finest("create connection")
    val dataSource: DataSource = dataSourceProvider.getDataSource
    dataSource.getConnection()
  }

  /**
   * Closes current connection if reference count for it is 0
   */
  protected def closeCurrentConnection() = {
    referenceCounter -= 1

    if (referenceCounter <= 0)
      if (connection != null && !connection.isClosed) {
        if (transactionCounter > 0) {
          rollbackTransaction()
          transactionCounter = 0
        }

        logger.finest("close connection")
        connection.close()
        referenceCounter = 0
      }
  }


  /**
   * Counter for nested transactions.
   * Really starts only outer transaction.
   */
  protected var transactionCounter: Int = 0

  protected def requireTransaction() {
    transactionCounter += 1

    if (transactionCounter > 1)
      return

    beginTransaction()
  }

  protected def beginTransaction() {
    if (connection == null || connection.isClosed)
      throw new RuntimeException("Connection must be opened to start transaction")

    connection.setAutoCommit(false)
  }

  protected def commit() {
    if (mustRollback)
      throw new RuntimeException("Error in code structure: commit() called after inner rollback().")

    if (transactionCounter == 0)
      throw new RuntimeException("Transaction not started")

    transactionCounter -= 1

    if (transactionCounter == 0) {
      commitTransaction()
    }
  }

  protected def commitTransaction() {
    connection.commit()
    connection.setAutoCommit(true)
  }

  protected var mustRollback: Boolean = false

  protected def rollback() {
    mustRollback = true

    if (transactionCounter == 0)
      throw new RuntimeException("Transaction not started.")

    transactionCounter -= 1

    if (transactionCounter == 0) {
      rollbackTransaction()
    }
  }

  protected def rollbackTransaction() {
    connection.rollback()
    connection.setAutoCommit(true)
    mustRollback = false
  }

  def withConnection[A](f: Connection => A): A = {
    getCurrentConnection()
    try {
      f(connection)
    } finally {
      closeCurrentConnection()
    }
  }

  def withSeparateConnection[A](f: Connection => A): A = {
    val conn = createConnection()
    conn.setAutoCommit(true)
    try {
      f(conn)
    } finally {
      conn.close()
    }
  }

  /**
   * Begin transaction if it is not started yet, commit if success or rollback when exception occurs.
   * @param f
   * @tparam A
   * @example connectionProvider.inTransaction { connection => foo(connection, x, y) }
   */
  def inTransaction[A](f: Connection => A): A = {
    getCurrentConnection()

    try {
      requireTransaction()

      val result = f(connection)

      commit()
      result
    } catch {
      case e@_ =>
        rollback()
        throw e
    } finally {
      closeCurrentConnection()
    }
  }
}


