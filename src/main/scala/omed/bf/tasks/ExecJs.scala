package omed.bf.tasks

import omed.bf.ProcessTask


class ExecJs(
  val script: String,
  val outParams: Map[String, String]
) extends ProcessTask("_Meta_BFSjs")

object ExecJs {
  def apply(xml: scala.xml.Node): ExecJs = {
    // определяем имя хранимой процедуры
    val script = xml.attribute("JavaScriptText").map(_.text).orNull
    val replacedScript =
      if (script!=null)
        script.replace("$CallBF","$System.CallBF").replace("$Delete","$System.Delete")
          .replace("$Edit","$System.Edit").replace("$XMLGetAttr","$System.XMLGetAttr")
          .replace("$WindowGridData","$System.WindowGridData").replace("$ClassData","$System.ClassData")
          .replace("$Log","$System.Log")
      else null
    // выходные параметры
    val outParamNodes = xml \\ "Params" \\ "_Meta_jsParameter"
    val outParams = outParamNodes.map(node => {
      val destination = node.attribute("BFParameter")
        .map(_.text.replaceFirst("\\@", "")).orNull
      val source = node.attribute("jsParameter").map(_.text).orNull

      source -> destination
    }).toMap

    new ExecJs(replacedScript, outParams)
  }
}