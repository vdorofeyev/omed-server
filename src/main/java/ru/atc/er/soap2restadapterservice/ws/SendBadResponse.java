
package ru.atc.er.soap2restadapterservice.ws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Ответ в случае возникновения ошибки
 *                     
 * 
 * <p>Java class for SendBadResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SendBadResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="MessageData" type="{http://soap2RestAdapterService.er.atc.ru/ws}MessageDataBadResponse"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SendBadResponse", propOrder = {
    "messageData"
})
public class SendBadResponse {

    @XmlElement(name = "MessageData", required = true)
    protected MessageDataBadResponse messageData;

    /**
     * Gets the value of the messageData property.
     * 
     * @return
     *     possible object is
     *     {@link MessageDataBadResponse }
     *     
     */
    public MessageDataBadResponse getMessageData() {
        return messageData;
    }

    /**
     * Sets the value of the messageData property.
     * 
     * @param value
     *     allowed object is
     *     {@link MessageDataBadResponse }
     *     
     */
    public void setMessageData(MessageDataBadResponse value) {
        this.messageData = value;
    }

}
