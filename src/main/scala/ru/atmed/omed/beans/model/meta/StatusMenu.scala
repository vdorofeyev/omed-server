package ru.atmed.omed.beans.model.meta

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 05.11.13
 * Time: 16:26
 * To change this template use File | Settings | File Templates.
 */
case class StatusMenu (val name :String,
                       val businessFunctionId :String,
                       val alignment:String,
                       val buttonPosition :String,
                       val sectionId:String,
                       val row :Int,
                       val sortOrder: Int,
                       val buttonGroupId:String) {

}
