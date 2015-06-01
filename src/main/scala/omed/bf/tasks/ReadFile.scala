package omed.bf.tasks


import omed.bf.{ClientTask, ProcessTask}

class ReadFile (
  override val xml: scala.xml.Node
) extends ProcessTask("_Meta_BFSReadFile") with ClientTask

object ReadFile {
  def apply(xml: scala.xml.Node): ReadFile = {
    new ReadFile(xml)
  }
}