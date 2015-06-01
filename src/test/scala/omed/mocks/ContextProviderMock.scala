package omed.mocks

import omed.system._
import java.util.TimeZone

class ContextProviderMock extends ContextProvider {
  def getContext: Context =
    new Context(
        sessionId = "sessionId-001", 
        domainId = 1,
        hcuId = "123",
        userId = "testAgent", 
        authorId = "Test Agent", 
        request = "test://test/",
        isSuperUser = false,
         timeZone = TimeZone.getTimeZone("GMT+0"),
      roleId= null)
}