package omed.bf

import omed.lang.eval._
import omed.model.DataType
import java.sql.{Types, Connection}

/**
 * Набор функций, связанных с бизнес-логикой
 */
object BusinessAwareConfiguration {

  /**
   * Проверяет корректность номера СНИЛС по контрольной сумме
   *
   * 1) Проверка контрольного числа Страхового номера проводится только для номеров больше номера 001—001-998
   * 2) Контрольное число СНИЛС рассчитывается следующим образом:
   * 2.1) Каждая цифра СНИЛС умножается на номер своей позиции (позиции отсчитываются с конца)
   * 2.2) Полученные произведения суммируются
   * 2.3) Если сумма меньше 100, то контрольное число равно самой сумме
   * 2.4) Если сумма равна 100 или 101, то контрольное число равно 00
   * 2.5) Если сумма больше 101, то сумма делится нацело на 101 и контрольное число определяется остатком от деления аналогично пунктам 2.3 и 2.4
   *
   * @param raw Строка входных данных
   * @return Является ли аргумент номером СНИЛС
   */
  def checkSnils(raw: String): Boolean = {
    val snils = Option(raw).getOrElse("").replaceAll("\\s+|-", "")
    if (!snils.matches("\\d{11}")) return false

    val body = snils.take(9)
    if (body < "001001998") return true

    val controlSum = snils.takeRight(2).toInt
    val pairs = body.map(_.toString.toInt) zip (1 to 9).reverse
    val sum = pairs.map(p => p._1 * p._2).sum % 101 % 100

    sum == controlSum
  }

  /**
   * Проверяет корректность ИНН по контрольной сумме
   *
   * 1.     Вычисляется контрольная сумма по 11-ти знакам со следующими весовыми коэффициентами: (7,2,4,10,3,5,9,4,6,8,0)
   * 2.     Вычисляется контрольное число(1) как остаток от деления контрольной суммы на 11
   * 3.     Если контрольное число(1) больше 9, то контрольное число(1) вычисляется как остаток от деления контрольного числа(1) на 10
   * 4.     Вычисляется контрольная сумма по 12-ти знакам со следующими весовыми коэффициентами: (3,7,2,4,10,3,5,9,4,6,8,0).
   * 5.     Вычисляется контрольное число(2) как остаток от деления контрольной суммы на 11
   * 6.     Если контрольное число(2) больше 9, то контрольное число(2) вычисляется как остаток от деления контрольного числа(2) на 10
   * 7.     Контрольное число(1) проверяется с одиннадцатым знаком ИНН и контрольное число(2) проверяется с двенадцатым знаком ИНН. В случае их равенства ИНН считается правильным.
   *
   * @param raw Строка входных данных
   * @return Является ли аргумент номером ИНН
   */
  def checkInn(raw: String): Boolean = {
    val inn = Option(raw).getOrElse("").replaceAll("\\s+|-", "")
    if (!inn.matches("\\d{12}")) return false

    val part1 = inn.take(10)
    val part2 = inn.take(11)

    val controlSum1 = inn.charAt(10).toString.toInt
    val pairs1 = part1.map(_.toString.toInt) zip Seq(7, 2, 4, 10, 3, 5, 9, 4, 6, 8)
    val sum1 = pairs1.map(p => p._1 * p._2).sum % 11 % 10

    val controlSum2 = inn.charAt(11).toString.toInt
    val pairs2 = part2.map(_.toString.toInt) zip Seq(3, 7, 2, 4, 10, 3, 5, 9, 4, 6, 8)
    val sum2 = pairs2.map(p => p._1 * p._2).sum % 11 % 10

    sum1 == controlSum1 && sum2 == controlSum2
  }

  val custom: Configuration = {
    val checksnils = new FunctionDecl(
      signature = new FunctionSign(
        name = "checksnils",
        arguments = Seq(
          new ArgumentSign(DataType.String)
        )
      ),
      rettype = DataType.Boolean,
      impl = args => {
        val raw = args.head.asInstanceOf[String]
        checkSnils(raw).asInstanceOf[AnyRef]
      }
    )

    val checkinn = new FunctionDecl(
      signature = new FunctionSign(
        name = "checkinn",
        arguments = Seq(
          new ArgumentSign(DataType.String)
        )
      ),
      rettype = DataType.Boolean,
      impl = args => {
        val raw = args.head.asInstanceOf[String]
        checkInn(raw).asInstanceOf[AnyRef]
      }
    )

    val func = Seq(checksnils, checkinn)
    val funcMap = func.groupBy(_.signature.name)
    new Configuration(funcMap)
  }
}
