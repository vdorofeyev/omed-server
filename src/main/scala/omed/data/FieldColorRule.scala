package omed.data

import java.sql.ResultSet

/**
 * Правило окрашивания ячейки
 * (колонка определяется по propertyId)
 * @param propertyId Идентификатор поля, для которого задано правило
 */
class FieldColorRule(
  override val name: String,
  override val condition: String,
  override val classId: String,
  override val color: String,
  override val color2: String,
  override val priority: Int,
  val propertyId: String,
  val propertyCode: String
) extends ColorRule(
    name,
    condition,
    classId,
    color,
    color2,
    priority) with Serializable


object FieldColorRule {
  def apply(dbResult:ResultSet):FieldColorRule={
    val name = dbResult.getString("Name")
    val condition = dbResult.getString("ConditionString")
    val classId = Option(dbResult.getObject("ClassID")).map(x => x.toString).orNull
    val color = dbResult.getString("Color")
    val priority = Option(dbResult.getInt("Priority")).getOrElse(0)
    val propertyId = dbResult.getString("PropertyID")
    val propertyCode = dbResult.getString("Property_Code")
    new FieldColorRule(
      name, condition, classId, color,null, priority, propertyId, propertyCode)
  }
}
