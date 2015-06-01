package ru.atmed.omed.beans.model.meta

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 25.11.13
 * Time: 17:40
 * To change this template use File | Settings | File Templates.
 */
case class CardInCardItem (id:String, viewCardId :String,caption:String,sortOrder:Int,groupId:String, insertViewCardId : String, gridId:String,filter:String) {

}


case class CardInCardItemSeq(data: Seq[CardInCardItem])