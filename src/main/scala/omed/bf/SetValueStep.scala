package omed.bf

import com.google.inject.Inject
import omed.model.{SimpleValue, Value, EntityInstance, MetaClassProvider}
import xml.XML

class SetValueStep(val description: scala.xml.Node) extends ClientStep {

  @Inject var metaClassProvider: MetaClassProvider = null

  override def parseResults(clientMessage: String): Map[String, Value] = {
    val xml = description
    val destination = xml.attribute("Destination")
      .map(_.head).map(_.text).orNull
    val sourceExp = xml.attribute("SourceExp")
      .map(_.head).map(_.text).orNull

    super.parseResults(clientMessage)
  }
}
