package omed.fer

import java.net.URL
import ru.atc.er.soap2restadapterservice.ws._
import scala.xml._
import javax.xml.transform.dom.DOMSource
import scala.xml.parsing.NoBindingFactoryAdapter
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.TransformerFactory


class FERService(wsdlLocation: String) {

  val serviceFactory = new Soap2RestAdapterService(new URL(wsdlLocation))

  def asXml(dom: org.w3c.dom.Node): Node = {
    val source = new DOMSource(dom)
    val adapter = new NoBindingFactoryAdapter
    val saxResult = new SAXResult(adapter)
    val transformerFactory = TransformerFactory.newInstance()
    val transformer = transformerFactory.newTransformer()
    transformer.transform(source, saxResult)
    adapter.rootElem
  }

  def sendMessage(message: FERMessage): Node = {
    val result = try {
      val service = serviceFactory.getSoap2RestAdapterService()

      val appData = new AppDataSendRequest()
      appData.setMessageCode(message.code)
      appData.setMessage(message.body.getBytes("UTF8"))
      val tmp = 8
      val messageData = new MessageDataSendRequest()
      messageData.setAppData(appData)

      val sendRequest = new Send()
      sendRequest.setMessageData(messageData)

      val sendResponse = service.send(sendRequest)
      sendResponse.getMessageData.getAppData.getAny
    } catch {
      case e @ _ => throw new omed.errors.DataError("Ошибка обращения к сервису ФЭР", 0, e)
    }
    Option(result).map(_.asInstanceOf[org.w3c.dom.Node]).map(asXml).orNull
  }

}
