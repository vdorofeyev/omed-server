package ru.atmed.omed.beans.model.meta


case class StatusWindowGrid(id: String,
                            statusId: String,
                            windowGridId: String,
                            isVisible: Boolean,
                            isDeleteAllowed: Boolean,
                            isInsertAllowed: Boolean,
                            isEditAllowed: Boolean)


case class StatusWindowGridSeq(data:Seq[StatusWindowGrid])