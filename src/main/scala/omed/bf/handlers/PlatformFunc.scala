package omed.bf.handlers

import scala.Some
import scala.Some
import omed.model.{SimpleValue, Value}
import omed.data.DataTable

/**
 * Платформенные функции
 *
 */
object PlatformFunc {

  protected def evaluateCell(
    data: DataTable, resultSetNumber: Int, rowNumber: Int,
    column: String): Object = {

    if (data == null)
      throw new Exception("Набор данных [DataTable] должен быть не null.")

    val row = try {
      data.data(rowNumber - 1)
    } catch {
      case e@_ => throw new Exception("Не возможно получить строку " + rowNumber.toString +
        " из выборки.", e)
    }
    val columnIndex = try {
      val tR = data.columns.indexWhere(col => (col == column))
      if (tR >= 0) tR else throw new Exception("Колонка с именем " + column + " не найдена.")
    }
    val result = try {
      row(columnIndex).asInstanceOf[Object]
    } catch {
      case e@_ => throw new Exception("Не возможно извлеч данные из строки", e)
    }

    result
  }

  /**
   * Применить функцию для выбранных данных
   */
  def evaluate(exp: String, dataTable: DataTable): Value = {
    val pattern = """#(.*)\((.*)\)""".r

    // получить имя функции и её аргументы
    val r = pattern.findFirstIn(exp) match {
      case Some(pattern(funcName, args)) => (funcName, args)
      case _ => null
    }

    if (r == null)
      throw new Exception

    val evalResult =
      r._1.toLowerCase match {
        // функция "CELL"
        case "cell" => {
          val argPattern = """\b*(.+)\b*,\b*(.+)\b*,\b*@(.+)\b*""".r
          val argPattern(resultSetNumber, rowNumber, columnName) = r._2

          this.evaluateCell(dataTable, resultSetNumber.toInt, rowNumber.toInt, columnName.trim())
        }
        case _ => null
      }

    SimpleValue(evalResult)
  }
}