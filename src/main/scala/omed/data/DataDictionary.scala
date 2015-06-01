package omed.data

/**
 * @author artem
 *         Represents simple sorted map
 */
class DataDictionary(val keys: Seq[String], val data: Array[Any]) {
  private val reverseSeq = Map((keys zip keys.indices): _*)

  def apply(key: String) = data(reverseSeq(key))

  def get[T](key: String) = data(reverseSeq(key)).asInstanceOf[T]
}