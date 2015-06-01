package ru.atmed.omed.beans.model.meta

import java.sql.ResultSet

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 07.02.14
 * Time: 15:39
 * To change this template use File | Settings | File Templates.
 */
case class ModuleInDomain (domain:Int,moduleId:String) {

}
object  ModuleInDomain{
  def apply(resultSet:ResultSet) : ModuleInDomain={
      new ModuleInDomain(resultSet.getInt("Number"),resultSet.getString("ModuleID"))
  }
}

case class ModuleInDomainSeq(data:Seq[ModuleInDomain])