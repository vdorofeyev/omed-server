package omed.bf.handlers

import java.util.logging.Logger

import com.google.inject.Inject

import omed.bf._
import omed.db.{DB, ConnectionProvider, DataAccessSupport}
import omed.system.ContextProvider
import omed.model._
import omed.bf.tasks.CreateObject
import omed.data.{EntityFactory, DataWriterService, DataTable}
import omed.errors.MetaModelError
import omed.model.services.ExpressionEvaluator
import omed.lang.eval.DBUtils

/**
 * Реализация шага процесса выполнения хранимой процедуры.
 */
class CreateObjectHandler extends ProcessStepHandler
  with DataAccessSupport {
  /**
   * Логгер.
   */
  val logger = Logger.getLogger(classOf[CreateObjectHandler].getName())

  /**
   * Фабрика подключений к источнику данных.
   */
  @Inject
  var connectionProvider: ConnectionProvider = null
  /**
   * Менеджер контекстов, через который можно получить
   * текущий контекст.
   */
  @Inject
  var contextProvider: ContextProvider = null

  /**
   * Провайдер для записи данных в хранилище
   */
  @Inject
  var dataWriterService: DataWriterService = null

  @Inject
  var metaClassProvider: MetaClassProvider = null
  @Inject
  var expressionEvaluator: ExpressionEvaluator = null
  @Inject
  var configProvider: ConfigurationProvider = null
  @Inject
  var entityFactory:EntityFactory = null

  @Inject
  var businessFunctionLogger:BusinessFunctionLogger = null

  @Inject
  var model:MetaModel = null
  override val name = "_Meta_BFSCreateObject"

  /**
   * Проверка возможности выполнения данного шага.
   *
   * @param step Задача на обработку
   * @return `Правда` | `Ложь`
   */
//  def canHandle(step: ProcessTask) = {
//    step.stepType == name
//  }

  /**
   * Обработать шаг.
   *
   * @param context Текущий контекст
   * @return Контекст процесса
   */
  def handle(task: ProcessTask, context: Map[String, Value],processId:String): Map[String, Value] = {
    val targetTask = task.asInstanceOf[CreateObject]

    // получаем мета-класс по коду или идентификатору
    val classSource = if (targetTask.sourceClassID == null) {
       val expressionResult = expressionEvaluator.evaluate(targetTask.source, configProvider.create(), context)
       expressionResult match {
            case v: SimpleValue => v.data.toString
            case v: EntityInstance => v.getId
            case _ => null
       }
    } else  targetTask.sourceClassID
    val metaClass =
      try {
        model.getObjectById(classSource)
      } catch {
        case _ => throw new MetaModelError("No such class [" + classSource + "]")
      }

    // создаем объект и возвращаем пустой объект

    val newObj = dataWriterService.addRecord(metaClass.id)
    val fields = targetTask.codeExpressionMap.mapValues(exp => {
      val value = DBUtils.platformValueToDb(expressionEvaluator.evaluate(exp, configProvider.create(), context))
      if(value==null) null else value.toString
    })
    businessFunctionLogger.addLogStep(processId,new BusinessFunctionStepLog("Шаг создания объекта",context,Map("newObject"->newObj,"class"->SimpleValue(metaClass.code),"копируемые поля"->SimpleValue(targetTask.codeExpressionMap.toString()))))
    dataWriterService.editRecord(newObj,fields)

    Map(targetTask.destination -> newObj)
  }

}