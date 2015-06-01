package omed.data

/**
 * Created with IntelliJ IDEA.
 * User: naryshkinaa
 * Date: 21.04.14
 * Time: 17:54
 * To change this template use File | Settings | File Templates.
 */
object DataQuery {
    val ISFQuery =
      """
        |	 select ID,SortOrder,ISFNodeID,Value1,Value2,Value3,Text from ISFData where ObjectID = ? and PropertyCode = ?
      """.stripMargin
}
