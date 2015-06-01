package ru.atmed.omed.beans.model.meta

/**
 * Статус метаобъекта
 *
 * @param id Идентификатор
 * @param name Название
 * @param isNew Признак "Новый"
 */
case class MetaObjectStatus(
  val id: String,
  val name: String,
  val isNew: Boolean,
  val diagrammId:String,
  val isEditNotAllowed: Boolean,
  val isDeleteNotAllowed: Boolean,
  val defaultTabId:String = null,
  val predNotificationDesriptions :Seq[PredNotificationDescription] =Seq()                           )

case class MetaObjectStatusSeq(data: Seq[MetaObjectStatus])

case class PredNotificationDescription(val id:String,val statusId:String,val notificationGroupId:String,val maxTransitionTime : Int)

