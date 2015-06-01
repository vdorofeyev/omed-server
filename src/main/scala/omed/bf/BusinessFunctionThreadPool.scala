package omed.bf


import scala.collection.mutable

/**
 * Стек выполнения БФ. При циклическом вызовы происходит exception
 */
class BusinessFunctionThreadPool{
  val pool :  mutable.Stack[String] =  mutable.Stack[String]()
  var rootProcessId : String = null
  def containBF(bfId:String):Boolean = {
    pool.contains(bfId)
  }
  def addBF(bfId:String){
    pool.push(bfId)
  }
  def deleteBF{
    pool.pop()
  }
//  def getName:String={
//    test.get()
//  }
  def clearPool{
    pool.clear()
    rootProcessId = null
  }
  def getRootProcessId:Option[String]={
    Option(rootProcessId)
  }
  def setRootProcessId(id:String){
    rootProcessId = id
  }
//  def setName{
//    test.set(Thread.currentThread().getName())
//  }
}