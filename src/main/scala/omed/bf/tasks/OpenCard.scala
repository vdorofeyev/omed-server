package omed.bf.tasks



import omed.bf.{ClientTask, ProcessTask}

class OpenCard (
val name: String,
val target: String
) extends ProcessTask("_Meta_BFSOpenCard") with ClientTask {
def xml = {
<_Meta_BFSOpenCard
Name={ name }
Object={ target }/>
}
}

object OpenCard {
  def apply(xml: scala.xml.Node): OpenCard = {
    val name = xml.attribute("Name").map(_.text).orNull
    val obj = xml.attribute("Object").map(_.text).orNull
    new OpenCard(name, obj)
  }
}
