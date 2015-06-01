package omed.bf.handlers

import omed.bf._
import omed.model.{MetaClassProvider, SimpleValue, Value}
import omed.bf.tasks.CreateDBF
import com.google.inject.Inject
import omed.data.{DataTable, DataReaderService}
import omed.model.services.ExpressionEvaluator
import omed.bf.BusinessFunctionStepLog
import omed.forms.MetaFormProvider
import ru.atmed.omed.beans.model.meta.{MetaGridColumn, Metafield}
import java.io.{FileOutputStream, ByteArrayOutputStream}
import net.iharder.Base64
import com.linuxense.javadbf.{DBFWriter, DBFField}

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 17.10.13
 * Time: 11:58
 * To change this template use File | Settings | File Templates.
 */
class CreateDBFHandler extends ProcessStepHandler{

  @Inject
  var businessFunctionLogger : BusinessFunctionLogger = null
  @Inject
  var dataReaderService: DataReaderService = null
  @Inject
  var expressionEvaluator: ExpressionEvaluator = null
  @Inject
  var configProvider: ConfigurationProvider = null
  @Inject
  var metaClassProvider: MetaClassProvider = null
  @Inject
  var metaFormProvider: MetaFormProvider = null

  override val name = "_Meta_BFSArrayToDBF"
  def handle(step: ProcessTask, context: Map[String, Value],processId:String): Map[String, Value]={
   val task = step.asInstanceOf[CreateDBF]
    val objectID = expressionEvaluator.evaluate( task.objectExp, configProvider.create(),context).getId
    val array = dataReaderService.getCollectionByArrayName(task.arrayName,objectID)
    val grid =  metaFormProvider.getMetaCard(objectID).refGrids.find(p=>p.metaCardGrid.arrayName==task.arrayName)
    if(grid.isEmpty) throw new RuntimeException("не найден связанный грид для arrayName: " + task.arrayName)
    val  dbfData = arrayToDBF(array,grid.get.fields)
    businessFunctionLogger.addLogStep(processId,new BusinessFunctionStepLog("Шаг создание DBF файла",context,Map()))
    Map(task.destination->SimpleValue(dbfData))
  }
  def arrayToDBF(data:DataTable,fields:Seq[Metafield]):String={
    val byteArray = new ByteArrayOutputStream()
    val filteredColumns =  fields.filter(p=> p.asInstanceOf[MetaGridColumn].isVisible)
    val dbfFields =filteredColumns.map(f=> getDBFField(f) ).toArray
    val columnToIndexMap = filteredColumns.map(f=>f.getCodeName->data.columns.indexOf(f.getCodeName)).toMap

    val writer = new DBFWriter()
    writer.setFields( dbfFields)
    data.data.foreach(f=> {
      val obj = filteredColumns.map(p=> {
      val value =f(columnToIndexMap(p.getCodeName))
      if(value !=null)
        f(columnToIndexMap(p.getCodeName)) match {
          case i: java.lang.Integer => i.toDouble.asInstanceOf[AnyRef]
          case d: java.math.BigDecimal => d.doubleValue().asInstanceOf[AnyRef]
          case e :Any => e.asInstanceOf[AnyRef]
        }
      else null
      }).toArray
      // filteredColumns.map(p=> "test Record".asInstanceOf[AnyRef]).toArray
      writer.addRecord(obj)
    })
   // writer.addRecord( rowData)
    writer.write(byteArray)
    Base64.encodeBytes(byteArray.toByteArray())
  }
  def getDBFField(field:Metafield) :DBFField={
    val result = new DBFField()
  //  result.
    result.setName(if(field.getCodeName.length>10) field.getCodeName.substring(0,9) else field.getCodeName )
//    result.setDataType(DBFField.FIELD_TYPE_C)
//    result.setFieldLength(32)
//    return result
    field.getEditorType match {
      case "DropDown" => {
         result.setDataType(DBFField.FIELD_TYPE_C)
         result.setFieldLength(32)}
      case "Date" => {
        result.setDataType(DBFField.FIELD_TYPE_D)
      }
      case "DateTime" => {
        result.setDataType(DBFField.FIELD_TYPE_D)
      }
      case "Auto" =>{
         field.getTypeCode match {
           case "int" => {
             result.setDataType(DBFField.FIELD_TYPE_N)
             result.setFieldLength(8)
             result.setDecimalCount(0)
           }
           case "numeric" => {
             result.setDataType(DBFField.FIELD_TYPE_F)
             result.setFieldLength(19)
             result.setDecimalCount(4)
           }
           case "ref" => {
             result.setDataType(DBFField.FIELD_TYPE_C)
             result.setFieldLength(32)
           }
           case "date" => {
             result.setDataType(DBFField.FIELD_TYPE_D)
           }
           case "datetime" => {
             result.setDataType(DBFField.FIELD_TYPE_D)
           }
           case "string" => {
             result.setDataType(DBFField.FIELD_TYPE_C)
             val length = field.getTypeExtInfo.toInt
             result.setFieldLength( if(length==0) 2000 else length )
           }
           case _ => {
             result.setDataType(DBFField.FIELD_TYPE_C)
             result.setFieldLength(255)
           }
         }
      }
      case _ => {
        result.setDataType(DBFField.FIELD_TYPE_C)
        result.setFieldLength(255)
      }
    }
    result
  }
}
