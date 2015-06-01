package omed.reports

/**
 * Данные о шаблоне отчета
 * @param id Идентификатор шаблона
 * @param name Наименование шаблона
 * @param content Двоичное представление шаблона отчета в формате FastReport
 */
class TemplateDescription (val id: String, val name: String, val content: Array[Byte])