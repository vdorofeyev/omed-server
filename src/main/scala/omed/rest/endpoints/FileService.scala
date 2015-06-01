package omed.rest.endpoints

import javax.ws.rs._
import javax.servlet.http.HttpServletRequest
import java.io.InputStream
import javax.ws.rs.core._
import java.io.OutputStream

import com.google.inject.Inject

import omed.forms.MetaFormProvider
import omed.data.DataReaderService
import omed.system.ContextProvider


/**
 * Сервис загрузки и получения файлов
 */
@Path("/streaming")
class FileService {
  @Inject
  var metaFormProvider: MetaFormProvider = null
  @Inject
  var dataReader: DataReaderService = null
  @Context
  var httpRequest: HttpServletRequest = null

  @Inject
  var contextProvider: ContextProvider = null

  /**
   * Загружает получаемый файл и распечатывает его в выходной поток процесса
   * @param stream Входящий поток данных
   * @return Ответ сервера об успешном завершении операции
   */
  @POST
  @Path("/upload")
  @Consumes(Array(MediaType.APPLICATION_OCTET_STREAM))
  def uploadFile(stream: InputStream): Response = {
      var summ = 0
      var r = 0

      //val out = new FileOutputStream(new File("c:\\1.avi"));

      var bytes = new Array[Byte](1024)

      while (r != -1) {
        r = stream.read(bytes)
        if (r != -1) {
          summ += r
          println(summ.toString)
          //      out.write(bytes, 0, r)
        }
      }
      //out.flush();
      //out.close();

      Response.status(Response.Status.OK).entity(summ.toString).build()
  }

  /**
   * Получает данные бинарного объекта
   * @return Ответ сервера с полученными у хранилища данными
   */
  @GET
  @Path("/download")
  @Produces(Array(MediaType.APPLICATION_OCTET_STREAM))
  def downloadFile() = {
      //получить бин
      val metaObjIds = metaFormProvider.getMainMenu().map(x => x.openViewId)

      val outputStream = new StreamingOutput() {
        def write(output: OutputStream) {

          metaObjIds.foreach(id => {

            val gridData = try {
              dataReader.getGridData(
                gridId = id, nodeId = null, refId = null, nodeData = null,
                recordId = null, viewCardId = null, fieldId = null, variablesXml = null,
                treeVariablesXml = null)
            } catch { case _ => null }

            if (gridData != null) {
              gridData.data.foreach(row => {
                val row_str = (row.foldLeft(new StringBuilder()) {
                  (sbe, f) => sbe.append(Option(f).getOrElse("null").toString)
                }).toString.map(c => c.toByte).toArray
                // записать данные в поток
                output.write(row_str)
              })
            }
          })
        }

      }
      Response.ok(outputStream).build()
  }

}
