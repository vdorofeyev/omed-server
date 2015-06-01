package omed.model.services

import omed.model._
import com.google.inject.Inject
import omed.lang.parsers.CalcExpressionParser
import omed.lang.compilers.ExpressionCompiler
import omed.lang.struct._
import omed.lang.eval.{Configuration}
import omed.data.{EntityFactory, DataTable, DataReaderService}
import omed.model.MetaModel
import com.sun.org.apache.xml.internal.utils.MutableAttrListImpl
import scala.collection.mutable.ListBuffer
import java.util.UUID
import omed.system.ContextProvider
import omed.db.DBProfiler
import omed.cache.ExecStatProvider
import java.sql.Timestamp
import omed.bf.ConfigurationProvider
import omed.lang.struct.SQLAlias
import omed.model.MetaModel


class ExpressionEvaluator {
  @Inject
  var metaClassProvider: MetaClassProvider = null
  @Inject
  var dataReader: DataReaderService = null
  @Inject
  var contextProvider:ContextProvider = null
  @Inject
  var execStatProvider: ExecStatProvider = null
  @Inject
  var entityFactory:EntityFactory = null
  @Inject
  var configProvider: ConfigurationProvider = null
  @Inject
  var model:MetaModel = null
  @Inject
  var entityDataProvider:EntityDataProvider = null

  lazy val currentConfig = configProvider.create()

  def compile(expression: String, config: Configuration, variables: Map[String, Value]
             ): Expression = {
    val parsed = CalcExpressionParser.parse(expression)
    val compiled = ExpressionCompiler.prepare(model, config, parsed, variables.mapValues(_ match {
      case e: EntityInstance => e.obj.code
      case v: SimpleValue => v.dataType.toString
      case s: SQLExpr => "_SQL_" + s.joins.last.alias.obj.code
    }))
    compiled
  }

  def evaluate(expression: Expression, config: Configuration, variables: Map[String, Value]): Value = {
  //  val model = new LazyMetaModel(metaClassProvider)

//    val queryExecutor = new DbQueryExecutor(model)
//    val expressionDataProvider = new ExpressionDataProviderImpl((model)
//    val refreshedValues = variables.mapValues(_ match {
//      case e: EntityInstance if e.data.size == 1 && e.data.contains("ID") =>
//        queryExecutor.getEntityInstance(e.obj.code, e.getId)
//      case v @ _ => v
//    })
//    // вычисляем значения всех промежуточных выражений по ссылке через несколько объектов
//    val queriedContext = new InlineQueryEvaluator(model, queryExecutor).
//      evaluateInlineQueries(expression, refreshedValues)
//    val mergedContext = refreshedValues ++ queriedContext

    val result = omed.lang.eval.ExpressionEvaluator.evaluate(variables, config, expression,contextProvider.getContext.timeZone,entityDataProvider,model)
    result
  }

  def compileSQL(expression: String, config: Configuration,targetClass:String,additionalFilters :Expression = null,context:Map[String,Value]):Value={
    val tmp  = new SQLExpr(null,null,Seq(SQLJoin(SQLAlias("__"+targetClass,model(targetClass)),SQLJoinType.Init,null,null)))
    var updatedVars = context++ contextProvider.getContext.getSystemVariables ++ Map("__var1" -> tmp)
    updatedVars =  convertGuidsToObject(updatedVars)
    val compiled = compile(expression, config, updatedVars)
    val updateCompiled = if(additionalFilters!=null) LogicalExpression(LogicalOperationType.And,Seq(compiled,additionalFilters)) else compiled
    val result =evaluate(updateCompiled, config, updatedVars)
    omed.lang.eval.ExpressionEvaluator.selectForSQLExpr(result)
  }
  def evaluate(expression: String, config: Configuration =  currentConfig, variables: Map[String, Value]): Value = {
    if(isServerExecution(expression)) return serverExecution(expression,config,variables)
     val convertedVariables = convertGuidsToObject(variables)
    val compiled = DBProfiler.profile("compile",execStatProvider,true) { compile(expression, config, convertedVariables)  }
    val result = DBProfiler.profile("evaluate in Expression",execStatProvider,true) { evaluate(compiled, config, convertedVariables) }
    result
  }
  def isValidGUID(uuid:String ):Boolean = {
    if( uuid == null)  false
    try {
      val fromStringUUID = UUID.fromString(uuid)
      val toStringUUID = fromStringUUID.toString()
      toStringUUID.equals(uuid.toLowerCase())
    } catch {
      case _ => false
    }
  }
  def convertGuidsToObject( variables: Map[String, Value])={
     if (variables.isEmpty)  variables
     else  variables.mapValues(_ match {
        case e: EntityInstance => e
        case v: SimpleValue =>
          if ((v.dataType == DataType.String || v.dataType == DataType.Guid) && v.data != null &&  isValidGUID(v.data.toString())) {
            entityFactory.createEntity(v.data.toString)
          }
          else {
            v
          }
        case _ @ e => e
    })
  }
  private def isServerExecution(expression:String):Boolean={
    expression.startsWith("$ARRAY")
  }
  private def serverExecution(expression:String,config: Configuration, variables: Map[String, Value]):Value={
    val separators = Array('(',')',',')
    try{
      val param = expression.split(separators)
      val funcname = param(0).replace(" ","")

      val objectExpr = param(1)
      val arrayExpr = param(2)
      val propertyExpr = param(3)
      val obj = evaluate(objectExpr,config,variables)
      val arrayName =  evaluate(arrayExpr,config,variables).toString
      val propertyName =   evaluate(propertyExpr,config,variables).toString
      val array = dataReader.getCollectionByArrayName(arrayName,obj.getId)
      funcname match  {
        case "$ARRAYMIN" => findMinObjectFromArray(array,propertyName)
        case "$ARRAYMAX" => findMaxObjectFromArray(array,propertyName)
        case "$ARRAYFIND" => findObjectWithValueFromArray(array,propertyName, evaluate(param(4),config,variables))
        case _ => null
      }
    }
    catch {
      case e:Exception => throw new RuntimeException("Ошибка при обработке выражения "+ expression + e.toString)
    }
  }
  private def findObjectWithValueFromArray(array:DataTable, propertyName:String, value :Value):Value={
    if(array.data.length==0) return null
    val columnIndex = array.columns.indexOf(propertyName)
    if(columnIndex == -1) throw new RuntimeException("В выборке данных не найдена колонка "+ propertyName)
    val arrayValues = array.data.map(f => f(columnIndex))
    val notNullvalue = arrayValues.find(p => p!=null)
    if(notNullvalue.isEmpty) return null
    val index = arrayValues.indexWhere(p=> p==value.asInstanceOf[SimpleValue].data)
    if(index == -1) return null
    val mapData = (array.columns zip  array.data(index)).toMap
    entityFactory.createEntityWithData(mapData)
   // new EntityInstance( metaClassProvider.getClassMetadata(mapData("_ClassID").asInstanceOf[String]),mapData)
  }
  private def findMinObjectFromArray (array:DataTable, propertyName:String) :Value={
    if(array.data.length==0) return null
    val columnIndex = array.columns.indexOf(propertyName)
    if(columnIndex == -1) throw new RuntimeException("В выборке данных не найдена колонка "+ propertyName)
    val arrayValues = array.data.map(f => f(columnIndex))
    val notNullvalue = arrayValues.find(p => p!=null)
    if(notNullvalue.isEmpty) return null
    val min = array.data.map(f => f(columnIndex).asInstanceOf[AnyRef]    ).min(new Ordering[Any] {def compare(a:Any,b:Any) :Int={
      if(a==null) return 1
      if(b==null) return -1
      a match {
        case t:Timestamp  => t compareTo(b.asInstanceOf[Timestamp])
        case s:String => s compare b.asInstanceOf[String]
        case i:Int => i compare b.asInstanceOf
        case i:java.math.BigDecimal => i   compareTo(b.asInstanceOf[java.math.BigDecimal])
        case i:Float =>  i   compare a.asInstanceOf[Float]
      }
    }})
    val mapData = (array.columns zip  array.data(arrayValues.indexOf(min))).toMap
    entityFactory.createEntityWithData(mapData)
   //new EntityInstance( metaClassProvider.getClassMetadata(mapData("_ClassID").asInstanceOf[String]),mapData)
  }
  private def findMaxObjectFromArray (array:DataTable, propertyName:String) :Value={
    if(array.data.length==0) return null
    val columnIndex = array.columns.indexOf(propertyName)
    if(columnIndex == -1) throw new RuntimeException("В выборке данных не найдена колонка "+ propertyName)
    val arrayValues = array.data.map(f => f(columnIndex))
    val notNullvalue = arrayValues.find(p => p!=null)
    if(notNullvalue.isEmpty) return null

    val max =  array.data.map(f => f(columnIndex).asInstanceOf[AnyRef]    ).max(new Ordering[Any] {def compare(a:Any,b:Any) :Int={
      if(a==null) return -1
      if(b==null) return 1
      a match {
        case t:Timestamp  => t compareTo(b.asInstanceOf[Timestamp])
        case s:String => s compare b.asInstanceOf[String]
        case i:Int => i compare b.asInstanceOf
        case i:java.math.BigDecimal => i   compareTo(b.asInstanceOf[java.math.BigDecimal])
        case i:Float =>  i   compare a.asInstanceOf[Float]

      }
    }})
    val mapData = (array.columns zip  array.data(arrayValues.indexOf(max))).toMap
    entityFactory.createEntityWithData(mapData)
   // new EntityInstance( metaClassProvider.getClassMetadata(mapData("_ClassID").asInstanceOf[String]),mapData)
  }

//  class DbQueryExecutor(val model: MetaModel) extends QueryExecutor {
//    def getEntityInstance(entityCode: String, entityId: String) = {
//      new EntityInstance(model(entityCode),
//        dataReader.getObjectData(entityCode, entityId))
//    }
//
//  }
}