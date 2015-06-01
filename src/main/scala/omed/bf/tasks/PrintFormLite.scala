package omed.bf.tasks


import omed.bf.{ClientTask, ProcessTask}

class PrintFormLite (
  val templateId:String,
  val dataSetVar:String
) extends ProcessTask("_Meta_BFSPrintFormLite") with ClientTask {
  def xml = {
    <_Meta_BFSPrintFormLite TemplateID ={templateId} DataSet ={dataSetVar}/>
  }
}
object PrintFormLite {
  def apply(xml: scala.xml.Node, dataSetVar:String): PrintFormLite = {
    val templateId = xml.attribute("TemplateID").map(_.text).orNull
    new PrintFormLite(templateId,dataSetVar)
  }
}
