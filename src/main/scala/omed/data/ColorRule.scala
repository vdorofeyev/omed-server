package omed.data

import java.sql.ResultSet

/**
 * Правило окрашивания.
 * Задаёт правила окрашивания строк при отображении на форме-списке
 *
 * @param name Имя правила
 * @param condition Условие для применения окрашивания
 * @param classId Идентификатор метакласса, для которого работает это правило
 * @param color Цвет окрашивания
 * @param priority Приоритет правила
 */
class ColorRule(
  val name: String,
  val condition: String,
  val classId: String,
  val color: String,
  val color2: String,
  val priority: Int) extends Serializable

object ColorRule{
  def apply(dbResult:ResultSet):ColorRule = {
    val name = dbResult.getString("Name")
    val condition = dbResult.getString("ConditionString")
    val classId = Option(dbResult.getObject("ClassID")).map(x => x.toString).orNull
    val color = dbResult.getString("Color")
    val color2 = dbResult.getString("Color2")
    val priority = Option(dbResult.getInt("Priority")).getOrElse(0)
    new ColorRule(name, condition, classId, color,color2, priority)
  }
}