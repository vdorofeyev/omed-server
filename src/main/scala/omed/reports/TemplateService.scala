package omed.reports

/**
 * Интерфейс провайдера описания шаблонов отчетных форм
 */
trait TemplateService {
  /**
   * Получает данные справочника по указанному идентификатору
   * @param templateId  Идентификатор шаблона
   * @return Описание шаблона
   */
  def getTemplateDescription(templateId: String): TemplateDescription

}