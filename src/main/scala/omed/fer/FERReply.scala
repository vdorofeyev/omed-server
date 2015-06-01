package omed.fer

import xml.Node
import omed.errors.DataError

/**
 *
 */
trait FERReply {

  val body: scala.xml.Node

  def extractValue(field: String): String = {
    val result = try {
      (body \\ field).head.text
    } catch { case _ => null }

    result
  }
  def checkError{
    val error = extractValue("error")
    val errors = extractValue("errors")
    if(error!=null){
       throw new DataError(String.format("%s",error), -2345);
    }
    if(errors!=null){
      throw new DataError(String.format("%s",errors), -2345);
    }
  }
  def isError: Boolean ={
    val error = extractValue("error")
    val errors = extractValue("errors")
    error != null || errors!=null
  }

}
