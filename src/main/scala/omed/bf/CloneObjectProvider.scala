package omed.bf

import omed.model.{EntityInstance, Value}

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 18.09.13
 * Time: 10:50
 * To change this template use File | Settings | File Templates.
 */
trait CloneObjectProvider {
  def cloneObject(destination:EntityInstance,data:Map[String,Any])
//  def cloneObject(destination:EntityInstance,source:EntityInstance)
}
