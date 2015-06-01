package omed.bf.tasks

import omed.bf.{ClientTask, ProcessTask}


class ClientPrint (
  val content: String
) extends ProcessTask("_Meta_BFSClientPrint") with ClientTask {
  def xml: scala.xml.Node = {
    <_Meta_BFSClientPrint
      Content={ content }/>
  }
}
