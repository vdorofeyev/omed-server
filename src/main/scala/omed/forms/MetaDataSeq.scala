package omed.forms

/**
 * Класс для кеширования последовательности метаобъектов
 */
trait MetaDataSeq [A] {
   def data :Seq[A]
}
