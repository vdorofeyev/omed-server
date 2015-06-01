package omed.auth

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import omed.system.{Context, ContextProvider}

@RunWith(classOf[JUnitRunner])
class PermissionMetaProviderTest extends FunSuite {
  test("Get user permissions") {

    val contextProvider1 = new ContextProvider {
      def getContext = new Context(userId = "userId", authorId = "authorId",
        sessionId = "", domainId = 1, hcuId = "", request = "", isSuperUser = false, timeZone = null,roleId= "role1")
    }
    val contextProvider2 = new ContextProvider {
      def getContext = new Context(userId = "userId", authorId = "authorId",
        sessionId = "", domainId = 1, hcuId = "", request = "", isSuperUser = false, timeZone = null,roleId= "role2")
    }
    // user with roles 1, 2, 3, 5, 6 (except 4)
    val permReader = new PermissionReader {
      def getUserRoles(userId: String) = {
        val roles = Map("userId" -> Nil, "authorId" -> Seq("role1", "role2"))
        roles(userId)
      }
      def getDataClassPermissions(classId:String):Seq[PermissionData]={
        Seq()
      }
      def getAllRoles =
        UserRole("role1", "role2", "role1name") ::
          UserRole("role2", null, "role2name") ::
          UserRole("role3", null, "role3name") :: Nil

      def getAllPermissions =
        PermissionMeta("id1", "role1", "obj1", "", PermissionType.ReadExec, true) ::
          PermissionMeta("id0", "role3", "obj1", "", PermissionType.Write, true) ::
          PermissionMeta("id2", "role2", "obj1", "", PermissionType.ReadExec, false) ::
          // 2
          PermissionMeta("id3", "role1", "obj2", "", PermissionType.ReadExec, true) ::
          // 3
          PermissionMeta("id4", "role2", "obj3", "", PermissionType.Write, true) ::
          // 4
          PermissionMeta("id5", "role2", "obj4", "", PermissionType.ReadExec, true) ::
          PermissionMeta("id6", "role1", "obj4", "", PermissionType.Write, false) ::
          PermissionMeta("id7", "role2", "obj4", "", PermissionType.Write, true) ::
          // 5
          PermissionMeta("id9", "role1", "obj5", "", PermissionType.Write, false) ::
          // 6
          PermissionMeta("id11", "role1", "obj6", "", PermissionType.ReadExec, false) ::
          PermissionMeta("id12", "role2", "obj6", "", PermissionType.ReadExec, true) ::
          PermissionMeta("id14", "role2", "obj6", "", PermissionType.Write, true) ::    Nil
      def getObjectPermissions(objectId:String) : Seq[PermissionMeta]={
          getAllPermissions.filter(_.objectId == objectId)
      }
    }

    val permProvider = new PermissionProviderImpl()
    permProvider.contextProvider = contextProvider1
    permProvider.permissionReader = permReader
    assert(permProvider.getMetaPermission("obj0000000").forall( f=> !f._2),"must be all false" )
    permProvider.contextProvider = contextProvider2
    assert(permProvider.getMetaPermission("obj1").forall( f=> !f._2),"проверка на запрет на чтение" )
    assert(permProvider.getMetaPermission("obj2").filter(_._2).keySet == Set(PermissionType.ReadExec),"доступно только чтение")
    assert(permProvider.getMetaPermission("obj3").forall( f=> f._2),"проверка на разрешение на чтение и запись" )
    assert(permProvider.getMetaPermission("obj4").filter(_._2).keySet == Set(PermissionType.ReadExec),"проверка на запрет на запись")
    assert(permProvider.getMetaPermission("obj5").forall( f=> !f._2),"запрет на запись" )
    assert(permProvider.getMetaPermission("obj6").forall( f=> !f._2),"проверка за запрет на чтение при разрешения на запись" )

    //assert(permProvider.getPermissions("obj7") == Set(PermissionType.Write))
//    assert(permProvider.getPermissions("obj7").filter(_._2).keySet == PermissionType.values.toSet)
 //   assert(permProvider.getPermissions("obj8").filter(_._2).keySet == PermissionType.values.toSet)
  //  assert(permProvider.getPermissions("obj9").filter(_._2).keySet == PermissionType.values.toSet)
  }

  test("Get super-user permissions") {

    val contextProvider = new ContextProvider {
      def getContext = new Context(userId = "userId", authorId = "authorId",
        sessionId = "", domainId = 1, hcuId = "", request = "", isSuperUser = true, timeZone = null,roleId= null)
    }

    val permProvider = new PermissionProviderImpl()
    permProvider.contextProvider = contextProvider
    permProvider.permissionReader = null

    assert(permProvider.getMetaPermission("obj0000000").filter(_._2).keySet == PermissionType.values.toSet)
  }
}
