package omed.data

import omed.lang.eval._
import omed.model.DataType
import com.google.inject.Inject
import omed.db.{DB, ConnectionProvider}
import omed.system.ContextProvider
import java.sql.{Types, Connection}

/**
 * Фабрика конфигураций, позволяющая строить наборы функций,
 * взаимодействующих со слоем хранения данных
 */
class DataAwareConfiguration {

  @Inject
  var connectionProvider: ConnectionProvider = null

  @Inject
  var contextProvider: ContextProvider = null

  /**
   * Получение конфигурации с функциями, выполняемыми в контексте подключения к СУБД
   * @return Конфигурация
   */
  def forCurrentSession(): Configuration = {
    val generate = new FunctionDecl(
      signature = new FunctionSign(
        name = "generate",
        arguments = Seq(
          new ArgumentSign(DataType.String),
          new ArgumentSign(DataType.String)
        )
      ),
      rettype = DataType.Int,
      impl = args => {
        val generator = args.head.asInstanceOf[String]
        val sequenceOption = args.tail.headOption.map(_.asInstanceOf[String])
        val combinedName = (generator + "." + sequenceOption.map(s => "." + s).getOrElse("")).take(256)

        val params = Map[String, AnyRef]("Sequence" -> combinedName)
          .mapValues(DBUtils.platformValueToDb).toList

        def getResult(conn: Connection): AnyRef = {
          val callTemplate = "{ ? = call _Generator.GenerateValue (@Sequence = ?) }"
          val statement = conn.prepareCall(callTemplate)

          statement.registerOutParameter(1, Types.INTEGER) // выходное значение
          statement.setString(2, combinedName) // название последовательности

          statement.execute()
          statement.getObject(1)
        }

        try {
          connectionProvider withSeparateConnection getResult
        } catch { case _ => null }
      }
    )
    val func = Seq(generate)
    val funcMap = Configuration.standard.functions ++
      func.groupBy(_.signature.name)
    new Configuration(funcMap)
  }
}
