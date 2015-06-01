package omed.bf

import xml.{Elem, XML}
import omed.model.{SimpleValue, Value}

class ClientStep {
  /**
   * Parse XML to dictionary of variables.
   * Elements under &lt;data&gt; are variables
   * and their inner texts are values
   * @param clientMessage source xml string with root &lt;data&gt; tag.
   */
  def parseResults(clientMessage: String): Map[String, Value] = {
    val xml = XML.loadString(clientMessage)

    val stepError = xml \\ "stepError"
    if (!stepError.isEmpty && stepError.text.toLowerCase == "true") {
      throw new RuntimeException("Step error message found")
    }

    val data = xml \\ "data"

    if (data.isEmpty)
      Map.empty
    else data.head.child.filter(node => node.isInstanceOf[Elem]).
      map(node => (node.label, SimpleValue(node.child map (_.toString) mkString))).toMap
  }
}
