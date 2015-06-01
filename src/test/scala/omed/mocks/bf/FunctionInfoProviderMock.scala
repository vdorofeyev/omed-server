package omed.mocks.bf

import sun.reflect.generics.reflectiveObjects.NotImplementedException

import scala.collection.mutable.HashMap

import omed.bf.{ FunctionInfoProvider, BusinessFunction }

class FunctionInfoProviderMock extends FunctionInfoProvider {

  val functions = new HashMap[String, BusinessFunction]

  def getFunctionInfo(functionId: String): Option[BusinessFunction] =
    this.functions.get(functionId)

}