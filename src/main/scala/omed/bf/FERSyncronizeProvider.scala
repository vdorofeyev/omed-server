package omed.bf

import omed.fer.HL7PatientRecordMessage

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 02.10.13
 * Time: 18:05
 * To change this template use File | Settings | File Templates.
 */
trait FERSyncronizeProvider {
  /**
   * заполнение всех не занятых таймслотов из ФЭР за определенный день (ФИО не проставляется)
   * @param slots
   * @param date
   */
    def addFerSlots(slots:Map[String,Map[String,Boolean]],date:String)

  /**
   * добавление записи из ФЭР (проверка на свободность таймслота производится заранее). При отсутсвии необходимого пациента создается новый
   * @param hl7Message
   */
    def addRecord(hl7Message:HL7PatientRecordMessage)

  /**
   * Получение свободных таймслотов для заданного расписания, даты, и набора таймслотов
   * @param locationId
   * @param times
   * @param date
   * @return
   */
    def getSlot(locationId:String, times: Set[String],date:String) :Seq[String]
}
