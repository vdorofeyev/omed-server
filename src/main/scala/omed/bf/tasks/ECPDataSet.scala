package omed.bf.tasks

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 27.11.13
 * Time: 15:06
 * To change this template use File | Settings | File Templates.
 */
class ECPDataSet (val objExpr:String,val result:String) extends GetServerValue{

}
object ECPDataSet{
  def apply(objExpr:String, result:String):ECPDataSet={
    new ECPDataSet(objExpr,Option(result).map(_.replaceFirst("\\@", "")).getOrElse((null)))
  }
}

