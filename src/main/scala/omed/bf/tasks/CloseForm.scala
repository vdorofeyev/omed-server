package omed.bf.tasks

import omed.bf.{ClientTask, ProcessTask}

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 20.09.13
 * Time: 16:27
 * To change this template use File | Settings | File Templates.
 */
class CloseForm extends ProcessTask("_Meta_BFSCloseForm") with ClientTask {
    def xml={
        <_Meta_BFSCloseForm/>
    }
}

object CloseForm{
 def apply:CloseForm={
   new CloseForm()
  }
}
