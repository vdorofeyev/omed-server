package omed.bf.tasks

import omed.bf.ProcessTask

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 16.09.13
 * Time: 12:31
 * To change this template use File | Settings | File Templates.
 */
class CloneObject (
             var destination:String ,
             val source:String,
             val resultVar:String) extends ProcessTask("_Meta_BFSCloneObject"){
}
object CloneObject{
    def apply(xml: scala.xml.Node): CloneObject = {
      // опрелеляем параметры шага
      val source = xml.attribute("Source").map(_.text).orNull
      val destination = xml.attribute("Destination").map(_.text).orNull
      val resultVar = xml.attribute("ResultVariable").map(_.text.replaceFirst("\\@", "")).orNull
      new CloneObject(destination,source,resultVar)
    }
}

