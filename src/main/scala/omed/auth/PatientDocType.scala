package omed.auth


/**
 * Тип документа, по которому производится аутентификация пациента.
 */
object PatientDocType extends Enumeration {
  val Snils = Value("snils")
  val Password = Value("pass")
  val Policy = Value("policy")
}