
package ru.atc.er.soap2restadapterservice.ws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Содержит сообщение об ошибке
 *                     
 * 
 * <p>Java class for AppDataBadResponse complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AppDataBadResponse">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Error" type="{http://soap2RestAdapterService.er.atc.ru/ws}Error"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AppDataBadResponse", propOrder = {
    "error"
})
public class AppDataBadResponse {

    @XmlElement(name = "Error", required = true)
    protected Error error;

    /**
     * Gets the value of the error property.
     * 
     * @return
     *     possible object is
     *     {@link Error }
     *     
     */
    public Error getError() {
        return error;
    }

    /**
     * Sets the value of the error property.
     * 
     * @param value
     *     allowed object is
     *     {@link Error }
     *     
     */
    public void setError(Error value) {
        this.error = value;
    }

}
