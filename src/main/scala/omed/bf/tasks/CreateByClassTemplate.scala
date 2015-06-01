package omed.bf.tasks

import omed.bf.ProcessTask

/**
 * Created with IntelliJ IDEA.
 * User: naryshkinaa
 * Date: 04.04.14
 * Time: 14:13
 * To change this template use File | Settings | File Templates.
 */
class CreateByClassTemplate(
                             val templateClassTypeId:String,
                             val resultVariable :String,
                             val templateObjectExpr:String)
  extends ProcessTask("_Meta_BFSCreateByClassTemplate")

object CreateByClassTemplate{
  def apply(xml: scala.xml.Node): CreateByClassTemplate = {
    // опрелеляем параметры шага
    val templateClassTypeId = xml.attribute("TemplateClassTypeID").map(_.text).orNull
    val resultVariable = xml.attribute("ResultVariable").map(_.text.replaceFirst("\\@", "")).orNull
    val templateObjectExpr = xml.attribute("TemplateObject").map(_.text).orNull
    new CreateByClassTemplate(templateClassTypeId, resultVariable, templateObjectExpr)
  }
}
