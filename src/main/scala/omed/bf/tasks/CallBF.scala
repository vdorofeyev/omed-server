package omed.bf.tasks

import omed.bf.ProcessTask

/**
 * Created by andrejnaryskin on 17.03.14.
 */
class CallBF (val expression:String,val bf:String,val inParams: Map[String, String],val outParams: Map[String, String])  extends ProcessTask("_Meta_BFSCallBF") {

}

object CallBF {
  def apply(xml: scala.xml.Node): CallBF = {
    // определяем имя хранимой процедуры
    val expression = xml.attribute("Exp")
      .map(_.head).map(_.text).orNull
    val bf = xml.attribute("CalledBusinessFunctionID")
      .map(_.head).map(_.text).orNull
    // параметры вызова
    val inParamNodes = xml \\ "Params" \\ "_Meta_CallBFSInputParameter"
    val inParams = inParamNodes.map ( node => {
      val subVar = node.attribute("VarNameSubBF").map(_.text.replaceFirst("\\@", "")).orNull
      val bfVar = node.attribute("VarNameParentBF").map(_.text.replaceFirst("\\@", "")).orNull
      bfVar -> subVar
    } ).toMap
    val outParamNodes = xml \\ "Params" \\ "_Meta_CallBFSOutputParameter"
    val outParams = outParamNodes.map(node => {
      val subVar = node.attribute("VarNameSubBF").map(_.text.replaceFirst("\\@", "")).orNull
      val bfVar = node.attribute("VarNameParentBF").map(_.text.replaceFirst("\\@", "")).orNull
      bfVar -> subVar
    }).toMap

    new CallBF(expression, bf,inParams,outParams)
  }
}
