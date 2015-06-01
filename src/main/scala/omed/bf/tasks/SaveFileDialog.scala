package omed.bf.tasks


import omed.bf.{ClientTask, ProcessTask}

class SaveFileDialog(
  override val xml: scala.xml.Node
) extends ProcessTask("_Meta_BFSSaveFileDialog") with ClientTask

object SaveFileDialog {
  def apply(xml: scala.xml.Node): SaveFileDialog = {
    new SaveFileDialog(xml)
  }
}