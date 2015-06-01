
package ru.atc.er.soap2restadapterservice.ws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Запрос в электронную регистратуру
 *                     
 * 
 * <p>Java class for Send complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Send">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="MessageData" type="{http://soap2RestAdapterService.er.atc.ru/ws}MessageDataSendRequest"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Send", propOrder = {
    "messageData"
})
public class Send {

    @XmlElement(name = "MessageData", required = true)
    protected MessageDataSendRequest messageData;

    /**
     * Gets the value of the messageData property.
     * 
     * @return
     *     possible object is
     *     {@link MessageDataSendRequest }
     *     
     */
    public MessageDataSendRequest getMessageData() {
        return messageData;
    }

    /**
     * Sets the value of the messageData property.
     * 
     * @param value
     *     allowed object is
     *     {@link MessageDataSendRequest }
     *     
     */
    public void setMessageData(MessageDataSendRequest value) {
        this.messageData = value;
    }

}
