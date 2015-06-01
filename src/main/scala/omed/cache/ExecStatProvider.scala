package omed.cache

/**
 * Протокол статистики выполнения запроса
 */
trait ExecStatProvider {
//  добавить информацию о выполненной ХП
    def addExecedSP(name:String,time:Long)
  // добавить информацию о выполнинном блоке на сервере приложений
    def addExecedBlock(name:String,time:Long,startTime:Long)
  //информация о том что будет добавлен блок сервера приложений для вычитания времени выполнения БД
    def addPredExecedBlock(name:String)
    def toXml:String
}
