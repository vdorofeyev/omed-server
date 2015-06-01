package omed.bf

/**
 * Created with IntelliJ IDEA.
 * User: rocker
 * Date: 27.07.12
 * Time: 18:02
 * To change this template use File | Settings | File Templates.
 */
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite


@RunWith(classOf[JUnitRunner])
class ClientResultParserTest extends FunSuite {
  test("parseClientBfStepResult from XML") {
    val r = new ClientStep().parseResults(
      "<data>" +
        "<var1>testVar1</var1>" +
        "<var2>testVar2</var2>" +
      "</data>")
    assert(r("var1").toString == "testVar1")
    assert(r("var2").toString == "testVar2")

    val r2 = new ClientStep().parseResults(
      "<data></data>")
    assert(r2 != null)
    assert(r2.count(_ => true) == 0)

    val r3 = new ClientStep().parseResults(
      "<data>" +
        "<var1><t><t1>asdfasdf</t1></t><t><m2>aaa</m2></t></var1>" +
        "</data>")
    assert(r3 != null)
    assert(r3("var1").toString == "<t><t1>asdfasdf</t1></t><t><m2>aaa</m2></t>")
  }

  test("parse empty client step result") {
    val result = new ClientStep().parseResults(
      "<d></d>")
    assert(result.isEmpty)
  }
}
