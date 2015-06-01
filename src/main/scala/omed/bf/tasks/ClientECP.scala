package omed.bf.tasks

import omed.bf.{ClientTask, ProcessTask}

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 23.09.13
 * Time: 18:22
 * To change this template use File | Settings | File Templates.
 */
class ClientECP (
               val name: String,
               val destination: String,
               val source: String
  ) extends ProcessTask("_Meta_BFSCreateECP") with ClientTask
{
  def xml = {
        <_Meta_BFSCreateECP
        Name={ name }
        Source={ source }
        Destination= {destination}/>
  }
}
  object ClientECP {
    def apply(source:String,destination:String): ClientECP = {
      new ClientECP("Создание цифровой подписи",destination,source)
    }
  }
