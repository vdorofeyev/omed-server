package omed.bf.tasks

import omed.bf.ProcessTask

class CallAPI(
  val functionName: String,
  val params: Map[String, String]
) extends ProcessTask("_Meta_BFSCallAPI")

object CallAPI {
  def apply(xml: scala.xml.Node): CallAPI = {
    // определяем имя хранимой процедуры
    val functionName = xml.attribute("FunctionName").get.toString

    // параметры вызова
    val paramNodes = xml \\ "Params" \\ "_Meta_CallAPIParameter"
    val params = paramNodes.map(node => {
      val code = node.attribute("Code").map(_.text.replaceFirst("\\@", "")).orNull
      val sourceExp = node.attribute("SourceExp").map(_.text).orNull

      code -> sourceExp
    }).toMap

    new CallAPI(functionName, params)
  }
}