package omed.bf.tasks

import omed.bf.ProcessTask


class ExecStoredProc(
  val procedureName: String,
  val params: Map[String, String],
  val outParams: Map[String, String],
  val independent: Boolean = false,
  val timeOut:Int =0
) extends ProcessTask("_Meta_BFSExecSP")

object ExecStoredProc {
  def apply(xml: scala.xml.Node): ExecStoredProc = {
    // определяем имя хранимой процедуры
    val procedureName = xml.attribute("ProcedureName").get.toString

    // параметры вызова
    val paramNodes = xml \\ "Params" \\ "_Meta_SPParameter"
    val params = paramNodes.map(node => {
      val code = node.attribute("Code").map(_.text.replaceFirst("\\@", "")).orNull
      val sourceExp = node.attribute("SourceExp").map(_.text).orNull

      code -> sourceExp
    }).toMap

    // выходные параметры
    val outParamNodes = xml \\ "Params" \\ "_Meta_SPOutput"
    val outParams = outParamNodes.map(node => {
      val destination = node.attribute("Destination")
        .map(_.text.replaceFirst("\\@", "")).orNull
      val source = node.attribute("Source").map(_.text).orNull

      destination -> source
    }).toMap

    new ExecStoredProc(procedureName, params, outParams)
  }
}