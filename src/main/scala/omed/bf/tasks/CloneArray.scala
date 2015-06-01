package omed.bf.tasks

import omed.bf.ProcessTask

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 17.09.13
 * Time: 15:30
 * To change this template use File | Settings | File Templates.
 */
class CloneArray (
  var destination:String ,
  val source:String,
  val arrayName:String)
  extends ProcessTask("_Meta_BFSCloneArray"){
  }
object CloneArray{
    def apply(xml: scala.xml.Node): CloneArray = {
      // опрелеляем параметры шага
      val source = xml.attribute("Source").map(_.text).orNull
      val destination = xml.attribute("Destination").map(_.text).orNull
      val arrayName =  xml.attribute("ArrayName").map(_.text).orNull
      new CloneArray(destination,source,arrayName)
    }
}
