package omed.bf.handlers

import omed.bf._
import omed.model.{MetaField, SimpleValue, Value}
import omed.bf.tasks.{ECPDataSet, GetFormGridDataSet, GetFormCardDataSet, GetServerValue}
import com.google.inject.Inject
import omed.data.{DataTable, DataReaderService}
import omed.forms.MetaFormProvider
import omed.system.ContextProvider
import omed.bf.BusinessFunctionStepLog
import omed.model.services.ExpressionEvaluator
import scala.xml.XML
import ru.atmed.omed.beans.model.meta._
import ru.atmed.omed.beans.model.meta.NodeParameter
import ru.atmed.omed.beans.model.meta.FilterNode
import omed.bf.BusinessFunctionStepLog

/**
 * Получение DataSet-ов для одного или нескольких объектов
 */
class GetServerValueHandler extends ProcessStepHandler{
  @Inject
  var dataReader:DataReaderService = null

  @Inject
  var metaFromProvider:MetaFormProvider = null

  @Inject
  var businessFunctionLogger:BusinessFunctionLogger = null

  @Inject
  var contextProvider :ContextProvider = null
  @Inject
  var dataSetBuilder :DataSetBuilder = null

  @Inject
  var expressionEvaluator: ExpressionEvaluator = null

  @Inject
  var configProvider: ConfigurationProvider = null

  override val name = "ServerValueInitialize"
  def handle(task: ProcessTask, context: Map[String, Value],processId:String): Map[String, Value] = {
   // val targetTask = task.asInstanceOf[GetFormCardDataSet]
    task match {
      case e:ECPDataSet => {
        var recordId= expressionEvaluator.evaluate(e.objExpr,configProvider.create(),context).getId//context(e.varName).getId
        val metaCard = metaFromProvider.getMetaCard(recordId,isSuperUser=true)
        val data = dataReader.getCardData(recordId,true)
        val xml =
          <reportData>
            {
              dataSetBuilder.getDataSetTable("",metaCard,Seq(recordId),null,false)
           // Seq(  dataSetBuilder.getDataSetTable("Default",metaCard.fields,data,null,false)) ++   metaCard.refGrids.filter(p=> p.classId !="CC947029-B264-4DEC-B1D2-83B4D88B29D5").map(f=> dataSetBuilder.getDataSetTable(f.arrayName,f.fields,dataReader.getGridData(f.getWindowsGridId,null,recordId,null,null,metaCard.viewCardId,null,null,null),null,false))
            }
          </reportData>;
        val result = xml.toString()

        businessFunctionLogger.addLogStep(processId,new BusinessFunctionStepLog("Шаг создания DataSet на сервере приложений" ,context,Map("var"->SimpleValue(e.result),"result"->SimpleValue(result))))
        Map(e.result->SimpleValue(result))
      }

//      case e:GetFormCardDataSet => {
//        var recordId= expressionEvaluator.evaluate(e.objExpr,configProvider.create(),context).getId//context(e.varName).getId
//           val metaCard = metaFromProvider.getMetaCard(recordId)
//           val data = dataReader.getCardData(recordId)
//         val xml =
//          <reportData>
//            {
//            Seq(  dataSetBuilder.getDataSetTable("Default",metaCard.fields,data)) ++
//              metaCard.refGrids.map(f=>dataSetBuilder.getDataSetTable(f.arrayName,f.fields,dataReader.getGridData(f.getWindowsGridId,null,recordId,null,null,metaCard.viewCardId,null,null,null)))
//            }
//          </reportData>;
//        val result = xml.toString()
//
//        businessFunctionLogger.addLogStep(processId,new BusinessFunctionStepLog("Шаг создания DataSet на сервере приложений" ,context,Map("var"->SimpleValue(e.result),"result"->SimpleValue(result))))
//        Map(e.result->SimpleValue(result))
//      }
      case e:GetFormGridDataSet => {
          val ids = getObjectSeqFromClientXML(context(e.xmlDataVar).toString)
          if(ids.length == 0) throw new RuntimeException("Не выбрано ни одного объекта")
          val metaCard = metaFromProvider.getMetaCard(ids(0),isSuperUser=true)
//          val array = ids.map(dataReader.getCardData)
//          val defaultData = new DataTable(array(0).columns,Seq(),array.map(f=>f.data).flatten)
           val xml =
          <reportData>
            {
              val treeDs = if(context.contains(e.treeIdVar) && context(e.treeIdVar)!=null && context(e.treeIdVar).toString.length>0) Seq(dataSetBuilder.getTreeDataSetTable(getTreeMeta(context(e.treeIdVar).getId),context(e.treeDataVar).toString)) else Seq()
//              dataSetBuilder.getDataSetTable("Default", metaCard.fields,defaultData,e.templateId) ++
//                metaCard.refGrids.map(f=> {
//                   val arrayRefGrid = ids.map(record=>  dataReader.getGridData(f.getWindowsGridId,null,record,null,null,metaCard.viewCardId,null,null,null))
//                   dataSetBuilder.getDataSetTable(f.arrayName,f.fields,new DataTable(arrayRefGrid(0).columns,Seq(),arrayRefGrid.map(f=>f.data).flatten),e.templateId)
//                 }).flatten ++ treeDs
              dataSetBuilder.getDataSetTable("",metaCard,ids,e.templateId) ++ treeDs
            }
          </reportData>;
        val result = xml.toString()

        businessFunctionLogger.addLogStep(processId,new BusinessFunctionStepLog("Шаг создания DataSet на сервере приложений" ,context,Map("var"->SimpleValue(e.result),"result"->SimpleValue(result))))
        Map(e.result->SimpleValue(result))
      }
    }
  }

  def getTreeMeta(treeId:String):Seq[Metafield]={
     val tree = dataReader.getTreeFilter(treeId)
    val parameters = tree.map(f=> f.data).flatten
    parameters.map(f=> MetafieldImpl(null,f.varName,f.editorType,f.typeTypeCode,f.typeExtInfo))
  }
  def getObjectSeqFromClientXML(xmlString:String):Seq[String]={
     val xml =   XML.loadString("<root>"+xmlString + "</root>")
     val ids = (xml\ "object").map(f=>f.attribute("ID").map(p=>p.text).get)
     ids
  }
}
