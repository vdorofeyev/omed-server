package omed.auth

/**
 * Created by andrejnaryskin on 03.03.14.
 */
case class PermissionData (
                            classId:String,
                            roleId:String,
                            expression:String,
                            action:PermissionType.Value,
                            isAllowed: Boolean) {

}

case class PermissionDataSeq(data:Seq[PermissionData])
