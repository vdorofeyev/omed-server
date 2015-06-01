package omed.TimerBF

import java.sql.ResultSet

/**
 * Created with IntelliJ IDEA.
 * User: naryshkinaa
 * Date: 28.03.14
 * Time: 17:31
 * To change this template use File | Settings | File Templates.
 */
case class BFSchedulerMeta (bfId:String,timeout:Int) {

}
object BFSchedulerMeta {
  def apply (dbResult:ResultSet):BFSchedulerMeta={
    val bfId= dbResult.getString("BusinessFunctionID")
    val period = dbResult.getInt("Period")
    new BFSchedulerMeta(bfId,period)
  }
}
