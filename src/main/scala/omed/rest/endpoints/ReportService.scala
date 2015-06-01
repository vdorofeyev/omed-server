package omed.rest.endpoints

import javax.ws.rs.{GET, Produces, Path, PathParam}
import javax.ws.rs.core.{Context, MediaType, Response}
import javax.servlet.http.HttpServletRequest
import java.io.StringWriter

import com.google.inject.Inject
import net.iharder.Base64
import xml.{Utility, XML}

import omed.reports.TemplateService
import omed.errors.NotFoundError


/**
 * Сервис получения шаблонов отчетных форм.
 */
@Path("/report")
@Produces(Array(MediaType.APPLICATION_XML))
class ReportService {

  /**
   * Объектное представление HTTP-запроса, предоставляемое Java Servlet API.
   */
  @Context
  var httpRequest: HttpServletRequest = null

  /**
   * Внутренний сервис получения отчетных форм, обеспечивающий взаимодействие
   * с хранилищем метаданных.
   */
  @Inject
  var templateService: TemplateService = null

  /**
   * Получает описание шаблона отчетной формы
   * @param templateId Идентификатор шаблона
   * @return Ответ сервиса с описанием отчетной формы в формате FastReport
   */
  @GET
  @Path("/template/{templateId}")
  @Produces(Array(MediaType.APPLICATION_XML))
  def getTemplateData(@PathParam("templateId") templateId: String): Response = {
    var template = templateService.getTemplateDescription(templateId)

    if (template == null)
      throw new NotFoundError("Требуемый шаблон не найден")

    val xml =
      <template>
        <id>{Option(template.id).getOrElse("")}</id>
        <name>{Option(template.name).getOrElse("")}</name>
        <content>{Option(template.content).map(Base64.encodeBytes).getOrElse("")}</content>
      </template>

    val sw = new StringWriter()
    XML.write(sw, Utility.trim(xml), "UTF-8", true, null)
    val content = sw.toString

    Response.ok().entity(content).build()
  }

}
