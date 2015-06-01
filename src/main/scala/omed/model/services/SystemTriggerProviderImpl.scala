package omed.model.services

import omed.model.{MetaClassProvider, EntityInstance}
import com.google.inject.Inject
import omed.db.{DBProfiler, DBProvider}
import omed.cache.ExecStatProvider
import omed.data.DataWriterService
import omed.bf.ConfigurationProvider

/**
 * Created with IntelliJ IDEA.
 * User: naryshkinaa
 * Date: 23.04.14
 * Time: 11:26
 * To change this template use File | Settings | File Templates.
 */
class SystemTriggerProviderImpl extends SystemTriggerProvider{
  @Inject
  var metaClassProvider: MetaClassProvider = null
  @Inject
  var dbProvider: DBProvider = null
  @Inject
  var execStatProvider:ExecStatProvider = null
  @Inject
  var dataWriterService: DataWriterService = null
  @Inject
  var expressionEvaluator:ExpressionEvaluator = null
  @Inject
  var configProvider: ConfigurationProvider = null

  def updateName(entity: EntityInstance){
    DBProfiler.profile("update _Name",execStatProvider,true){
     val _name = expressionEvaluator.evaluate(entity.obj.aliasPattern,variables = Map("this"->entity)).toString
     dataWriterService.directSaveField(entity.getClassId,entity.getId,"_Name",_name)
    }
  }
}
