package omed.db

import java.util.logging.Logger
import java.util.concurrent.ConcurrentHashMap
import scala.math
import java.util.concurrent.atomic.AtomicLong
import scala.collection.JavaConversions._
import omed.cache.ExecStatProvider

/**
 * Профилировка некоторых механизмов сервиса.
 */
object DBProfiler {

  case class Stat(val calls: Int,
             val totalTime: Long,
             val maxTime: Long)

  /**
   * Статистика вызовов.
   */
  val stats = new ConcurrentHashMap[String, Stat]
  /**
   * Общее количество вызывов.
   */
  val totalCalls = new AtomicLong()

  private val logger = Logger.getLogger("DBProfiler")

  /**
   * Подсчёт времени выполнения функции `f`
   * и запись результатов для последующиего анализа.
   * 
   * @param name Название функции 
   * @param f Функция
   * @return Результат функции `f`
   */
  def profile[A](name: String,execStatProvider:ExecStatProvider = null,isAppServer: Boolean = false)(f: => A): A = {

    val startTime = System.currentTimeMillis()
    if(execStatProvider!=null)  execStatProvider.addPredExecedBlock(name+startTime.toString)
    val result = f

    if(!isAppServer) record(name, System.currentTimeMillis() - startTime)
    if(execStatProvider!=null) {
        if(isAppServer)  execStatProvider.addExecedBlock(name,System.currentTimeMillis() - startTime,startTime)
        else execStatProvider.addExecedSP(name,System.currentTimeMillis() - startTime)
    }
    result
  }

  /**
   * Добавление информации о вызове.
   * 
   * @param name Название функции
   * @param millis Время выполенения, мс
   */
  def record(name: String, millis: Long) {
    // create or update statistics for given name
    var oldStat = stats.putIfAbsent(name, Stat(1, millis, millis))

    while (oldStat != null) {
      val newStat =
        Stat(oldStat.calls + 1, oldStat.totalTime + millis, math.max(oldStat.maxTime, millis))
      oldStat = if (stats.replace(name, oldStat, newStat))
        null
      else
        stats.get(name)
    }

    val total = totalCalls.incrementAndGet

    if (total % 1000 == 0)
      logger.info("DB statistics:\n" + printStatistics)
  }

  private def printStatistics: String = {
    String.format("%-40s %6s %6s %6s\n", "Name", "Calls", "Avg ms", "Max ms") +
    stats.toMap.map(x => x match {
      case (name, stat) => String.format("%-40s %6d %6d %6d",
        name,
        stat.calls.asInstanceOf[AnyRef],
        (stat.totalTime / stat.calls).asInstanceOf[AnyRef],
        stat.maxTime.asInstanceOf[AnyRef]) }
    ).toSeq.sorted.mkString("\n")
  }
}
