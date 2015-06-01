package omed.bf.tasks

import omed.bf.ProcessTask

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 22.11.13
 * Time: 16:48
 * To change this template use File | Settings | File Templates.
 */
class CheckECP ( val objectExp:String,var destination: String = null,var fioVar:String = null,var dataVar :String = null) extends  ProcessTask ("_Meta_BFSCheckECP"){

}
object CheckECP{
  def apply(xml:scala.xml.Node) :CheckECP={
   // val destination =xml.attribute("Destination").map(_.text.replaceFirst("\\@", "")).orNull
    val objectExp = xml.attribute("SignedDocument").map(f=> f.toString()).getOrElse(null)
    new CheckECP(objectExp)
  }

}