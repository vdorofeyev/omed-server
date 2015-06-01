package omed.mocks

import scala.collection.mutable.ArrayBuffer

class ConnectionProviderMock extends omed.db.ConnectionProvider {

  val methodHistory = new ArrayBuffer[String]

  protected override def getCurrentConnection(): java.sql.Connection = {
    this.methodHistory += "getCurrentConnection"
    null
  }

  protected override def closeCurrentConnection() = {
    this.methodHistory += "closeCurrentConnection"

  }

  protected override def requireTransaction() {
    this.methodHistory += "requireTransaction"
  }

  protected override def commit() {
    this.methodHistory += "commit"
  }

  protected override def rollback() {
    this.methodHistory += "rollback"
  }

}