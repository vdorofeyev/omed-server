package omed.TimerBF

import javax.ejb.{Timer, Timeout, Startup, Singleton}
import com.google.inject.{Injector, Guice}
import omed.data.DataReaderService
import scala.collection.mutable.ArrayBuffer
import javax.annotation.PostConstruct
import omed.db.ConnectionProvider
import omed.system.GuiceFactory

/**
 * Класс для работы БФ по расписанию
 */
@Singleton
@Startup
class StartService {
  var timeServices :ArrayBuffer[BFScheduler] = ArrayBuffer[BFScheduler] ()
  var injector:Injector  = null
  var mainObjectScope:GuiceObjectScope = null
  val bfQuery =
    """
      | select BusinessFunctionID,Period from _Meta_TimerBF
    """.stripMargin
  @PostConstruct
  private def init() {
    val module = new ExampleModule()
    injector = GuiceFactory.getInjector.createChildInjector(module)//Guice.createInjector(module)    //
    mainObjectScope = module.getObjectScope

    getBF.foreach(f => {
      val objScope = new ObjectScope()
      mainObjectScope.enter(objScope)
      val bfScheduler =  injector.getInstance(classOf[BFScheduler])
      bfScheduler.init(f,mainObjectScope,objScope)
      timeServices+= bfScheduler
      mainObjectScope.leave()
    })
  }

  def getBF:Seq[BFSchedulerMeta]={
    mainObjectScope.enter(new ObjectScope())
    val connectionProvider =  injector.getInstance(classOf[ConnectionProvider])
    mainObjectScope.leave()
    connectionProvider.withConnection {
      connection =>
        val statement = connection.prepareStatement(bfQuery)
        val resultSet =statement.executeQuery()
        val bfs = new ArrayBuffer[BFSchedulerMeta]
        while (resultSet.next()) {
          bfs += BFSchedulerMeta(resultSet)
        }
        bfs.toSeq
    }
  }
}
