package omed.model


class StatusValidator (val statusId: String, val validatorId: String) extends Serializable

case class StatusValidatorSeq(data: Seq[StatusValidator])

class StatusInputValidator (val statusId: String, val validatorId: String) extends Serializable

case class StatusInputValidatorSeq(data: Seq[StatusInputValidator])

class TransitionValidator (val transitionId: String, val validatorId: String) extends Serializable

case class TransitionValidatorSeq(data: Seq[TransitionValidator])

class BFValidator(val bfId:String, val validatorId:String)extends  Serializable

case class BFValidatorSeq(data:Seq[BFValidator])

