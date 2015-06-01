package omed.bf.tasks

import omed.bf.{ClientTask, ProcessTask}


class GetClientValue (
  val destination: String,
  val expression: String
) extends ProcessTask("_Meta_BFSSetValue") with ClientTask {
  def xml: scala.xml.Node = {
    <_Meta_BFSSetValue
      Destination={ destination }
      SourceExp={ expression }/>
  }
}

object GetClientValue {
  def apply(xml: scala.xml.Node): GetClientValue = {
    val sourceExp = xml.attribute("SourceExp").map(_.text).orNull
    val dest = xml.attribute("Destination").map(_.text).orNull
    new GetClientValue(dest, sourceExp)
  }
}
