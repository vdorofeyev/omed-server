/**
 *
 */
package omed.rest.endpoints

import javax.ws.rs._
import javax.ws.rs.core.{  MediaType, Response }
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.StreamingOutput
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Writer

import com.google.inject.Inject

import ru.atmed.omed.beans.model.meta._
import omed.auth._
import omed.forms.MetaFormProvider
import omed.data._
import omed.system.{Context, ContextProvider}
import omed.model._

import omed.lang.eval.{ExpressionEvaluator, Configuration}
import omed.lang.struct.{VariableField, SimpleVariable, Constant, CalcFunction}

import omed.system.{Context, ContextProvider}

import ru.atmed.omed.beans.model.meta.MetaFormGrid
import ru.atmed.omed.beans.model.meta.MetaGrid
import omed.bf.tasks.GetClientValue

/**
 * Служебный сервис, предоставляющий методы для тестирования взаимодействия
 * с хранилищем данных и проверки логической целостности и непротиворечивости метаданных
 */
@Path("/test")
@Produces(Array(MediaType.APPLICATION_XML))
class TestService {

  /**
   * Объектное представление HTTP запроса, формируемое на уровне Servlet API.
   */
  //@Context
  @Inject
  var httpRequest: HttpServletRequest = null

  /**
   * Провайдер контекста, предоставляющий доступ к текущему контексту, объекту класса [[omed.system.Context]]
   */
  @Inject
  var contextProvider: ContextProvider = null

  @Inject
  private var authBean: Auth = null
  @Inject
  private var metaFormProvider: MetaFormProvider = null
  @Inject
  private var dataWriter: DataWriterService = null
  @Inject
  private var dataReader: DataReaderService = null
  @Inject
  private var metaClassProvider:MetaClassProvider = null

  private var domain: Int = 0
  private val username = "admin"
  private val authorname = null
  private val password = "123"
  private val userAgent = "service test system"
  private val userIp = ""

  def login() {
    val (user, author) = authBean.authenticateByPassword(domain, username, authorname, password)
    val sessionAuthor = if (author != null) author.id else user.id
    val sessionId = authBean.login(
      null, domain,
      user.id, sessionAuthor,
      userAgent, userIp)

    val session = httpRequest.getSession()

    session.setAttribute("domainId", domain)
    session.setAttribute("userId", user.id)
    session.setAttribute("authorId", author.id)
    session.setAttribute("isSuperUser", author.isSuperUser)
    session.setAttribute("sessionId", sessionId)
  }

  def sessionId() = {
    contextProvider.getContext.sessionId
  }

  /**
   * Формирует простое сообщение в ответ на запрос пользователя
   * @return Ответ сервиса на запрос пользователя, всегда содержит
   *         строку pong в форме простого XML-документа
   */
  @GET
  @Path("/ping")
  @Produces(Array(MediaType.APPLICATION_XML))
  def ping() = {
    "<?xml version='1.0' encoding='utf-8'?>\n<test>pong</test>"
  }

  @GET
  @Path("/customTest")
  @Produces(Array(MediaType.APPLICATION_XML))
  def customTest() = {

    "<?xml version='1.0' encoding='utf-8'?>\n<test>"+"</test>"
  }


  /**
   * Проверяет загрузку справочников для всех полей всех форм, перечисленных в главном меню
   * @param domain Идентификатор домена, метаданные которого требуют проверки
   * @return Ответ сервиса с описанием процесса проверки и выявленных проблемах в виде HTML-документа
   */


  @GET
  @Path("/references")
  @Produces(Array("text/html; charset=UTF-8"))
  def referencesTest(@QueryParam("domain") domain: Int = 0): Response = {

    this.domain = domain
    login()

    val outputStream = new StreamingOutput() {
      def write(output: OutputStream) {
        val writer = new OutputStreamWriter(output, "utf-8")
        writer.write("<html><h1>Тест загрузки справочников</h1>\n"
          + "Результаты для домена № " + domain + "<br/>\n"
          + "Загружаются справочники для всех полей всех форм, перечисленных в главном меню.<br/>\n")
        writer.write("<table>")
        writer.write("<tr style='font-weight: bold;'>"
          + "<td>Form</td><td>Field</td><td>Type</td><td>Result</td></tr>")

        def fieldsWithGrids = getGrids flatMap (g => g.mainGrid.fields map (f => (f, g)))
        fieldsWithGrids.foreach(t => {
          testRef(t._1, t._2, writer)
          writer.flush()
        })

        writer.write("</table>")

        writer.write("<h1>Готово</h1>")
        writer.write("</html>")
        writer.close()
      }
    }

    Response.ok(outputStream).build()
  }

  /*
   * Return iterator for all grid forms, retrieved from main menu
   */
  def getGrids: Iterator[MetaFormGrid] = {

    val menu = metaFormProvider.getMainMenu()
    val gridIds = menu.filter(m => m.openViewId != null).map(_.openViewId).toSet

    val allGridIds = scala.collection.mutable.Set[String]()

    def getGridsTree(id: String): Iterator[MetaFormGrid] = {
      if (allGridIds.contains(id))
        Iterator()
      else {
        allGridIds += id
        try {
          val grid = metaFormProvider.getMetaFormGrid(id)

          def lookupsIterator(g: MetaGrid) =
            g.fields.filter(_.getTypeCode.toLowerCase == "ref").iterator.map(f =>
              getGridsTree(f.defaultFormGridId)).flatten

          val data = dataReader.getGridData(grid.mainGrid.windowGridId, null, null, null, null, null, null, null, null)
          val idIndex = data.columns.indexOf("ID")

          val refGridIterator = if (data.data.length > 0 && idIndex >= 0) {
            val card = metaFormProvider.getMetaCard(data.data(0)(idIndex).asInstanceOf[String])
            card.refGrids.iterator.map(refGrid => lookupsIterator(refGrid)).flatten
          } else Iterator()

          Seq(grid).iterator ++ lookupsIterator(grid.mainGrid) ++ refGridIterator
        } catch {
          case e: Exception =>
            Seq(MetaFormGrid(caption = id + ": " + e.getMessage + e.getStackTraceString)).iterator
        }
      }
    }

     gridIds.iterator map (id => getGridsTree(id)) flatten
  }

  def testRef(field: Metafield, form: Metaform, writer: Writer) {
    if (field.getTypeCode.toLowerCase == "ref") {
      if (Option(field.getTypeExtInfo).getOrElse("") == "") {
        writer.write("<tr><td>" + form.getCaption + "</td><td>" + field.getCodeName
          + "</td><td>" + field.getTypeCode
          + "</td><td style='color: red'>empty Type.ExtInfo</td></tr>")
        return
      }

      try {
        val startTime = System.currentTimeMillis()
        dataReader.getDictionaryData(field.getViewFieldId, null)
        val finishTime = System.currentTimeMillis()

        writer.write("<tr><td>" + form.getCaption + "</td><td>" + field.getCodeName
          + "</td><td>" + field.getTypeCode
          + "</td><td style='color: green'>passed in " + (finishTime - startTime) + " ms</td></tr>")
      } catch {
        case _@ e => writer.write("<tr><td>" + form.getCaption + "</td><td>" + field.getCodeName
          + "</td><td>" + field.getTypeCode
          + "</td><td style='color: red'>Error loading dictionary for reference field"
          + "<br/>\n(" + e.getMessage() + ")</td></tr>")
      }
    } else if (field.getEditorType.toLowerCase == "dropdown") {
      //			if (Option(field.getExtInfo).getOrElse("") == "") {
      //				errors ++ ("Reference field " + field.getCodeName
      //					+ " (" + field.getViewFieldId + ") has empty Type.ExtInfo")
      //				return
      //			}

      try {
        val startTime = System.currentTimeMillis()
        dataReader.getDictionaryData(field.getViewFieldId, null)
        val finishTime = System.currentTimeMillis()

        writer.write("<tr><td>" + form.getCaption + "</td><td>" + field.getCodeName
          + "</td><td>" + field.getTypeCode
          + "</td><td style='color: green'>passed in " + (finishTime - startTime) + " ms</td></tr>")
      } catch {
        case _@ e => writer.write("<tr><td>" + form.getCaption + "</td><td>" + field.getCodeName
          + "</td><td>" + field.getTypeCode
          + "</td><td style='color: red'>Error loading dictionary for drop-down field"
          + "<br/>\n(" + e.getMessage() + ")</td></tr>")
      }
    }
  }

  /**
   * Проверяет работу с данными, создает и удаляет записи
   * для всех форм-списков, перечисленных в главном меню.
   * @param domain Идентификатор домена
   * @return Ответ сервиса с описанием процесса и результата проверки
   *         в виде простого HTML-документа
   */
  @GET
  @Path("/insert")
  @Produces(Array("text/html; charset=UTF-8"))
  def insertTest(@QueryParam("domain") domain: Int = 0): Response = {

    this.domain = domain
    login()

    val outputStream = new StreamingOutput() {
      def write(output: OutputStream) {
        val writer = new OutputStreamWriter(output, "utf-8")
        writer.write("<html><h1>Тест создания и удаления записей</h1>\n"
          + "Результаты для домена № " + domain + "<br/>\n"
          + "Создаются и потом удаляются записи для всех форм-списков, перечисленных в главном меню.<br/>\n")
        writer.write("<table>")

        var initTime = System.currentTimeMillis()
        // filter grids which allow insert
        getGrids filter (p => p.mainGrid.isInsertAllowed && metaClassProvider.getClassMetadata(p.getClassId).code.startsWith("_Meta") ) foreach (g => {
          val startTime = System.currentTimeMillis()

          writer.write("<tr>")
          // get Id of inserted record
          val id: String = try {
            insertRecord(g.mainGrid)
          } catch {
            case _@ e => {
              writer.write("<td>" + g.getCaption + "</td><td style='color: red'>INSERT failed; "
                + e.getMessage() + "</td>")
              null
            }
          }

          val insertDoneTime = System.currentTimeMillis()

          // delete if inserted
          val deleted = if (id != null) {
            try {
              deleteRecord(id)
              true
            } catch {
              case _@ e => {
                writer.write("<td>" + g.getCaption + "</td><td style='color: orange'>DELETE failed; "
                  + e.getMessage() + "</td>")
                false
              }
            }
          } else false

          val deleteDoneTime = System.currentTimeMillis()

          if (deleted) writer.write(new StringBuilder().append("<td>")
            .append(g.getCaption)
            .append("</td><td style='color: green'>passed")
            .append(" (grid meta: ").append(startTime - initTime)
            .append(" ms, insert: ").append(insertDoneTime - startTime)
            .append(" ms, delete: ").append(deleteDoneTime - insertDoneTime)
            .append(" ms)</td>").toString())

          writer.write("</tr>\n")
          writer.flush()

          initTime = System.currentTimeMillis()
        })

        writer.write("</table>")

        writer.write("<h1>Готово</h1>")
        writer.write("</html>")
        writer.close()
      }
    }

    Response.ok(outputStream).build()
  }

  def insertRecord(grid: MetaGrid): String = {
    val id = dataWriter.addRecord(grid.classId).getId
    if (id != null)
      id
    else
      throw new Exception("Запись не создана")
  }

  def deleteRecord(id: String) {
    dataWriter.deleteRecord(id)
  }

  /**
   * Для всех форм-списков производит получение всех данных,
   * а также данных первой записи из списка для формы-карточки.
   * @param domain Идентификатор домена
   * @return Ответ сервиса с описанием процесса и результата проверки
   *         в виде простого HTML-документа
   */
  @GET
  @Path("/readCardData")
  @Produces(Array("text/html; charset=UTF-8"))
  def readCardDataTest(@QueryParam("domain") domain: Int = 0): Response = {

    this.domain = domain
    login()

    val outputStream = new StreamingOutput() {
      def write(output: OutputStream) {
        val writer = new OutputStreamWriter(output, "utf-8")
        writer.write("<html><h1>Тест получения данных формы</h1>\n"
          + "Результаты для домена № " + domain + "<br/>\n"
          + "Для всех форм-списков производится получение всех данных, а также данных первой записи из списка для формы-карточки.<br/>\n")
        writer.write("<table>")
        writer.write("<tr><td style='width: 20%'>Название грида</td><td style='width: 50%'>Данные грида</td><td>Данные карточки</td><td>Метаданные карточки</td></tr>")

        var initTime = System.currentTimeMillis()
        // filter grids which allow insert
        getGrids foreach (g => {
          writer.write("<tr>")
          writer.write("<td>" + g.getCaption + "</td>")
          val startTime = System.currentTimeMillis()

          // get grid data
          try {
            val gridData = dataReader.getGridData(g.mainGrid.windowGridId, null, null, null, null, null, null, null, null)
            val gridDataTime = System.currentTimeMillis()
            writer.write("<td>" + (gridDataTime - startTime) + " мс</td>")

            // get card data
            if (gridData.data.length > 0) {
              val recordId = gridData.data(0)(gridData.columns.indexOf("ID")).toString()
              val cardData = dataReader.getCardData(recordId)
              val cardDataTime = System.currentTimeMillis()

              // check for data existence
              if (cardData.data == null || cardData.data.length == 0) {
                writer.write("<td style='color: red; font-weight: bold;'>Пустой ответ</td>")
              } else {
                writer.write("<td>" + (cardDataTime - gridDataTime) + " мс</td>")
              }

              // get card meta
              val cardMeta = metaFormProvider.getMetaCard(recordId)
              val cardMetaTime = System.currentTimeMillis()
              writer.write("<td>" + (cardMetaTime - cardDataTime) + " мс</td>")
            } else {
              writer.write("<td>не вызывалось</td><td>не вызывалось</td>")
            }
          } catch {
            case _@ e => {
              writer.write("<td style='color: red' colspan='3'>" + e.getMessage() + "</td>")
            }
          }

          writer.write("</tr>\n")
          writer.flush()
        })

        writer.write("</table>")

        writer.write("<h1>Готово</h1>")
        writer.write("</html>")
        writer.close()
      }
    }

    Response.ok(outputStream).build()
  }

  /**
   * Получает DisplayString для ссылок
   * @param domain Идентификатор домена, метаданные которого тестируются
   * @return Ответ сервиса с описанием процесса и результата проверки
   *         в виде простого HTML-документа
   */
  @GET
  @Path("/display-string")
  @Produces(Array("text/html; charset=UTF-8"))
  def displayStringTest(@QueryParam("domain") domain: Int = 0): Response = {

    this.domain = domain
    login()

    val outputStream = new StreamingOutput() {
      def write(output: OutputStream) {
        val writer = new OutputStreamWriter(output, "utf-8")
        writer.write("<html><h1>Тест получения DisplayString для ссылок</h1>\n"
          + "Результаты для домена № " + domain + "<br/>\n")
        writer.write("<table>")
        writer.write("<tr style='font-weight: bold;'>"
          + "<td>Form</td><td>Field</td><td>Type</td><td>Result</td></tr>")

        val gridIterator = getGrids
        while (gridIterator.hasNext) {
          try {
            val grid = gridIterator.next()
            if (grid.mainGrid.fields != null) {

              val data = dataReader.getGridData(grid.mainGrid.windowGridId, null, null, null, null, null, null, null, null)

              grid.mainGrid.fields.foreach(field => {
                if (field.getTypeCode.toLowerCase == "ref"
                  || field.getEditorType.toLowerCase == "dropdown") {

                  if (!data.columns.contains(field.getCodeName + "$")) {
                    writer.write("<tr><td>" + grid.caption + "</td><td>" + field.getCodeName
                      + "</td><td>" + field.getTypeCode
                      + "</td><td style='color: red'>no display string</td></tr>")
                  } else {
                    writer.write("<tr><td>" + grid.caption + "</td><td>" + field.getCodeName
                      + "</td><td>" + field.getTypeCode
                      + "</td><td style='color: green'>passed</td></tr>")
                  }
                }
              })
            } else {
              writer.write("<tr><td style='color: red' colspan=\"4\">" + grid.caption + "</td></tr>")
            }
            writer.flush()
          } catch {
            case e: Exception => writer.write(
              "<tr><td style='color: red' colspan=\"4\">" + e.toString + "</td></tr>")
          }
        }
        writer.write("</table>")

        writer.write("<h1>Готово</h1>")
        writer.write("</html>")
        writer.close()
      }
    }

    Response.ok(outputStream).build()
  }
}
