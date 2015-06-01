package omed.bf.tasks

import omed.bf.{ClientTask, ProcessTask}

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 26.11.13
 * Time: 16:31
 * To change this template use File | Settings | File Templates.
 */
class ClientCheckECP ( override val xml: scala.xml.Node
                       ) extends ProcessTask("_Meta_BFSClientCheckECP") with ClientTask {

}

object ClientCheckECP {

  def apply(xml: scala.xml.Node): ClientCheckECP = {
    new ClientCheckECP(xml)
  }
  def apply(ResultVar: String, FioVar: String, DataVar: String): ClientCheckECP = {
    new ClientCheckECP(<_Meta_BFSClientCheckECP
      Result={ ResultVar }
      Fio={ FioVar }
      Data= {DataVar}/>)
  }

}