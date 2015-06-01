package omed.db

import com.hazelcast.core.Hazelcast

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 10.01.14
 * Time: 16:49
 * To change this template use File | Settings | File Templates.
 */
class LockProvider {
  def locked[A](name: String)(f: => A): A = {
    val lock = Hazelcast.getLock(name)
    lock.lock()
    try{
        f
    } finally {
      lock.unlock()
    }
  }
}
