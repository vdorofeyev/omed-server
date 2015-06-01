package omed.bf

import scala.xml._
import com.google.inject.Inject
import omed.db.{DB, ConnectionProvider}
import omed.system.ContextProvider
import omed.cache.{ExecStatProvider, DomainCacheService}
import omed.errors.MetaModelError

class FunctionInfoProviderImpl extends FunctionInfoProvider {

  @Inject
  var connectionProvider: ConnectionProvider = null
  @Inject
  var contextProvider: ContextProvider = null
  @Inject
  var domainCacheService: DomainCacheService = null

  @Inject
  var execStatProvider : ExecStatProvider = null
  def getFunctionInfo(functionId: String): Option[BusinessFunction] = {
    val cached = domainCacheService.get(classOf[BusinessFunction], functionId)
    if (cached != null) return Option(cached)

    val description = loadFunctionInfoFromStorage(functionId)
    val result = Option(description).map(readFunctionDescription)

    if (result.isDefined)
      domainCacheService.put(classOf[BusinessFunction], functionId, result.get)
    else throw new MetaModelError("Бизнес функция с идентификатором = " + functionId + " не найдена")
    result
  }

  /**
   * Получить описание бизнес-функции из хранилища
   */
  private def loadFunctionInfoFromStorage(functionId: String): String = {
    connectionProvider.withConnection {
      connection =>
        val dbResult = DB.dbExec(connection, "[_Meta].[GetBFDescr]",
          contextProvider.getContext.sessionId,
          List(("BusinessFunctionID", functionId)),execStatProvider)

        if (dbResult.next())
          dbResult.getString("Descr")
        else
          null
    }
  }

  /**
   * Прочитать описание бизнес-функции из XML
   * replacement for new Model2Xml().parseBf(description)
   */
  private def readFunctionDescription(description: String): BusinessFunction = {
    val xml = XML.loadString(description)
    val functionId = (xml \\ "_Meta_BusinessFunction" \ "@ID")
      .headOption.map(_.text).orNull
    val functionName = (xml \\ "_Meta_BusinessFunction" \ "@Name")
      .headOption.map(_.text).orNull
    val varResult = (xml \\ "_Meta_BusinessFunction" \ "@ResultVariable")
      .headOption.map(_.text).orNull
    val steps = (xml \\ "Steps").headOption
      .map(_.child
      .map(node =>
      node match {
        case e: Elem =>
          val id = e.attribute("ID").map(_.head.text).orNull
          val stepType = e.label
          val name = e.attribute("Name").map(_.head.text).orNull
          val description = e
          new FunctionStep(id, stepType, name, description)
        case _ =>
          null
      })
      .filter(_ != null)).getOrElse(Seq())
    val parameters = (xml \ "Params").headOption
      .map(_.child
      .map(node =>
      node match {
        case e: Elem =>
          val name = e.attribute("Name").map(_.head.text).orNull
          val order =  e.attribute("SortOrder").map(_.head.text).orNull
          if( name==null || order ==null)  throw new MetaModelError("В параметрах вызова БФ не указано имя переменной или порядок")
            new FunctionParameter(name.replaceFirst("\\@", ""),order.toInt)
        case _ =>
          null
      })
      .filter(_ != null)).getOrElse(Seq())
    new BusinessFunction(functionId, functionName, steps,varResult,parameters)
  }

}
