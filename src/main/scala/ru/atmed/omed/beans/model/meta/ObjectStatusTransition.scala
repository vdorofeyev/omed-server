package ru.atmed.omed.beans.model.meta

/**
 * Переход объекта от статуса к статусу.
 * 
 * @param id ИД перехода
 * @param statusDiagramID Диаграмма статусов, в рамках которой осуществляются переходы
 * @param beginStatusID Статус из которого переходим
 * @param endStatusID Статус в который переходим
 * @param condition Условие перехода
 * @param moduleID ИД модуля
 */
case class ObjectStatusTransition(
  id: String,
  statusDiagramID: String,
  beginStatusID: String,
  endStatusID: String,
  condition: String,
  moduleID: String,
  classID:String
  )

case class ObjectStatusTransitionSeq(data :Seq[ObjectStatusTransition])