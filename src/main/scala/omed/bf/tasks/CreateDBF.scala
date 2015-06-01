package omed.bf.tasks

import omed.bf.ProcessTask

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 17.10.13
 * Time: 11:50
 * To change this template use File | Settings | File Templates.
 */
class CreateDBF (val destination:String,
                  val objectExp:String,
                  val arrayName:String) extends  ProcessTask ("_Meta_BFSArrayToDBF"){

}
object CreateDBF{
  def apply(xml:scala.xml.Node) :CreateDBF={
    val destination =xml.attribute("Destination").map(_.text.replaceFirst("\\@", "")).orNull
    val objectExp = xml.attribute("Object").map(f=> f.toString()).getOrElse("@this")
    val arrayName = xml.attribute("ArrayName").get.toString
    new CreateDBF(destination,objectExp,arrayName)
  }

}
