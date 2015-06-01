package omed.rest.providers

import javax.ws.rs.ext.{Provider, ExceptionMapper}
import javax.ws.rs.core.{MediaType, HttpHeaders, Response}

import omed.errors.{ValidationException, ObjectLockedException}
import java.io.StringWriter
import xml.XML
import ru.atmed.omed.beans.model.meta.{ValidationResultType, ClassValidationRule}

/**
 * Обработчик исключений возникающих из-за рассогласований в метамодели.
 */
@Provider
class ValidationExceptionMapper extends ExceptionMapper[ValidationException] {
  /**
   *
   * @param e Перехваченное исключение
   * @return Ответ сервиса с сообщением об ошибке
   */
  def toResponse(e: ValidationException): Response = {
    val message =
      <result>
        <returnCode>-5</returnCode>
        <message>Ошибка валидации.</message>
        <validation>
          <record>{e.results.map(validation => {
            <result>
              <code>{
                val rule = validation.validationRule.asInstanceOf[ClassValidationRule]
                rule.validationResultType match {
                  case ValidationResultType.Error => "error"
                  case ValidationResultType.Warning => "warning"
                }
                }</code>
              <message>{ validation.validationRule.falseMessage }</message>
              { if (!validation.compiled.condition.getUsedVariableFields("this").isEmpty)
              <fields>
                { validation.compiled.condition.getUsedVariableFields("this").map(
                fieldName => <field>{ fieldName }</field> )}
              </fields>
              }
            </result>
        })}</record>
        </validation>
      </result>

    val sw = new StringWriter()
    XML.write(sw, message, "UTF-8", true, null)
    val xmlMessage = sw.toString

    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
      .entity(xmlMessage)
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
      .build()
  }
}
