package omed.bf

class BusinessFunction(
  val id: String,
  val name: String,
  val steps: Seq[FunctionStep],
  val resultName: String = null,
  val params:Seq[FunctionParameter]= Seq.empty[FunctionParameter]
                        ) extends Serializable
