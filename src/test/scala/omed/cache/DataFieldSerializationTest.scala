package omed.cache

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import omed.model._
import java.io._

@RunWith(classOf[JUnitRunner])
class DataFieldSerializationTest extends FunSuite {

    test("DataFieldSerializationTest") {
    	val stream = new ByteArrayOutputStream()
      val oos = new ObjectOutputStream(stream)
    	
    	val field = DataField("id1", "name2", DataType.Int)
    	//val mo = MetaObject("str1", "str2", "str3", List[MetaField](), "str4")
    	
    	oos.writeObject(field)
    	oos.flush()

      val str = stream.toString
      val len = str.length

    	val inputStream = new ByteArrayInputStream(stream.toByteArray())
    	val ois = new ObjectInputStream(inputStream)
    	val newField = ois.readObject().asInstanceOf[DataField]
    	
    	assert(field.id == newField.id)
    	assert(field.code == newField.code)
    	assert(field.dataType == newField.dataType)
    }
}