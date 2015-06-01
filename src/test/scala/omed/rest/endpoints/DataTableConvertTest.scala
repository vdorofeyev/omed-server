package omed.rest.endpoints

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import omed.rest.model2xml.Model2Xml
import omed.data.{DataViewRow, DataViewTable}

@RunWith(classOf[JUnitRunner])
class DataTableConvertTest extends FunSuite {

  test("DataTable conversion to XML") {
    val columns = IndexedSeq[String]("A", "B", "C")
    val binaries = Seq[Int]()
    val dataBuffer = IndexedSeq(
        new DataViewRow(Array(1, 2, "X"), null, null, Array(null, null, null)),
        new DataViewRow(Array(3, 4, "Y"), null, null, Array(null, null, null)))

    val dt = new DataViewTable(columns, binaries, dataBuffer,null)

    val ds = new DataService() {
      def pub_convert(dt: DataViewTable, hiddenFields: Set[String]) =
        new Model2Xml().convert(dt, hiddenFields)
    }

    val result = ds.pub_convert(dt, Set())
    assert(result != null)
    assert(result == "<result><object><A>1</A><B>2</B><C>X</C></object><object><A>3</A><B>4</B><C>Y</C></object><Action></Action></result>")

  }
}