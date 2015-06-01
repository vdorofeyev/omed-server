package omed.bf.tasks

import omed.bf.ProcessTask


class SetAttributeValue(
  val source: String,
  val destination: String
)  extends ProcessTask("_Meta_BFSSetAttributeValue") {
   override def description:String = {
     stepType + "\n Source: " + source + "\n Destiantion: "+ destination
   }

}

object SetAttributeValue {
  def apply(xml: scala.xml.Node): SetAttributeValue = {
    // Destination="@this.PatientFIO"
    val destination = xml.attribute("Destination")
      .map(_.head).map(_.text).orNull

    // SourceExp="@this.PatientID.LastName + &quot; &quot; + SUBSTRING(@this.PatientID.FirstName, 1, 1)
    // + &quot;. &quot; + SUBSTRING(@this.PatientID.SecondName, 1, 1) +
    // &quot;. &quot;"
    val sourceExp = xml.attribute("SourceExp")
      .map(_.head).map(_.text).orNull

    new SetAttributeValue(sourceExp, destination)
  }
}