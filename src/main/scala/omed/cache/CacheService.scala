package omed.cache


trait CacheService {
  def put[A <: AnyRef](clazz: Class[A], key: String, obj: A)

  def get[A <: AnyRef](clazz: Class[A], key: String): A

  def drop[A <: AnyRef](clazz: Class[A])

  def isEmpty[A <: AnyRef](clazz: Class[A]): Boolean

  def map[A <: AnyRef](clazz: Class[A]): Map[String, A]
}
