package omed.bf.tasks

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 20.09.13
 * Time: 12:13
 * To change this template use File | Settings | File Templates.
 */
class GetFormGridDataSet(val xmlDataVar:String,val treeIdVar:String,val treeDataVar:String, val result:String, val templateId:String) extends GetServerValue{

}
object GetFormGridDataSet{
  def apply(xmlDataVar:String, treeIdvar:String,treeDatavar:String, result:String, templateId:String):GetFormGridDataSet={
     new GetFormGridDataSet(Option(xmlDataVar).map(_.replaceFirst("\\@", "")).getOrElse((null)),
       Option(treeIdvar).map(_.replaceFirst("\\@", "")).getOrElse((null)),
       Option(treeDatavar).map(_.replaceFirst("\\@", "")).getOrElse((null)),
       Option(result).map(_.replaceFirst("\\@", "")).getOrElse((null)),
       templateId)
  }
}
