package omed.bf.handlers

import java.util.logging.Logger

import com.google.inject.Inject

import omed.bf._
import omed.db.{DB, ConnectionProvider, DataAccessSupport}
import omed.system.{GuiceFactory, ContextProvider}
import omed.model._
import omed.bf.tasks.{ExecJs, ExecStoredProc}
import omed.data.{EntityFactory, DataReaderService, DataTable}
import omed.data._
import omed.errors.MetaModelError
import org.mozilla.javascript._

import omed.model.services.ExpressionEvaluator
import scala.xml.XML

import omed.model.ReferenceField
import omed.bf.BusinessFunctionStepLog
import omed.model.BackReferenceField
import omed.model.DataField
import java.util.UUID
import java.text.SimpleDateFormat
import java.math
import omed.lang.eval.DBUtils

/**
 * Реализация шага процесса выполнения скрипта.
 */
class ExecJsHandler extends ProcessStepHandler
with DataAccessSupport {
  /**
   * Логгер.
   */
  val logger = Logger.getLogger(classOf[ExecJsHandler].getName())

  /**
   * Фабрика подключений к источнику данных.
   */
  @Inject
  var connectionProvider: ConnectionProvider = null
  /**
   * Менеджер контекстов, через который можно получить
   * текущий контекст.
   */
  @Inject
  var contextProvider: ContextProvider = null

  /**
   * Провайдер мета-модели
   */
  @Inject
  var metaClassProvider: MetaClassProvider = null


  @Inject
  var dataReader: DataReaderService = null

  @Inject
  var expressionEvaluator: ExpressionEvaluator = null

  @Inject
  var businessFunctionLogger: BusinessFunctionLogger = null

  @Inject
  var entityFactory: EntityFactory = null

  @Inject
  var businessFunctionExecutor: BusinessFunctionExecutor = null
  @Inject
  var functionInfoProvider: FunctionInfoProvider = null
  @Inject
  var processStateProvider: ProcessStateProvider = null
  @Inject
  var dataWriter: DataWriterService = null
  @Inject
  var model: MetaModel = null

  @Inject
  var businessFunctionThreadPool:BusinessFunctionThreadPool = null
  override val name = "_Meta_BFSjs"

  var processInstanceId: String = null
  /**
   * Проверка возможности выполнения данного шага.
   */
  //  def canHandle(step: ProcessTask) = {
  //    step.stepType == name
  //  }

  /**
   * Обработать шаг.
   *
   * @param task Описание шага процесса
   * @param context Текущий контекст
   * @return Контекст процесса
   */
  def handle(task: ProcessTask, context: Map[String, Value], processId: String): Map[String, Value] = {
    processInstanceId = processId
    val targetTask = task.asInstanceOf[ExecJs]
    val updatedContext = expressionEvaluator.convertGuidsToObject(context)
    val jsExecutor = new JsExecutor(targetTask.script, targetTask.outParams,updatedContext)
    businessFunctionLogger.addLogStep(processId, new BusinessFunctionStepLog("Шаг Вызова JavaScript", updatedContext, Map("script" -> SimpleValue(targetTask.script))))
    val result = try {
      jsExecutor.exec()
    } finally {
      jsExecutor.close()
    }

    result
  }

  class JsExecutor(script: String, outParams: Map[String, String], context: Map[String, Value]) {
    val ctx: Context = {
      val cx = ContextFactory.getGlobal.enterContext()
      cx.getWrapFactory().setJavaPrimitiveWrap(false)
      // all code will be interpreted to avoid PermGen penalty
      cx.setOptimizationLevel(-1)
      cx.setLanguageVersion(Context.VERSION_1_7)
      val tmp = cx.getClassShutterSetter

      try {
        cx.setClassShutter(
          new ClassShutter() {
            def visibleToScripts(fullClassName: String): Boolean = {

              val javaLangPrefix = "java.lang."
              val javaLangForbiddenNames = Set(
                "Class", "ClassLoader", "ClassLoaderHelper", "Compiler",
                "Package", "Process", "ProcessBuilder", "Runtime",
                "System", "Terminator", "Thread",
                "ThreadDeath", "ThreadGroup", "ThreadLocal")
              val permittedJavaLang = fullClassName.startsWith(javaLangPrefix) &&
                !javaLangForbiddenNames.contains(fullClassName.substring(javaLangPrefix.length))

              val javaMathPrefix = "java.math."
              val permittedJavaMath = fullClassName.startsWith(javaMathPrefix)

              val javaTextPrefix = "java.text."
              val permittedJavaText = fullClassName.startsWith(javaTextPrefix)

              val javaUtilPrefix = "java.util."
              val javaUtilForbiddenNames = Set(
                "concurrent.", "jar.", "logging.", "prefs.", "regex.", "spi.", "zip.")
              val permittedJavaUtil = fullClassName.startsWith(javaUtilPrefix) &&
                !javaUtilForbiddenNames.contains(fullClassName.substring(javaUtilPrefix.length))

              val javaBusinessPrevix = "omed.bf."
              val permittedBusinessClasses = fullClassName.startsWith(javaBusinessPrevix)
              permittedJavaLang || permittedJavaMath || permittedJavaText || permittedJavaUtil || permittedBusinessClasses

            }
          })
      }
      catch {
        case e: SecurityException => null
        case e@_ => throw e
      }
      cx
    }

    val scope: Scriptable = ctx.initStandardObjects()

    def close() {
      Context.exit()
    }

    def exec() = {
      buildJsContext(scope, context)
      val jsResult = execScript(ctx, scope, script, outParams)
      val result = jsResult.mapValues(jsValToJava)
      //convertJsValuesToSimpleValues(jsResult)
      result
    }

    trait JsPropertyDelegate {
      def get(so: ScriptableObject): AnyRef

      def set(so: ScriptableObject, obj: AnyRef): Unit
    }

    class JsLazyReference(val classCode: String, val id: String) extends JsPropertyDelegate {
      override def get(so: ScriptableObject): AnyRef = {
        // create complex object based on entity instance
        val instance = try {
          val metaClass = metaClassProvider.getClassByRecord(id)
          loadEntityInstance(metaClass, id)
        }
        catch {
          case _ => null
        }
        if (instance != null) buildComplexJsObject(instance) else null
      }

      def set(so: ScriptableObject, obj: AnyRef) {
        // do nothing
      }
    }

    class JsLazyArray(val classCode: String, val refField: String, val refValue: String) extends JsPropertyDelegate {
      override def get(so: ScriptableObject): AnyRef = {
        // create list of complex objects based on entity instance
        businessFunctionLogger.addLogStep(processInstanceId, new BusinessFunctionStepLog("Получение данных javaScript collection before", params = Map("classCode" -> SimpleValue(classCode), "refField" -> SimpleValue(refField), "refValue" -> SimpleValue(refValue))))
        //  val metaClass = metaClassProvider.getClassByCode(classCode)
        val records = dataReader.getCollectionData(classCode, refField, refValue)

        // construct entities from raw data records
        val entities = for {
          record <- records.data
          dataMap = (records.columns zip record).toMap
        } yield entityFactory.createEntityWithData(dataMap)

        createJSArray(entities)
      }

      def set(so: ScriptableObject, obj: AnyRef) {
        // do nothing
      }
    }

    def buildJsContext(scope: Scriptable, context: Map[String, Value]): Unit = {
      val properties = context.mapValues(_ match {
        case sv: SimpleValue => buildSimpleJsObject(sv.dataType, sv.data)
        case e: EntityInstance => buildComplexJsObject(e)
      })

      for ((varName, varValue) <- properties) {
        val propertyName = "$" + varName.replaceAll("\\@", "")
        val propertyValue = varValue.asInstanceOf[AnyRef]
        scope.put(propertyName, scope, propertyValue)
      }

      scope.put("$System", scope, this)
    }


    def buildSimpleJsObject(dataType: DataType.Value, data: Any): AnyRef = {
      import omed.model.DataType._
      val dateFormat = new SimpleDateFormat("MM/dd/yyyy") // new SimpleDateFormat("yyyy.mm.dd hh:mm:ss") //
      val dateTimeFormat = new SimpleDateFormat("MM dd,yyyy hh:mm:ss")
      val converted = if (data != null) dataType match {
        case String => data
        case Char => data.toString
        case Int => if (data.isInstanceOf[Number]) data.asInstanceOf[Number] else data.asInstanceOf[String].toInt.asInstanceOf[Number]
        case Boolean => data match {
          case x: String => x.toUpperCase == "Y"
          case x: Boolean => x
          case _ => false
        }
        case Date => {
          data.asInstanceOf[java.util.Date].getTime
          //  dateFormat.format(data.asInstanceOf[java.util.Date])
          // new java.util.Date( data.asInstanceOf[java.util.Date].getTime)

        }
        case DateTime | DateTime2 => {
          data.asInstanceOf[java.util.Date].getTime
          // (new java.util.Date( data.asInstanceOf[java.util.Date].getTime)).toString
          // data.asInstanceOf[java.util.Date].toString
          //  dateTimeFormat.format(data.asInstanceOf[java.util.Date])
          //new java.util.Date( data.asInstanceOf[java.util.Date].getTime)
        }
        case Guid => data.toString
        case Decimal => data.asInstanceOf[java.math.BigDecimal]
        case Float => data.asInstanceOf[Number]
        case Binary => data.asInstanceOf[Array[Byte]]
        case Null => null
        //case Entity =>
      } else null

      converted.asInstanceOf[AnyRef]
    }

    val JsPropertyAttributes = ScriptableObject.READONLY

    def buildComplexJsObject(e: EntityInstance): AnyRef = {
      // явное получение данных объекта если он еще не был загружен
      e.forceLoad
      val metaClass = e.obj
      // define js object and it properties
      val jsObject = new NativeObject()
      for (f <- metaClass.fields) f match {
        case df: DataField => {
          // directly accesible field value
          val dfValue = buildSimpleJsObject(df.dataType, e.data(df.code))
          jsObject.defineProperty(df.code, dfValue, JsPropertyAttributes)
        }
        case rf: ReferenceField => {
          // lazy field that creates referred object on-demand
          val classCode = rf.refObjectCode
          val objectId = Option(e.data(rf.code)).map(_.toString).orNull
          if (objectId != null) {
            val jsDelegate = new JsLazyReference(classCode, objectId)
            val getter = jsDelegate.getClass.getMethod("get", classOf[ScriptableObject])
            val setter = jsDelegate.getClass.getMethod("set", classOf[ScriptableObject], classOf[Object])
            jsObject.defineProperty(rf.code, jsDelegate, getter, setter, JsPropertyAttributes)
          } else {
            // in case of null-reference pass null directly
            jsObject.defineProperty(rf.code, null.asInstanceOf[AnyRef], JsPropertyAttributes)
          }
        }
        case bf: BackReferenceField => {
          // skip back-reference fields as they are deprecated
        }
      }

      // define js arrays for every computed back reference field
      for (f <- metaClass.backReferenceFields) {
        val jsDelegate = new JsLazyArray(f.refClassCode, f.refFieldCode, e.getId)
        val getter = jsDelegate.getClass.getMethod("get", classOf[ScriptableObject])
        val setter = jsDelegate.getClass.getMethod("set", classOf[ScriptableObject], classOf[Object])
        jsObject.defineProperty(f.arrayName, jsDelegate, getter, setter, JsPropertyAttributes)
      }
      jsObject
    }

    def loadEntityInstance(metaClass: MetaObject, id: String): EntityInstance = {
      entityFactory.createEntityWithCode(id, metaClass.code)
    }

    def createJSArray(entities:Seq[EntityInstance])={
      // contruct js objects from entities
      val jsObjects = entities.map(buildComplexJsObject)
      val jsArray = new NativeArray(jsObjects.toArray)
      ScriptRuntime.setBuiltinProtoAndParent(jsArray, scope, TopLevel.Builtins.Array)
      businessFunctionLogger.addLogStep(processInstanceId, new BusinessFunctionStepLog("Получение данных javaScript collection", params = Map("count" -> SimpleValue(entities.length))))
      jsArray
    }
    /**
     * Выполнить скрипт.
     *
     * @param script Имя хранимой процедуры
     * @param params Параметры выполнения скрипта
     * @return Набор объектов из контекста после выполнения скрипта
     */
    def execScript(context: Context,
                   scope: Scriptable,
                   script: String,
                   outParams: Map[String, String]): Map[String, AnyRef] = {
      context.evaluateString(scope, script, "CustomScript", 1, null)

      val resultVariables = for {
        (scriptName, contextName) <- outParams
        resultValue = scope.get(scriptName, scope)
      } yield contextName -> resultValue

      resultVariables
    }
   // Здесь описаны методы которые могут быть вызваны из   JS

    /**
     * добавление записи в лог БФ
     * @param value
     */
    def Log(value: Any){
        businessFunctionLogger.addLogStep(businessFunctionThreadPool.rootProcessId, new BusinessFunctionStepLog("Лог JavaScript", Map("message" ->  SimpleValue(value))))
    }

    /**
     * получение данных по идентификатору грида
     * @param windowGridId
     * @return
     */
    def WindowGridData(windowGridId: String): Any ={
      val records = dataReader.getGridData(windowGridId, null, null, null, null, null, null, null, null)
      val entities = for {
        record <- records.data
        dataMap = (records.columns zip record).toMap
      } yield entityFactory.createEntityWithData(dataMap) //new EntityInstance(metaClassProvider.getClassMetadata(dataMap("_ClassID").asInstanceOf[String]), dataMap)
      createJSArray(entities)
    }

    /**
     * получение данных по коду класса
     * @param classCode
     * @return
     */
    def ClassData(classCode: String): Any = {
      ClassData(classCode,null)
    }

    /**
     *   получение данных по коду класса с фильтрацией
     * @param classCode
     * @param filter
     * @return
     */
    def ClassData(classCode: String,filter:String): Any = {
      val array:EntityArray = expressionEvaluator.evaluate("$"+classCode+ (if(filter!=null)".filter("+ filter+")" else ""),variables = context).asInstanceOf[EntityArray]
      createJSArray(array.data.asInstanceOf[Seq[EntityInstance]])
    }

    /**
     * получение атрибута из xml
     * @param xmlStr
     * @param attribute
     * @return
     */
    def XMLGetAttr(xmlStr: String, attribute: String): String = {
      try {
        val xml = XML.loadString(xmlStr)
        xml.attribute(attribute).map(f => f.last.text).getOrElse(null)
      }
      catch {
        case e: Exception => throw new RuntimeException("ошибка при парсинге xml " + xmlStr + " attribute " + attribute + e.getMessage)
      }
    }

    /**
     * удаление объекта
     * @param obj
     */
    def Delete(obj: Any) {
      if (obj == null) return
      val javaObject = SimpleValue.apply(obj).getId
      dataWriter.deleteRecord(javaObject)
    }

    /**
     * редактирование объекта
     * @param obj
     * @param code
     * @param value
     * @return
     */
    def Edit(obj: Any, code: String, value: Any): Boolean = {
      val instance = entityFactory.createEntity(if (obj != null) SimpleValue.apply(obj).data.toString() else null)
      val updValue = value match {
        case d: java.lang.Double => {
          if (instance != null) {
            val field = instance.obj(code)
            field match {
              case e: DataField => e.dataType match {
                case DataType.Int => new SimpleValue(d.toInt, DataType.Int)
                case DataType.Date => new SimpleValue(new java.util.Date(d.toLong), DataType.Date)
                case _ => new math.BigDecimal(d)
              }
              case _ => new math.BigDecimal(d)
            }
          } else null
          //"%.4f".format(d).replace(",",".")
        }
        case null => null
        case e => SimpleValue.apply(e)
      }
      val javaValue = if (updValue != null) DBUtils.platformValueToDb(updValue).toString else null
      //SimpleValue.apply(convertValue).data.toString() else null

      val (isValid, validationResults) = dataWriter.editRecord(instance, Map(code -> javaValue))
      isValid
    }

    /**
     * вызов БФ
     * @param bfCode
     * @param vars
     * @return
     */
    def CallBF(bfCode: String, vars: Array[Any]): Any = {

      def isValidGUID(uuid: String): Boolean = {
        if (uuid == null) false
        try {
          // we have to convert to object and back to string because the built in fromString does not have
          // good validation logic.
          val fromStringUUID = UUID.fromString(uuid)
          val toStringUUID = fromStringUUID.toString()
          toStringUUID.equals(uuid.toLowerCase())
        } catch {
          case _ => false
        }
      }

      val bfId = if (isValidGUID(bfCode)) bfCode
      else dataReader.getBFID(bfCode)
      val functionInfo = functionInfoProvider.getFunctionInfo(bfId).get
      val vars2 = vars.map(jsValToJava)
      val updatedValues =  vars.map(jsValToJava)//vars2.map(v => SimpleValue.apply(v))
      val sortedParameters = functionInfo.params.sortBy(p => p.order)
      val params = Map(sortedParameters.map(p => p.varName).zip(updatedValues).toArray: _*)
      val process = businessFunctionExecutor.initFunctionInstance(bfId, params)
      val (nextStep, result) = businessFunctionExecutor.getNextClientStep(process)
      processStateProvider.dropProcess(process)
      val jsResult = if (result.isEmpty) null
      else
        result.get match {
          case sv: SimpleValue => sv.data.toString //buildSimpleJsObject(sv.dataType, sv.data)
          case e: EntityInstance => e.getId
        }
      if (nextStep.isEmpty) jsResult.asInstanceOf[AnyRef] //результат выполнения БФ
      else throw new MetaModelError("В шагах вложенной БФ недопустимы клиентские шаги")
    }

    def jsValToJava(jsVal: Any) = {
      SimpleValue.apply(
        jsVal match {
          case s: String => if (s == "undefined") null else s
          case d: java.lang.Double => new math.BigDecimal(d)
          case d: java.lang.Long => new java.util.Date(d)// "%.4f".format(d).replace(",",".")
          case v@_ => v
        }
      )
    }
  }

}

