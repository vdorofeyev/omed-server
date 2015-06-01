package omed.bf.tasks

import omed.bf.ProcessTask

/**
 * Created with IntelliJ IDEA.
 * User: naryshkinaa
 * Date: 23.04.14
 * Time: 15:57
 * To change this template use File | Settings | File Templates.
 */
class UpdateNameTask (val expression:String) extends ProcessTask("_Meta_BFSUpdateName")

  object UpdateNameTask {
    def apply(xml: scala.xml.Node): UpdateNameTask = {
      val expression = xml.attribute("Expression")
        .map(_.head).map(_.text).orNull
      new UpdateNameTask(expression)
    }
  }