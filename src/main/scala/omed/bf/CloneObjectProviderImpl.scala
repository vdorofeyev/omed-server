package omed.bf

import omed.model.{SimpleValue, EntityInstance, Value}
import com.google.inject.Inject
import omed.data.{DataReaderService, DataWriterService}

/**
 * Created with IntelliJ IDEA.
 * User: NaryshkinAA
 * Date: 18.09.13
 * Time: 10:39
 * To change this template use File | Settings | File Templates.
 */
class CloneObjectProviderImpl extends CloneObjectProvider{
  @Inject
  var dataWriter:DataWriterService = null
  @Inject
  var dataReader:DataReaderService = null

//  def cloneObject(destination:EntityInstance,data:Map[String,Any]){
//    val destinationId = destination match {
//      case e:EntityInstance => e.getId
//      case v :SimpleValue => v.toString
//    }
//    val classId = data("_ClassID").asInstanceOf[String]
//    //аттрибуты которые не должны быть скопированы
//    val noCopyFields = Set("ID","_ClassID","_ChangeDate","_CreateDate","_ChangeUserID","_CreateUserID","_Name","LockTime","_Deleted","_Domain")
//    data.filter(p=> !(noCopyFields contains p._1) && !p._1.endsWith("$")).foreach(f=>dataWriter.directSaveField(classId,destinationId,f._1,Option(f._2).map(_.toString).getOrElse(null)))
//  }

  def cloneObject(destination:EntityInstance,data:Map[String,Any]){
    val destinationId = destination.getId
    val classId =data("_ClassID").asInstanceOf[String]
    //аттрибуты которые не должны быть скопированы
    val noCopyFields = Set("ID","_ClassID","_ChangeDate","_CreateDate","_ChangeUserID","_CreateUserID","_Name","LockTime","_Deleted","_Domain")
    data.filter(p=> !(noCopyFields contains p._1) && !p._1.endsWith("$")).foreach(f=>dataWriter.directSaveField(classId,destinationId,f._1,Option(f._2).map(_.toString).getOrElse(null)))
//    val entitySource = source match {
//       case e:EntityInstance => e.data
//       case v :SimpleValue => dataReader.getObjectData(objectId = v.toString)
//   }
//    cloneObject(destination,entitySource)
  }
}
