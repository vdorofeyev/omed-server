package omed.cache

import org.joda.time.DateTime

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 09.09.13
 * Time: 13:00
 * To change this template use File | Settings | File Templates.
 */
 case class ExecedBlock(begin:Long = System.currentTimeMillis(),var end:Long = 0) {

}
