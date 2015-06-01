
package ru.atc.er.soap2restadapterservice.ws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Возвращает данные, полученные от электронной регистратуры
 *                     
 * 
 * <p>Java class for SendResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SendResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="MessageData" type="{http://soap2RestAdapterService.er.atc.ru/ws}MessageDataSendResponse"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SendResponse", propOrder = {
    "messageData"
})
public class SendResponse {

    @XmlElement(name = "MessageData", required = true)
    protected MessageDataSendResponse messageData;

    /**
     * Gets the value of the messageData property.
     * 
     * @return
     *     possible object is
     *     {@link MessageDataSendResponse }
     *     
     */
    public MessageDataSendResponse getMessageData() {
        return messageData;
    }

    /**
     * Sets the value of the messageData property.
     * 
     * @param value
     *     allowed object is
     *     {@link MessageDataSendResponse }
     *     
     */
    public void setMessageData(MessageDataSendResponse value) {
        this.messageData = value;
    }

}
