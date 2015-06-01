package omed.bf.handlers

import java.util.logging.Logger

import com.google.inject.Inject

import omed.bf.{BusinessFunctionStepLog, BusinessFunctionLogger, ProcessStepHandler, ProcessTask}
import omed.db.{DB, ConnectionProvider, DataAccessSupport}
import omed.system.ContextProvider
import omed.model.{ SimpleValue, Value, EntityInstance }
import omed.bf.tasks.ExecStoredProc
import omed.data.DataTable
import omed.cache.ExecStatProvider
import omed.lang.eval.DBUtils

/**
 * Реализация шага процесса выполнения хранимой процедуры.
 */
class ExecStoredProcHandler extends ProcessStepHandler
  with DataAccessSupport {
  /**
   * Логгер.
   */
  val logger = Logger.getLogger(classOf[ExecStoredProcHandler].getName())

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

  @Inject
  var businessFunctionLogger:BusinessFunctionLogger = null

  @Inject
  var execStatProvider:ExecStatProvider = null

  override val name = "_Meta_BFSExecSP"

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
   * @param step Описание шага процесса
   * @param context Текущий контекст
   * @return Контекст процесса
   */
  def handle(task: ProcessTask, context: Map[String, Value],processId:String): Map[String, Value] = {
    val targetTask = task.asInstanceOf[ExecStoredProc]
    val sessionId = contextProvider.getContext.sessionId

    // получим значения параметров из контекста по имени
    def byName(x: String) = getParamValue(context, x)
    val paramValues = targetTask.params.mapValues(x =>
      if (x != null && x.startsWith("@")) byName(x.replaceFirst("\\@", "")) else x)

    // получим данные от хранимой процедуры
    val hasOutput = !targetTask.outParams.isEmpty

    businessFunctionLogger.addLogStep(processId,new BusinessFunctionStepLog("Шаг Вызов хранимой процедуры" + targetTask.procedureName,context,paramValues.mapValues(f=>SimpleValue(f))))

    val data = execProc(targetTask.procedureName,
      paramValues, sessionId, hasOutput, targetTask.independent,targetTask.timeOut)


    // получим данные из результата ХП
    val lastResult = if (data != null) {
      def eval(x: String) = PlatformFunc.evaluate(x, data)
      targetTask.outParams.mapValues(eval)
    } else Map.empty[String, Value]

    if(data!=null) businessFunctionLogger.addLogStep(processId,new BusinessFunctionStepLog("Вызов хранимой процедуры после",Map(),lastResult))
    lastResult
  }

  /**
   * Получение значений параметров из текущего контекста
   * 
   * @param context Контекст, множество именнованных переменных
   * @param name Имя переменной
   * @return Объект, значение переменной из контекста.
   *         `EntityInstance` преобразуются в их идентификаторы
   */
  def getParamValue(context: Map[String, Object], name: String): AnyRef = {
    context.get(name).map(_ match {
      case i: EntityInstance => i.getId
      case x @ _ => x
    }).orNull
  }

  /**
   * Выполнить хранимую процедуру.
   * 
   * @param procedureName Имя хранимой процедуры
   * @param params Параметры хранимой процедуры
   * @param sessionId Идентификатор сессии
   * @param hasOutput Признак "Налчие результата выолнения хранимой процедуры"
   * @param independent Признак "Использовать новое соединение"
   * @return Таблица с данными
   */
  def execProc(
    procedureName: String, params: Map[String, Object], sessionId: String,
    hasOutput: Boolean, independent: Boolean,timeOut:Int =0): DataTable = {

    def getResult(connection: java.sql.Connection): DataTable = {
      logger.info(String.format("Executing server BF step %s in session %s, params: %s",
        name, sessionId, params.toString))

      val convertedParams = params.mapValues(DBUtils.platformValueToDb).toList

      val dbResult =
        //dataOperation {
        if (hasOutput)
          DB.dbExec(connection, procedureName, sessionId, convertedParams,execStatProvider,timeOut)
        else {
          DB.dbExecNoResultSet(connection, procedureName, sessionId, convertedParams,execStatProvider,timeOut)
          null
        }
    //  }

      if (dbResult != null) {
        dataOperation {
          val meta = dbResult.getMetaData()

          val columnSeq = for (i <- 1 to meta.getColumnCount())
            yield meta.getColumnName(i)
          def isBlob(i: Int) = {
            val columnType = meta.getColumnType(i)
            val blobTypes = Set(
              java.sql.Types.LONGVARBINARY,
              java.sql.Types.VARBINARY,
              java.sql.Types.BINARY,
              java.sql.Types.BLOB)
            blobTypes contains columnType
          }
          val binaries = for (i <- 1 to meta.getColumnCount() if isBlob(i)) yield i

          def fetchItem = (i: Int) =>
            if (isBlob(i)) dbResult.getBytes(i).asInstanceOf[Any]
            else dbResult.getObject(i).asInstanceOf[Any]

          val dataBuffer = scala.collection.mutable.Buffer[Array[Any]]()
          while (dbResult.next()) {
            val dataArr = Range(1, meta.getColumnCount() + 1)
              .map(fetchItem)
              .toArray

            dataBuffer += dataArr
          }
          new DataTable(columnSeq, binaries, dataBuffer.toSeq,Map())
        }
      } else null
    }

    if (independent) connectionProvider withSeparateConnection getResult
    else connectionProvider withConnection getResult
  }

}