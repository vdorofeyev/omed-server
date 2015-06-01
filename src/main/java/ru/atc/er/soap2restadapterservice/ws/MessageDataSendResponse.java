
package ru.atc.er.soap2restadapterservice.ws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Блок-обертка данных СМЭВ
 * 
 * <p>Java class for MessageDataSendResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MessageDataSendResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="AppData" type="{http://soap2RestAdapterService.er.atc.ru/ws}AppDataSendResponse" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MessageDataSendResponse", propOrder = {
    "appData"
})
public class MessageDataSendResponse {

    @XmlElement(name = "AppData")
    protected AppDataSendResponse appData;

    /**
     * Gets the value of the appData property.
     * 
     * @return
     *     possible object is
     *     {@link AppDataSendResponse }
     *     
     */
    public AppDataSendResponse getAppData() {
        return appData;
    }

    /**
     * Sets the value of the appData property.
     * 
     * @param value
     *     allowed object is
     *     {@link AppDataSendResponse }
     *     
     */
    public void setAppData(AppDataSendResponse value) {
        this.appData = value;
    }

}
