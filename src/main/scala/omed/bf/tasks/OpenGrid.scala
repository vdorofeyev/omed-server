package omed.bf.tasks

import omed.bf.{ClientTask, ProcessTask}

class OpenGrid (
  val name: String,
  val target: String,
  val field: String
) extends ProcessTask("_Meta_BFSOpenGrid") with ClientTask {
  def xml = {
    if (target != null)
      <_Meta_BFSOpenGrid
        Name={ name }
        Object={ target }/>
    else
      <_Meta_BFSOpenGrid
        Name={ name }
        FieldID={ field }/>
  }
}

object OpenGrid {
  def apply(xml: scala.xml.Node): OpenGrid = {
    val name = xml.attribute("Name").map(_.text).orNull
    val obj = xml.attribute("Object").map(_.text).orNull
    val field = xml.attribute("FieldID").map(_.text).orNull
    new OpenGrid(name, obj, field)
  }
}

