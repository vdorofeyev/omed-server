
package ru.atc.er.soap2restadapterservice.ws;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceException;


@WebServiceClient(name = "Soap2RestAdapterService", targetNamespace = "http://soap2RestAdapterService.er.atc.ru/ws")
public class Soap2RestAdapterService
    extends Service
{
    private final static QName SOAP2RESTADAPTERSERVICE_SERVICE_QNAME =
            new QName("http://soap2RestAdapterService.er.atc.ru/ws", "Soap2RestAdapterService");
    public static final QName SOAP2RESTADAPTERSERVICE_PORT_QNAME =
            new QName("http://soap2RestAdapterService.er.atc.ru/ws", "Soap2RestAdapterService");

    // "http://adapter-fer.rosminzdrav.ru/misAdapterService/ws/MisAdapterService?wsdl"

    public Soap2RestAdapterService(URL wsdlLocation) {
        super(wsdlLocation, SOAP2RESTADAPTERSERVICE_SERVICE_QNAME);
    }

    /**
     * 
     * @return
     *     returns Soap2RestAdapterServiceInterface
     */
    @WebEndpoint(name = "Soap2RestAdapterService")
    public Soap2RestAdapterServiceInterface getSoap2RestAdapterService() {
        return super.getPort(SOAP2RESTADAPTERSERVICE_PORT_QNAME, Soap2RestAdapterServiceInterface.class);
    }

}
