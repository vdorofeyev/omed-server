package omed.bf.handlers

import omed.bf.{ECPProvider, ConfigurationProvider, ProcessTask, ProcessStepHandler}
import omed.model._
import omed.bf.tasks.CheckECP

import com.google.inject.Inject
import omed.data.DataReaderService
import omed.model.services.ExpressionEvaluator
import java.security.Security


/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 22.11.13
 * Time: 16:46
 * To change this template use File | Settings | File Templates.
 */
class CheckECPHandler extends ProcessStepHandler {

  @Inject
  var dataReaderService : DataReaderService = null
//
//  @Inject
//  var expressionEvaluator: ExpressionEvaluator = null
//  @Inject
//  var configProvider: ConfigurationProvider = null
//
  @Inject
  var expressionEvaluator: ExpressionEvaluator = null
  @Inject
  var configProvider: ConfigurationProvider = null

  override val name = "_Meta_BFSCheckECP"

  //  def canHandle(step: ProcessTask) = {
  //    step.stepType == name
  //  }

  def handle(task: ProcessTask, context: Map[String, Value],processId:String): Map[String, Value] = {
     val targetTask = task.asInstanceOf[CheckECP]
     val resultVarName =  targetTask.destination.replaceFirst("\\@", "")
     val dataVarName =  targetTask.dataVar.replaceFirst("\\@", "")
     val fioVarName =  targetTask.fioVar.replaceFirst("\\@", "")
     val signObject = dataReaderService.getObjectData( objectId= expressionEvaluator.evaluate(targetTask.objectExp,configProvider.create(),context).getId)

     val signData = signObject("Sign").toString.getBytes
     val data =  signObject("Document").toString.getBytes
     val (verifyResult,fio) =  ECPProvider.CMSVerify(signData,null,data)


     Map(resultVarName->SimpleValue(verifyResult),dataVarName->SimpleValue(signObject("Document").toString),fioVarName->SimpleValue(fio))
  }

  /**
   * �������� CMS
   *
   * @param buffer �����
   * @param certs �����������
   * @param data ������
   * @throws Exception e
   */

}
