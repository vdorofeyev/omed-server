package omed.model


import com.hazelcast.core.{Hazelcast, IMap}
import com.google.inject.Inject
import omed.system.ContextProvider


/**
 * Кэш мета-объектов
 */
class MetaObjectCacheManager {

  @Inject
  var contextProvider: ContextProvider = null

  private def map = {
    val mapName = contextProvider.getContext.domainId.toString + ":omed.model.MetaClass"
    Hazelcast.getMap(mapName).asInstanceOf[IMap[String, MetaObject]]
  }

  // cache values only for current domain
  val nearCache = scala.collection.mutable.Map[String, MetaObject]()
  // code by id
  val index = scala.collection.mutable.Map[String, String]()
  var isAllInNearCache = false

  def addToNearCache(o: MetaObject) {
    nearCache.put(o.code, o)
    index.put(o.id, o.code)
  }

  def getAllClasses: Seq[MetaObject] = {
    import scala.collection.JavaConversions._

    if (isAllInNearCache)
      nearCache.values.toSeq
    else {
      map.foreach(x => addToNearCache(x._2))
      isAllInNearCache = true
      map.values.toSeq
    }
  }

  def getById(classId: String): MetaObject = {
    if (index.contains(classId))
      nearCache(index(classId))
    else {
      val result = map.get(classId)
      addToNearCache(result)
      result
    }
  }

  def getByCode(code: String): MetaObject = {
    if (nearCache.contains(code))
      nearCache(code)
    else {
      import com.hazelcast.query.PredicateBuilder
      import scala.collection.JavaConversions._

      val e = new PredicateBuilder().getEntryObject()
      val predicate = e.get("code").equal(code)

      val result = map.values(predicate)
      if(result.isEmpty) null
      else{
        addToNearCache(result.head)
        result.head
      }
    }
  }

  def put(key: String, obj: MetaObject) {
    map.put(key, obj)
    addToNearCache(obj)
  }

  def get(key: String) = getById(key)

  def drop() {
    map.clear()
    nearCache.clear()
  }

  def isEmpty() = nearCache.isEmpty && map.isEmpty
}
