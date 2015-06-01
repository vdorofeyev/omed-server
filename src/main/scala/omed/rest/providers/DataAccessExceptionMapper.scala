package omed.rest.providers
import javax.ws.rs.ext.{Provider, ExceptionMapper}
import omed.errors.DataAccessError
import javax.ws.rs.core.{MediaType, HttpHeaders, Response}
import java.io.{ StringWriter}
import xml.XML
/**
 * Created by andrejnaryskin on 06.03.14.
 */
@Provider
class DataAccessExceptionMapper extends ExceptionMapper[DataAccessError] {
/**
 *
 * @param e Перехваченное исключение
 * @return Ответ сервиса с сообщением об ошибке
 */
def toResponse(e: DataAccessError): Response = {
val message =
<result>
<returnCode>-6</returnCode>
<message>{e.getMessage}</message>
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