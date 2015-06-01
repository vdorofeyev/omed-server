package omed.bf.tasks

import omed.bf.{ClientTask, ProcessTask}

class OpenFolderDialog (
  val caption: String,
  val destination: String
) extends ProcessTask("_Meta_BFSOpenFolderDialog") with ClientTask {
  def xml = {
    <_Meta_BFSOpenFolderDialog
      Caption={ caption }
      Destination={ destination }/>
  }
}

object OpenFolderDialog {
  def apply(xml: scala.xml.Node): OpenFolderDialog = {
    val caption = xml.attribute("Caption").map(_.text).orNull
    val destination = xml.attribute("Destination").map(_.text).orNull
    new OpenFolderDialog(caption, destination)
  }
}

