package omed.cache

import com.hazelcast.core.Hazelcast
import com.google.inject.Inject
import omed.system.ContextProvider


abstract class HazelcastCacheService extends CacheService {

  protected def decorateName(name: String): String

  def put[A <: AnyRef](clazz: Class[A], key: String, obj: A) {
    Hazelcast.getMap[String, A](decorateName(clazz.getName)).put(key, obj)
  }

  def get[A <: AnyRef](clazz: Class[A], key: String) = {
    val map = Hazelcast.getMap[String, A](decorateName(clazz.getName))
    val result = map.get(key)
    result
  }

  def drop[A <: AnyRef](clazz: Class[A]) {
    Hazelcast.getMap[String, A](decorateName(clazz.getName)).clear()
  }

  def isEmpty[A <: AnyRef](clazz: Class[A]) = {
    Hazelcast.getMap[String, A](decorateName(clazz.getName)).isEmpty()
  }

  def map[A <: AnyRef](clazz: Class[A]): Map[String, A] = {
    import scala.collection.JavaConversions._
    val map = Hazelcast.getMap[String, A](decorateName(clazz.getName))
    map.toMap
  }
}
