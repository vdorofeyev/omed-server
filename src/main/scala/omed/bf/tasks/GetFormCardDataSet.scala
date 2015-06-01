package omed.bf.tasks

import ru.atmed.omed.beans.model.meta.{MetaCardField, MetaCard}
import omed.model.Value
import scala.xml.{Text, TopScope, Elem}
import omed.data.{DataTable, DataReaderService}
import java.util.TimeZone
import org.joda.time.DateTime
import java.text.SimpleDateFormat

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 18.09.13
 * Time: 17:33
 * To change this template use File | Settings | File Templates.
 */
class GetFormCardDataSet(val objExpr:String,val result:String) extends GetServerValue{
}

object GetFormCardDataSet{
  def apply(objExpr:String, result:String):GetFormCardDataSet={
     new GetFormCardDataSet(objExpr,Option(result).map(_.replaceFirst("\\@", "")).getOrElse((null)))
  }
}

