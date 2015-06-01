package omed.bf.tasks

import omed.bf.{ClientTask, ProcessTask}


class OpenFileDialog(
  override val xml: scala.xml.Node
) extends ProcessTask("_Meta_BFSOpenFileDialog") with ClientTask

object OpenFileDialog {
  def apply(xml: scala.xml.Node): OpenFileDialog = {
    new OpenFileDialog(xml)
  }
}
