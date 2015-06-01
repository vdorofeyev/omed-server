package omed.cache

import com.google.inject.Inject
import omed.system.ContextProvider


class DomainCacheService extends HazelcastCacheService {

  @Inject
  var contextProvider: ContextProvider = null

  protected def decorateName(name: String): String = {
    val domainStr = Option(contextProvider)
      .map(_.getContext.domainId.toString)
      .orNull
    String.format("%s:%s", domainStr, name)
  }
  def wrapKey(key:String, classCode:String): String ={
    key+"__"+classCode
  }
}
