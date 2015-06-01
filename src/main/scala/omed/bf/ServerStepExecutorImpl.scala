package omed.bf

import com.google.inject.{Injector, Inject}
import handlers._
import omed.model.Value

class ServerStepExecutorImpl extends ServerStepExecutor {

  @Inject
  var injector:Injector = null

  @Inject
  var ExecStoredProcHandler:ExecStoredProcHandler = null
  @Inject
  var SetAttrValueHandler:  SetAttrValueHandler  = null
  @Inject
  var StateTransitionHandler : StateTransitionHandler   = null
  @Inject
  var SetValueHandler:  SetValueHandler     = null
  @Inject
  var CallAPIHandler : CallAPIHandler    = null
  @Inject
  var CreateObjectHandler:  CreateObjectHandler  = null
  @Inject
  var ArchiveFilesHandler:  ArchiveFilesHandler  = null
  @Inject
  var ExecJsHandler  : ExecJsHandler          = null
  @Inject
  var CloneObjectHandler : CloneObjectHandler     = null
  @Inject
  var CloneArrayHandler:CloneArrayHandler = null
  @Inject
  var GetServerValueHandler :GetServerValueHandler   = null
  @Inject
  var CreateDBFHandler : CreateDBFHandler             = null
  @Inject
  var CheckECPHandler :  CheckECPHandler    = null
  @Inject
  var CallBFHandler: CallBFHandler   = null
  @Inject
  var CreateByTemplate: CreateByTemplateHandler   = null
  @Inject
  var UpdateNameHandler: UpdateNameHandler   = null
  private lazy val handlers = {
        val steps = Seq(
             ExecStoredProcHandler,
             SetAttrValueHandler,
             StateTransitionHandler,
             SetValueHandler,
             CallAPIHandler ,
             CreateObjectHandler,
             ArchiveFilesHandler,
             ExecJsHandler,
             CloneObjectHandler ,
             CloneArrayHandler,
             GetServerValueHandler ,
             CreateDBFHandler,
             CheckECPHandler,
             CallBFHandler,
             CreateByTemplate,
             UpdateNameHandler
           )
       // steps.foreach(f => if(!f.isInstanceOf[CheckECPHandler]) injector.injectMembers(f))
     // steps.foreach(injector.injectMembers)
        steps
  }
  def execute(step: ProcessTask, context: Map[String, Value], processId:String): Map[String, Value] = {
    val sh = handlers.find(_.canHandle(step))
    sh.map(s => s.handle(step, context,processId)).orNull
  }

  def canHandle(step: ProcessTask): Boolean =
    handlers.exists(_.canHandle(step))

}