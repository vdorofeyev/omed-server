package omed.model

import com.google.inject.Inject

class LazyMetaModel() extends MetaModel {

  @Inject
  var metaClassProvider:MetaClassProvider = null

  private var privateObjectMap:Map[String,MetaObject] = null

  override def objectMap = {
    if(privateObjectMap == null) privateObjectMap =  metaClassProvider.getAllClasses
    privateObjectMap
  }

  override def apply(name: String) =  metaClassProvider.getClassByCode(name)

  override def getObjectById(id: String) = metaClassProvider.getClassMetadata(id)



  override def checkClass(name:String):Boolean={
    this(name)!=null
  }
}