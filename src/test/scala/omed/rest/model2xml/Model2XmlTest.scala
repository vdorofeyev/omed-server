package omed.rest.model2xml

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import omed.model._
import omed.data.{DataViewRow, DataViewTable}

@RunWith(classOf[JUnitRunner])
class Model2XmlTest extends FunSuite {

  test("getEditRecordRequest from XML 1") {
    val r = new Model2Xml().parseEditReq(
      "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" +
        "<editedField>" +
        "        <id>9C69DF2B-1AE0-49BC-BB93-D04E53E0EE4A</id>" +
        "        <value></value>" +
        "</editedField>")
    assert(Map("9C69DF2B-1AE0-49BC-BB93-D04E53E0EE4A"->"") == r)
  }

  test("getEditRecordRequest from XML 2") {
    val r = new Model2Xml().parseEditReq(
      "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" +
        "<editedField>" +
        "        <id>9C69DF2B-1AE0-49BC-BB93-D04E53E0EE4A</id>" +
        "        <value/>" +
        "</editedField>")
    assert(Map("9C69DF2B-1AE0-49BC-BB93-D04E53E0EE4A"-> "") == r)
  }

  test("getEditRecordRequest from XML 3") {
    val r = new Model2Xml().parseEditReq(
      "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" +
        "<editedField>" +
        "        <id>9C69DF2B-1AE0-49BC-BB93-D04E53E0EE4A</id>" +
        "</editedField>")
    assert(Map("9C69DF2B-1AE0-49BC-BB93-D04E53E0EE4A"-> null) == r)
  }

  test("getEditRecordRequest from XML 4") {
    val r = new Model2Xml().parseEditReq(
      "<editedField xmlns:i=\"http://www.w3.org/2001/XMLSchema-instance\">" +
        "        <id>73283216-5568-4280-9CC7-84AEE1062D13</id>" +
        "        <value i:nil=\"true\"/>" +
        "</editedField>")
    assert(Map("73283216-5568-4280-9CC7-84AEE1062D13"-> null) == r)
  }

  test("getEditRecordRequest from XML 5") {
    val r = new Model2Xml().parseEditReq(
      "<editedField xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
        "        <id>73283216-5568-4280-9CC7-84AEE1062D13</id>" +
        "        <value xsi:nil=\"true\"/>" +
        "</editedField>")
    assert(Map("73283216-5568-4280-9CC7-84AEE1062D13"-> null) == r)
  }

  test("clearXmlFromHeader test") {
    var r = new Model2Xml().clearXmlFromHeader(
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
        "<variables>" +
        "    <EpisodeID>" +
        "        <object ID=\"C8185E7F-3769-4A9A-AAF2-9290D2F24BEE\" />" +
        "    </EpisodeID>" +
        "</variables>")

    assert(r == "<variables>    <EpisodeID>        <object ID=\"C8185E7F-3769-4A9A-AAF2-9290D2F24BEE\"></object>    </EpisodeID></variables>")

    r = new Model2Xml().clearXmlFromHeader(
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<variables>" +
        "    <EpisodeID>" +
        "        <object ID=\"C8185E7F-3769-4A9A-AAF2-9290D2F24BEE\" />" +
        "    </EpisodeID>" +
        "</variables>")

    assert(r == "<variables>    <EpisodeID>        <object ID=\"C8185E7F-3769-4A9A-AAF2-9290D2F24BEE\"></object>    </EpisodeID></variables>")
  }

  test("metaClassesToXml test") {

    val thisObj = MetaObject("Person",
      DataField(null, "a", DataType.String) ::
        DataField(null, "b", DataType.String) :: Nil)

    val thatObj = MetaObject("Sex",
      DataField(null, "c", DataType.Int) :: Nil)

    var r = new Model2Xml().metaClassesToXml(List(thisObj, thatObj))

    assert(r ==
      "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" +
      "<classes>" +
      "<class>" +
      "<code>Person</code>" + "<name>Person</name>" +
      "<fields>" +
      "<field><code>a</code><name>a</name><metaType>string</metaType><type>value</type></field>" +
      "<field><code>b</code><name>b</name><metaType>string</metaType><type>value</type></field>" +
      "</fields>" +
      "</class>" +
      "<class>" +
      "<code>Sex</code>" + "<name>Sex</name>" +
      "<fields>" +
      "<field><code>c</code><name>c</name><metaType>int</metaType><type>value</type></field>" +
      "</fields>" +
      "</class>" +
      "</classes>")

  }

  test("parsing _Meta_BFSSetAttributeValue step definition") {
    val xmlStr = """
		<_Meta_BFSSetAttributeValue
		ID="11101317-37A6-4E1A-8098-F7D692FB0762"
		BusinessFunctionID="5E663DA7-696B-49F7-BFFF-C07A80FDBC06"
		Destination="@this.PatientFIO" 
		Name="Получение списка идентификаторов модулей"
		SourceExp="@this.PatientID.LastName + &quot; &quot; + SUBSTRING(@this.PatientID.FirstName, 1, 1) 
		+ &quot;. &quot; + SUBSTRING(@this.PatientID.SecondName, 1, 1) +
		&quot;. &quot;"
		StepNumber="1" />
    """

    import scala.xml._
    val xml = XML.loadString(xmlStr)
    assert(xml != null)

    assert(xml.label == "_Meta_BFSSetAttributeValue")

    val destination = xml.attribute("Destination")
      .map(_.first).map(_.text).orNull

    val sourceExp = xml.attribute("SourceExp")
      .map(_.first).map(_.text).orNull

    assert(destination != null)
    assert(sourceExp != null)
  }

  test("parsing _Meta_BFSTransition step definition") {
    val xmlStr = """
		<_Meta_BFSTransition
		ID="457CDA80-2AFF-1275-9B37-1524B37BD1D1"
		BusinessFunctionID="B1672F10-2AFB-1275-93B2-12FECA17499C"
		TransitionID="3E19A020-2AFA-1275-993E-1138A87F76C6"
		Name="Переход В работу"
		StepNumber="1" />
    """

    import scala.xml._
    val xml = XML.loadString(xmlStr)
    assert(xml != null)

    assert(xml.label == "_Meta_BFSTransition")

    val transitionID = xml.attribute("TransitionID")
      .map(_.first).map(_.text).orNull

    assert(transitionID != null)
  }

  test("parsing tree vars") {
    var xmlStr = """
        <vars>
            <variables><t>1</t></variables>
            <treeVariables><t2>1</t2></treeVariables>
        </vars>
    """
    var r = new Model2Xml().parseTreeVars(xmlStr)

    assert(r != null)
    assert(r._1 == "<variables><t>1</t></variables>")
    assert(r._2 == "<variables><t2>1</t2></variables>")

    xmlStr = """<?xml version="1.0" encoding="UTF-8" ?>
      <vars>
          <variables></variables>
          <treeVariables><FirstName>Антонов</FirstName><LastName></LastName><SecondName></SecondName></treeVariables>
     </vars>
    """

    r = new Model2Xml().parseTreeVars(xmlStr)

    assert(r != null)
    assert(r._1 == "<variables></variables>")
    assert(r._2 == "<variables><FirstName>Антонов</FirstName><LastName></LastName><SecondName></SecondName></variables>")

  }

  test("serialize data with ovverided fields") {

    var rs = new DataViewTable(
      columns = Seq("ID", "Name", "Gender"),
      binaryItems = Seq.empty[Int],
      data = Seq(
        new DataViewRow(Array("recordId-9012312312", "Пётр", "М"), null, null, Array(null, null, null))))

    var hiddenFields: Set[String] = Set()

    var r = new Model2Xml().convert(rs, hiddenFields)
    assert(r != null)
    assert(r ==
      "<result><object><ID>recordId-9012312312</ID><Name>Пётр</Name><Gender>М</Gender></object><Action></Action></result>")

    rs = new DataViewTable(
      columns = Seq("ID", "Name", "Gender", "LastName", "Birth", "Street"),
      binaryItems = Seq.empty[Int],
      data = Seq(
        new DataViewRow(Array("recordId-9012312312", "Пётр", "М", null, null),
          Map("Name" -> Map("isReadOnly" -> Option(true)),
            "LastName" -> Map("isReadOnly" -> Option(true)),
            "Birth" -> null,
            "Street" -> Map(),
            "Gender" -> Map("isReadOnly" -> true, "color" -> "black")),
          null,
          Array(null, null, null, null, null)
        )
      )
    )

    r = new Model2Xml().convert(rs, hiddenFields)
    assert(r != null)
    assert(r ==
      "<result><object><ID>recordId-9012312312</ID><Name isReadOnly=\"true\">Пётр</Name><Gender isReadOnly=\"true\" color=\"black\">М</Gender><LastName isReadOnly=\"true\"></LastName></object><Action></Action></result>")
  }
}