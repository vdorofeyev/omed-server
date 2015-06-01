package omed.TimerBF

import javax.ejb._
import javax.annotation.{PostConstruct, Resource}
import com.google.inject.{Injector, Guice, Inject}
import javax.annotation.Resource
import omed.model.MetaClassProvider
import omed.system.{ContextProvider, GuiceFactory}
import omed.data.{DataReaderService, DataReaderServiceImpl}
import javax.interceptor.Interceptors
import com.google.inject.servlet.RequestScoped
import java.lang.annotation.Annotation
import omed.bf._
import javax.servlet.http.HttpServletRequest
import java.util.TimerTask
import java.util.Timer

/**
 * Класс для работы отдельной БФ по расписанию
 */

class BFScheduler {

  @Inject
  var serverStepExecutor: ServerStepExecutor = null

  @Inject
  var businessFunctionExecutor: BusinessFunctionExecutor = null

  @Inject
  var systemSessionProvider:SystemSessionProvider = null

  @Inject
  var systemServletRequest:HttpServletRequest = null

  @Inject
  var injector:Injector  = null
  @Inject
  var businessFunctionThreadPool:BusinessFunctionThreadPool = null
  @Inject
  var validationWarningPool:ValidationWarningPool = null

  private var bfSchedulerMeta :BFSchedulerMeta = null
  private var mainObjectScope:GuiceObjectScope = null
  private var objScope: ObjectScope = null
  def init(bf:BFSchedulerMeta, mainScope:GuiceObjectScope,objScope:ObjectScope){
    bfSchedulerMeta = bf
    mainObjectScope = mainScope
    this.objScope = objScope
    val timer = new Timer()
    timer.scheduleAtFixedRate(new TimerTask() {
     override def run() {
       timeout(timer)
      }
    }, bfSchedulerMeta.timeout * 1000, bfSchedulerMeta.timeout * 1000)

  }
  private def timeout(t: Timer) {
    try {
     mainObjectScope.enter(objScope)
     systemSessionProvider.getProductionDomains.foreach(d => {
       try {
        // serverStepExecutor.asInstanceOf[ServerStepExecutorImpl].injector = injector
         validationWarningPool.clearPool
         businessFunctionThreadPool.clearPool
         systemServletRequest.asInstanceOf[SystemServletRequest].setSession(d,systemSessionProvider.getSystemSessionForDomain(d))
         businessFunctionExecutor.initFunctionInstance(bfSchedulerMeta.bfId)
       }
       catch {
         case _ =>
       }
     })
    }
    catch {
      case _ =>
    }
    finally {
      mainObjectScope.leave()
    }

  }
}
