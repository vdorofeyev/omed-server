package omed.bf

import ru.atmed.omed.beans.model.meta.{MetaCardField, Metafield, MetaGrid, MetaCard}
import omed.data.{DataReaderService, DataTable}
import java.util.TimeZone
import java.text.SimpleDateFormat
import scala.xml.{XML, Text, TopScope, Elem}
import com.google.inject.Inject
import omed.system.ContextProvider
import org.joda.time.DateTime
import omed.model.{MetaField, DataType}
import omed.forms.MetaFormProvider

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 19.09.13
 * Time: 14:57
 * To change this template use File | Settings | File Templates.
 */
class DataSetBuilderImpl extends DataSetBuilder {
  @Inject
  var dataReader:DataReaderService = null
  @Inject
  var contextProvider:ContextProvider = null

  @Inject
  var metaFromProvider:MetaFormProvider = null

 val dateTimeOutFormat = new SimpleDateFormat(DataType.DateTimeFormat)

  val dateOutFormat =  new SimpleDateFormat(DataType.DateFormat)


  def getDataSetTable(name:String,metaCard:MetaCard, ids:Seq[String],reportTemplateId:String,withMeta:Boolean = true) :Seq[Elem]={
    val array = ids.map(dataReader.getCardData(_,true))
    val defaultData= array.map(f=> ( f.columns zip f.data(0)).toMap)

        getDataSetTable(name+"Default",metaCard.fields,defaultData,reportTemplateId,withMeta) ++
          metaCard.refGrids.map(f=> {
            val arrayRefGrid = ids.map(record=>  dataReader.getGridData(f.windowGridId,null,record,null,null,metaCard.viewCardId,null,null,null))

            getDataSetTable(name+f.metaCardGrid.arrayName,f.fields, arrayRefGrid.map(f => f.data).flatten.map(f =>  ( arrayRefGrid(0).columns zip f).toMap)  ,reportTemplateId,withMeta)
          }).flatten


  }

  def getTreeDataSetTable(fieldTypes:Seq[Metafield],data:String) :Elem={
    val fieldsMap =fieldTypes.map(f=> (f.getCodeName->f)).toMap
    val dataMap = if(data.length>0) {
      val  xml =   XML.loadString(data)
      xml.attributes.asAttrMap
    } else Map[String,String]()
    <ds name="Tree">
        <fields>
          {fieldsToXML(fieldTypes)}
        </fields>
      <data>
        <object>
          {dataToXML(fieldsMap,dataMap)}
        </object>
       </data>
    </ds>
  }

  def getDataSetTable(name:String,allFields:Seq[Metafield],data:Seq[Map[String,Any]],reportTemplateId:String,withMeta:Boolean):Seq[Elem]={
    val fields = allFields.filter(f => !isBinaryField(f))
    val fieldsMap = fields.map(f=> (f.getCodeName->f)).toMap
    val fieldsIDMap = fields.map(f=> (f.getViewFieldId->f)).toMap
    val mainDs =  <ds name={name}>
      {
      if(withMeta)
      <fields>
        {fieldsToXML(fields)}
      </fields>
      }
      <data>
        { data.map(f=>
        <object>
        {dataToXML(fieldsMap,f)}
        </object> )
        }
      </data>
    </ds>;
    val filtered = metaFromProvider.getReportFieldDetail(reportTemplateId).filter(f => fieldsIDMap contains(f.fieldId) )
    val tmp = filtered.map(f =>
    {
      // val columnIndex = data.columns.indexOf(fieldsIDMap(f.fieldId).getCodeName)
      val codeName = fieldsIDMap(f.fieldId).getCodeName
       getDataSetTable(name + fieldsIDMap(f.fieldId).getCodeName + "_",metaFromProvider.getMetaCard(null,f.viewCardId,isSuperUser = true),data.map( d => d(codeName).asInstanceOf[String]).filter(p => p!=null),reportTemplateId)
    }).flatten

    tmp ++ Seq(mainDs)
  }
  def fieldsToXML (fields:Seq[Metafield])={
   val t= fields.sortBy(f=>f.getCodeName).map(f=> if(hasDisplayName(f))
        <field code={f.getCodeName+"DisplayName"}  type="string" />
        <field code ={f.getCodeName} type={getFieldType(f)} />

    else <field code ={f.getCodeName} type={getFieldType(f)} />

    )
    t
  }
  def getFieldType(field:Metafield) :String={
    if(isRefField(field)) "guid"
    else field.getTypeCode
  }
  def hasDisplayName(metaField:Metafield):Boolean={
    metaField.getEditorType=="DropDown" || metaField.getTypeCode=="ref"
  }
  def isBinaryField(metaField:Metafield):Boolean={
     metaField.getTypeCode=="binary"
  }
  def isRefField(metaField:Metafield):Boolean={
     metaField.getTypeCode=="ref"
  }
  def isDateTime(metaField:Metafield):Boolean={
    metaField.getEditorType=="DateTime" || metaField.getTypeCode=="datetime"
  }
  def isDate(metaField:Metafield):Boolean={
    metaField.getEditorType=="Date" || metaField.getTypeCode=="date"
  }
  def dataToXML(fields:Map[String,Metafield],dataMap:Map[String,Any])={

    dateTimeOutFormat.setTimeZone(contextProvider.getContext.timeZone)

    val data = dataMap.filter(p=> p._2!= null)
    data.filter(p=> fields.contains(p._1)).map(f=>
    {
      val metaField = fields(f._1)
      val value = if(isDateTime(metaField))dateTimeOutFormat.format(DataType.parse(f._2.toString, DataType.DateTime))
      else if(isDate(metaField))dateOutFormat.format( DataType.parse(f._2.toString, DataType.Date))
      else f._2.toString
      val additionElem = if(isRefField(metaField)) {
        val value =
          if( data.get(metaField.getCodeName+"$").isDefined) Option(data(metaField.getCodeName+"$").toString)
          else {
               dataReader.getDisplayName(f._2.toString)
          }
         value.map(p => Seq(Elem(null,f._1+"DisplayName",null,TopScope,Text(p)))).getOrElse(Seq())
      }
      else Seq()

      Seq(Elem(null,f._1,null,TopScope,Text(value )))  ++ additionElem

    })
  }
}
