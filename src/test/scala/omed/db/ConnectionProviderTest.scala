package omed.db

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite}

@RunWith(classOf[JUnitRunner])
class ConnectionProviderTest extends FunSuite with BeforeAndAfter {

  class ExpectedError extends RuntimeException {}

  class TestConnectionProvider extends ConnectionProvider {
    override protected def createConnection() = {
      realConCount += 1
      new ConnectionMock()
    }

    override protected def commitTransaction() { committed = true }

    override protected def rollbackTransaction() { rolledback = true }

    override protected def beginTransaction() { realTranCount += 1 }

    var realConCount: Int = 0
    var realTranCount: Int = 0
    var committed: Boolean = false
    var rolledback: Boolean = false

    def getTransactionCount = transactionCounter

    def getConnectionCount = referenceCounter
  }

  var connectionProvider: TestConnectionProvider = null

  before {
    connectionProvider = new TestConnectionProvider()
  }

  test("One connection") {
    assert(connectionProvider.getConnectionCount == 0)
    connectionProvider.withConnection {
      connection =>
        assert(connectionProvider.getConnectionCount == 1)
    }
  }

  test("Inner connection") {
    assert(connectionProvider.getConnectionCount == 0)
    connectionProvider.withConnection {
      connection =>
        assert(connectionProvider.getConnectionCount == 1)
        assert(connectionProvider.realConCount == 1)

        connectionProvider.withConnection {
          connection =>
            assert(connectionProvider.getConnectionCount == 2)
            assert(connectionProvider.realConCount == 1)
        }
    }
  }

  test("Transaction commit") {
    connectionProvider.inTransaction {
      c =>
        assert(connectionProvider.getConnectionCount == 1)
        assert(connectionProvider.getTransactionCount == 1)
        assert(!connectionProvider.committed)
        assert(connectionProvider.realTranCount == 1)
    }

    assert(connectionProvider.committed)
  }

  test("Transaction commit in connection") {
    connectionProvider.withConnection {
      cc =>
        assert(connectionProvider.getConnectionCount == 1)
        assert(connectionProvider.realTranCount == 0)

        connectionProvider.inTransaction {
          c =>
            assert(connectionProvider.getConnectionCount == 2)
            assert(connectionProvider.getTransactionCount == 1)
            assert(connectionProvider.realConCount == 1)
            assert(connectionProvider.realTranCount == 1)
        }

        assert(connectionProvider.committed)
        assert(connectionProvider.getTransactionCount == 0)
    }
  }

  test("Transaction rollback") {
    intercept[ExpectedError] {
      assert(connectionProvider.realTranCount == 0)

      connectionProvider.inTransaction {
        c =>
          assert(connectionProvider.realTranCount == 1)
          throw new ExpectedError()
      }
    }

    assert(connectionProvider.getConnectionCount == 0)
    assert(!connectionProvider.committed)
    assert(connectionProvider.rolledback)
  }

  test("Transaction rollback in connection") {

  }

  test("Inner transaction") {
    connectionProvider.inTransaction {
      c =>
        assert(connectionProvider.realTranCount == 1)
        assert(connectionProvider.getTransactionCount == 1)
        assert(!connectionProvider.committed)

        connectionProvider.inTransaction {
          c =>
            assert(connectionProvider.realTranCount == 1)
            assert(connectionProvider.getTransactionCount == 2)
            assert(!connectionProvider.committed)
        }

        assert(connectionProvider.getTransactionCount == 1)
        assert(!connectionProvider.committed)
    }

    assert(connectionProvider.getTransactionCount == 0)
    assert(connectionProvider.committed)
    assert(!connectionProvider.rolledback)
  }

  test("Inner transaction rollback") {
    intercept[ExpectedError] {
      connectionProvider.inTransaction {
        c =>
          assert(connectionProvider.realTranCount == 1)
          assert(connectionProvider.getTransactionCount == 1)
          assert(!connectionProvider.committed)

          try {
            connectionProvider.inTransaction {
              c =>
                assert(connectionProvider.realTranCount == 1)
                assert(connectionProvider.getTransactionCount == 2)
                assert(!connectionProvider.committed)

                throw new ExpectedError()
            }

            throw new Exception("Must not throw")
          } catch {
            case e: ExpectedError =>
              assert(connectionProvider.realTranCount == 1)
              assert(connectionProvider.getTransactionCount == 1)
              assert(!connectionProvider.committed)
              assert(!connectionProvider.rolledback)
              throw e
          }
      }
    }

    assert(connectionProvider.getTransactionCount == 0)
    assert(!connectionProvider.committed)
    assert(connectionProvider.rolledback)
  }

  test("Inner transaction commit and outer rollback") {
    intercept[ExpectedError] {
      connectionProvider.inTransaction {
        c =>
          assert(connectionProvider.realTranCount == 1)
          assert(connectionProvider.getTransactionCount == 1)
          assert(!connectionProvider.committed)

          connectionProvider.inTransaction {
            c =>
              assert(connectionProvider.realTranCount == 1)
              assert(connectionProvider.getTransactionCount == 2)
              assert(!connectionProvider.committed)
          }

          assert(connectionProvider.getTransactionCount == 1)
          assert(!connectionProvider.committed)
          assert(!connectionProvider.rolledback)

          throw new ExpectedError()
      }
    }

    assert(connectionProvider.getTransactionCount == 0)
    assert(!connectionProvider.committed)
    assert(connectionProvider.rolledback)
  }

  test("Many transactions and connections") {
    intercept[ExpectedError] {
      connectionProvider.inTransaction {
        c =>
          assert(connectionProvider.realTranCount == 1)
          assert(connectionProvider.getTransactionCount == 1)

          connectionProvider.inTransaction {
            c =>
              assert(connectionProvider.realTranCount == 1)
              assert(connectionProvider.getTransactionCount == 2)

              connectionProvider.withConnection {
                c =>
                  assert(connectionProvider.realTranCount == 1)
                  assert(connectionProvider.getTransactionCount == 2)
              }

              connectionProvider.withConnection {
                c =>
                  assert(connectionProvider.realTranCount == 1)
                  assert(connectionProvider.getTransactionCount == 2)

                  connectionProvider.withConnection {
                    c =>
                      assert(connectionProvider.realTranCount == 1)
                      assert(connectionProvider.getTransactionCount == 2)

                      connectionProvider.inTransaction {
                        c =>
                          assert(connectionProvider.realTranCount == 1)
                          assert(connectionProvider.getTransactionCount == 3)
                      }

                      connectionProvider.inTransaction {
                        c =>
                          assert(connectionProvider.realTranCount == 1)
                          assert(connectionProvider.getTransactionCount == 3)

                          connectionProvider.withConnection {
                            c =>
                              assert(connectionProvider.realTranCount == 1)
                              assert(connectionProvider.getTransactionCount == 3)

                              throw new ExpectedError()
                          }
                      }
                  }
              }
          }
      }
    }

    assert(connectionProvider.realTranCount == 1)
    assert(connectionProvider.realConCount == 1)
    assert(connectionProvider.getTransactionCount == 0)
    assert(connectionProvider.getConnectionCount == 0)
    assert(!connectionProvider.committed)
    assert(connectionProvider.rolledback)
  }

  test("Commit wrong transaction") {
    intercept[RuntimeException] {
      // try to commit transaction after inner transaction was rollbacked
      connectionProvider.inTransaction {
        c =>
          try {
            connectionProvider.inTransaction {
              c =>
                throw new ExpectedError()
            }
          } catch {
            case _ => null
          }
      }
    }
  }
}
