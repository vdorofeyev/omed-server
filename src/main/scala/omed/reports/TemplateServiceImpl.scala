package omed.reports

import omed.db.{DBProfiler, DB, DataAccessSupport, ConnectionProvider}
import omed.system.ContextProvider
import com.google.inject.Inject

/**
 * Реализация провайдера описания шаблонов отчетных форм
 */
class TemplateServiceImpl extends TemplateService with DataAccessSupport {

  /**
   * Провайдер подключений к СУБД
   */
  @Inject
  var connectionProvider: ConnectionProvider = null

  /**
   * Провайдер контекста запроса
   */
  @Inject
  var contextProvider: ContextProvider = null

  /**
   * Получает данные справочника по указанному идентификатору
   * @param templateId  Идентификатор шаблона
   * @return Описание шаблона
   */
  def getTemplateDescription(templateId: String) = {
    connectionProvider.withConnection {
      connection =>
        val params = List(
          "TemplateID" -> templateId
        )

        DBProfiler.profile("[_Object].[GetTemplateDataByID]") {
          val rs = dataOperation {
            DB.dbExec(
              connection,
              "[_Object].[GetTemplateDataByID]",
              contextProvider.getContext.sessionId,
              params)
          }

          if (rs.next()) {
            new TemplateDescription(
              templateId,
              rs.getString("Name"),
              rs.getBytes("Template")
            )
          } else null
        }
    }
  }
}