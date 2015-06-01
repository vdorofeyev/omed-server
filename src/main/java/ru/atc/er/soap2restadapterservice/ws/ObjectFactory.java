
package ru.atc.er.soap2restadapterservice.ws;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the ru.atc.er.soap2restadapterservice.ws package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _SendRequest_QNAME = new QName("http://soap2RestAdapterService.er.atc.ru/ws", "SendRequest");
    private final static QName _SendBadResponse_QNAME = new QName("http://soap2RestAdapterService.er.atc.ru/ws", "SendBadResponse");
    private final static QName _SendResponse_QNAME = new QName("http://soap2RestAdapterService.er.atc.ru/ws", "SendResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: ru.atc.er.soap2restadapterservice.ws
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link SendResponse }
     * 
     */
    public SendResponse createSendResponse() {
        return new SendResponse();
    }

    /**
     * Create an instance of {@link Send }
     * 
     */
    public Send createSend() {
        return new Send();
    }

    /**
     * Create an instance of {@link SendBadResponse }
     * 
     */
    public SendBadResponse createSendBadResponse() {
        return new SendBadResponse();
    }

    /**
     * Create an instance of {@link AppDataBadResponse }
     * 
     */
    public AppDataBadResponse createAppDataBadResponse() {
        return new AppDataBadResponse();
    }

    /**
     * Create an instance of {@link MessageDataSendResponse }
     * 
     */
    public MessageDataSendResponse createMessageDataSendResponse() {
        return new MessageDataSendResponse();
    }

    /**
     * Create an instance of {@link AppDataSendRequest }
     * 
     */
    public AppDataSendRequest createAppDataSendRequest() {
        return new AppDataSendRequest();
    }

    /**
     * Create an instance of {@link AppDataSendResponse }
     * 
     */
    public AppDataSendResponse createAppDataSendResponse() {
        return new AppDataSendResponse();
    }

    /**
     * Create an instance of {@link MessageDataBadResponse }
     * 
     */
    public MessageDataBadResponse createMessageDataBadResponse() {
        return new MessageDataBadResponse();
    }

    /**
     * Create an instance of {@link MessageDataSendRequest }
     * 
     */
    public MessageDataSendRequest createMessageDataSendRequest() {
        return new MessageDataSendRequest();
    }

    /**
     * Create an instance of {@link Error }
     * 
     */
    public Error createError() {
        return new Error();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Send }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://soap2RestAdapterService.er.atc.ru/ws", name = "SendRequest")
    public JAXBElement<Send> createSendRequest(Send value) {
        return new JAXBElement<Send>(_SendRequest_QNAME, Send.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SendBadResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://soap2RestAdapterService.er.atc.ru/ws", name = "SendBadResponse")
    public JAXBElement<SendBadResponse> createSendBadResponse(SendBadResponse value) {
        return new JAXBElement<SendBadResponse>(_SendBadResponse_QNAME, SendBadResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SendResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://soap2RestAdapterService.er.atc.ru/ws", name = "SendResponse")
    public JAXBElement<SendResponse> createSendResponse(SendResponse value) {
        return new JAXBElement<SendResponse>(_SendResponse_QNAME, SendResponse.class, null, value);
    }

}
