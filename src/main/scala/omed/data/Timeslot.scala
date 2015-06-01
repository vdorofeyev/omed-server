package omed.data

/**
 * Timeslot of schedule.
 * User: Alexander Kolesnikov
 * Date: 05.04.13
 */
case class Timeslot(
                     id: String,
                     caption: String,
                     start: String,
                     finish: String,
                     resourceId: String,
                     resourceCaption: String,
                     statusId: String,
                     classId: String,
                     color: String = null,
                     color2: String = null,
                     resourceColor: String = null)
