package omed.bf.tasks

import omed.bf.ProcessTask


/**
 * Шаг Присвоить значение
 * @param destination Внутренняя переменная, которой присваиваем значение
 * @param sourceExp Выражение-источник
 */
class SetValue (
  val destination: String,
  val sourceExp: String
) extends ProcessTask("_Meta_BFSSetValue")

object SetValue {
  def apply(xml: scala.xml.Node): SetValue = {
    val destination = xml.attribute("Destination")
      .map(_.head).map(_.text).orNull
    val sourceExp = xml.attribute("SourceExp")
      .map(_.head).map(_.text).orNull

    new SetValue(destination, sourceExp)
  }
}
