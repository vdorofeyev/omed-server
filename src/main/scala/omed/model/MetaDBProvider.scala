package omed.model

import scala.collection.mutable.ArrayBuffer
import ru.atmed.omed.beans.model.meta.{ModuleInDomain, ObjectStatusTransition, MetaObjectStatus}
import omed.data.ColorRule

/**
 * Протокол получения системных метаданных из БД
 */
trait MetaDBProvider {
    def loadFieldsFromDb(metaObjects:ArrayBuffer[MetaObject]):(Map[String,ArrayBuffer[MetaField]],Map[String,ArrayBuffer[ArrayField]])
    def loadClassesFromDb:ArrayBuffer[MetaObject]
    def loadStatusesFromDb : List[MetaObjectStatus]
    def loadTransitionsFromDb:Map[String,Seq[ObjectStatusTransition]]
    def loadModuleInDomainFromDb :Seq[ModuleInDomain]
    def loadColorRules: Seq[ColorRule]
}
