package omed.bf.tasks

import omed.bf.ProcessTask


class CreateObject(
  val source: String,
  val sourceClassID: String,
  val destination: String,
  val codeExpressionMap:Map[String,String]
) extends ProcessTask("_Meta_BFSCreateObject")

object CreateObject {
  def apply(xml: scala.xml.Node): CreateObject = {
    // опрелеляем параметры шага
    val source = xml.attribute("Source").map(_.text).orNull
    val sourceClassID = xml.attribute("SourceClassID").map(_.text).orNull
    val destination = xml.attribute("Destination").map(_.text.replaceFirst("\\@", "")).orNull
    val paramNodes = xml \\ "Params" \\ "_Meta_CreateObjectParameter"
    val codeExpressionMap = paramNodes.map(node => {
      val code = node.attribute("ToPropertyCode").map(_.text.replaceFirst("\\@", "")).orNull
      val exp = node.attribute("Expression").map(_.text).orNull
      code -> exp
    }).toMap
    new CreateObject(source, sourceClassID, destination,codeExpressionMap)
  }
  def apply(source:String,classId:String,destination:String): CreateObject = {
       new CreateObject(source,classId,Option(destination).map(_.replaceFirst("\\@", "")).orNull,Map())
  }
}