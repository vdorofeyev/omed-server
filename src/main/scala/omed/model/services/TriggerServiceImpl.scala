/**
 *
 */
package omed.model.services

import omed.triggers._
import omed.db.{DBProfiler, DB, ConnectionProvider}
import com.google.inject.Inject
import omed.system.ContextProvider
import java.util.logging.Logger
import omed.model.{MetaObject, MetaClassProvider}
import omed.cache.{DomainCacheService, ExecStatProvider}
import com.hazelcast.core.Hazelcast
import omed.lang.eval.DBUtils
import omed.bf.ConfigurationProvider
import omed.data.EntityFactory

class TriggerServiceImpl extends TriggerService {
  val logger = Logger.getLogger(this.getClass.getName())
  @Inject
  var connectionProvider: ConnectionProvider = null
  @Inject
  var contextProvider: ContextProvider = null

  @Inject
  var metaClassProvider: MetaClassProvider = null

  @Inject
  var execStatProvider :ExecStatProvider = null

  @Inject
  var domainCacheService: DomainCacheService = null
  @Inject
  var configProvider: ConfigurationProvider = null
  @Inject
  var expressionEvaluator:ExpressionEvaluator = null
  @Inject
  var entityFactory:EntityFactory = null

  def getTriggersByClass(classId: String): Seq[Trigger] = {
    //получение всех родительских классов включая текущий
    val cached = domainCacheService.get(classOf[TriggerSeq], classId)
    if(cached!=null) cached.data
    else {
      val parents = metaClassProvider.getAllParents(classId)
      val triggers = DBProfiler.profile("cache GetTriggers",execStatProvider,true) {
        getAllTriggers.filter(t => !t.isNotInhereted && parents.contains(t.classID) || t.classID==classId) ++ Option(get_NameTrigger(classId)).map(f => Seq(f)).getOrElse((Seq())) }

      domainCacheService.put(classOf[TriggerSeq],classId,TriggerSeq(triggers))
      triggers
    }
  }

  /**
   * определяем поля, на которые настроен триггер: делим строку по запятым
   */
  def getWatchFields(watchList: String): Set[String] = {
    Option(watchList)
      .map(wl => wl.split(",").map(s => s.trim).toSet.filterNot(_.isEmpty))
      .getOrElse(Set())
  }
  def get_NameTrigger(classId:String):Trigger={
    val metaClass = metaClassProvider.getClassMetadata(classId)
    Option(metaClass.aliasPattern).map( f=> {
      try{
       val compiledExpression = expressionEvaluator.compile(f,configProvider.create(),Map("this"->entityFactory.createEntityWithDataAndObject(metaClass,Map("ID"->"ID"))))
       Trigger(TriggerPeriod.After,Set(TriggerEvent.OnUpdate),compiledExpression.getUsedVariableFields("this"),null,classId,Set(),true,TriggerType.Name)
      }
      catch{
        case _ => null
      }
    }).orNull
  }
  private def getAllTriggers(): Seq[Trigger] = {
    val keyAllTriggers = "keyAllTriggers"
    if(!domainCacheService.isEmpty(classOf[TriggerSeq])) return  domainCacheService.get(classOf[TriggerSeq], keyAllTriggers).data
  //  import scala.collection.mutable.{ListBuffer => MutableListBuffer}

    val lock = Hazelcast.getLock("omed.triggers.Triggers")
    lock.lock()

    try {
      val sessionId = contextProvider.getContext.sessionId
      val triggers = scala.collection.mutable.ListBuffer[Trigger]()

      connectionProvider.withConnection {
        connection =>
          val resultSet =  DBProfiler.profile("_Meta.GetTriggers",execStatProvider) {DB.dbExec(connection,
            "[_Meta].[GetTriggers]", sessionId, null)}
          while (resultSet.next()) {
            // triggerPeriod: B (before) or A (after)
            val triggerPeriod = Option(resultSet.getString("TriggerPeriod")).getOrElse("").toLowerCase
            val isOnInsert = DBUtils.fromDbBoolean(resultSet.getString("IsOnInsert"))
            val isOnUpdate = DBUtils.fromDbBoolean(resultSet.getString("IsOnUpdate"))
            val isOnDelete = DBUtils.fromDbBoolean(resultSet.getString("IsOnDelete"))
            val watchList = resultSet.getString("WatchList")
            val excludedList = try{
              resultSet.getString("ExcludedList")
            }
            catch{
              case _ => null
            }
            val businessFunctionID = resultSet.getString("BusinesFunctionID")
            val classID = resultSet.getString("ClassID")
            val isNotInhereted =  DBUtils.fromDbBoolean(resultSet.getString("IsNotInhereted"))
            // get only ready triggers
            if (triggerPeriod != "" &&
              (isOnInsert || isOnUpdate || isOnDelete) &&
              businessFunctionID != null &&
              classID != null) {

              // определяем момент выполнения триггера
              val period = triggerPeriod match {
                case "b" => TriggerPeriod.Before
                case "a" => TriggerPeriod.After
                case _ => throw new Exception("Для триггера не указан момент вызова (before/after).")
              }

              // отбираем указанные для триггера события из возможных
              val events = Map(
                TriggerEvent.OnInsert -> isOnInsert,
                TriggerEvent.OnUpdate -> isOnUpdate,
                TriggerEvent.OnDelete -> isOnDelete).filter(_._2).keySet.toList.toSet

              // определяем поля, на которые настроен триггер
              val fields = getWatchFields(watchList)
              val excludedFields = getWatchFields(excludedList)
              val trigger = Trigger(
                period = period,
                events = events,
                watchList = fields,
                functionID = businessFunctionID,
                classID = classID,
                excludedList = excludedFields,
                isNotInhereted = isNotInhereted,
                triggerType = TriggerType.BF)

              triggers append trigger
            }
          }
          val data =triggers.toSeq
          domainCacheService.put(classOf[TriggerSeq], keyAllTriggers,TriggerSeq(data))
          triggers.toSeq
      }
    } finally {
      lock.unlock()
    }
  }
}
