
package it.veneto.regione.schemas._2012.pagamenti.ente;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ctEnteBeneficiario complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ctEnteBeneficiario">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="identificativoUnivocoBeneficiario" type="{http://www.regione.veneto.it/schemas/2012/Pagamenti/Ente/}ctIdentificativoUnivocoPersonaG"/>
 *         &lt;element name="denominazioneBeneficiario" type="{http://www.regione.veneto.it/schemas/2012/Pagamenti/Ente/}stText70"/>
 *         &lt;element name="codiceUnitOperBeneficiario" type="{http://www.regione.veneto.it/schemas/2012/Pagamenti/Ente/}stText35" minOccurs="0"/>
 *         &lt;element name="denomUnitOperBeneficiario" type="{http://www.regione.veneto.it/schemas/2012/Pagamenti/Ente/}stText70" minOccurs="0"/>
 *         &lt;element name="indirizzoBeneficiario" type="{http://www.regione.veneto.it/schemas/2012/Pagamenti/Ente/}stText70" minOccurs="0"/>
 *         &lt;element name="civicoBeneficiario" type="{http://www.regione.veneto.it/schemas/2012/Pagamenti/Ente/}stText16" minOccurs="0"/>
 *         &lt;element name="capBeneficiario" type="{http://www.regione.veneto.it/schemas/2012/Pagamenti/Ente/}stText16" minOccurs="0"/>
 *         &lt;element name="localitaBeneficiario" type="{http://www.regione.veneto.it/schemas/2012/Pagamenti/Ente/}stText35" minOccurs="0"/>
 *         &lt;element name="provinciaBeneficiario" type="{http://www.regione.veneto.it/schemas/2012/Pagamenti/Ente/}stText35" minOccurs="0"/>
 *         &lt;element name="nazioneBeneficiario" type="{http://www.regione.veneto.it/schemas/2012/Pagamenti/Ente/}stNazione" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ctEnteBeneficiario", propOrder = {
    "identificativoUnivocoBeneficiario",
    "denominazioneBeneficiario",
    "codiceUnitOperBeneficiario",
    "denomUnitOperBeneficiario",
    "indirizzoBeneficiario",
    "civicoBeneficiario",
    "capBeneficiario",
    "localitaBeneficiario",
    "provinciaBeneficiario",
    "nazioneBeneficiario"
})
public class CtEnteBeneficiario {

    @XmlElement(required = true)
    protected CtIdentificativoUnivocoPersonaG identificativoUnivocoBeneficiario;
    @XmlElement(required = true)
    protected String denominazioneBeneficiario;
    protected String codiceUnitOperBeneficiario;
    protected String denomUnitOperBeneficiario;
    protected String indirizzoBeneficiario;
    protected String civicoBeneficiario;
    protected String capBeneficiario;
    protected String localitaBeneficiario;
    protected String provinciaBeneficiario;
    protected String nazioneBeneficiario;

    /**
     * Gets the value of the identificativoUnivocoBeneficiario property.
     * 
     * @return
     *     possible object is
     *     {@link CtIdentificativoUnivocoPersonaG }
     *     
     */
    public CtIdentificativoUnivocoPersonaG getIdentificativoUnivocoBeneficiario() {
        return identificativoUnivocoBeneficiario;
    }

    /**
     * Sets the value of the identificativoUnivocoBeneficiario property.
     * 
     * @param value
     *     allowed object is
     *     {@link CtIdentificativoUnivocoPersonaG }
     *     
     */
    public void setIdentificativoUnivocoBeneficiario(CtIdentificativoUnivocoPersonaG value) {
        this.identificativoUnivocoBeneficiario = value;
    }

    /**
     * Gets the value of the denominazioneBeneficiario property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDenominazioneBeneficiario() {
        return denominazioneBeneficiario;
    }

    /**
     * Sets the value of the denominazioneBeneficiario property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDenominazioneBeneficiario(String value) {
        this.denominazioneBeneficiario = value;
    }

    /**
     * Gets the value of the codiceUnitOperBeneficiario property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCodiceUnitOperBeneficiario() {
        return codiceUnitOperBeneficiario;
    }

    /**
     * Sets the value of the codiceUnitOperBeneficiario property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCodiceUnitOperBeneficiario(String value) {
        this.codiceUnitOperBeneficiario = value;
    }

    /**
     * Gets the value of the denomUnitOperBeneficiario property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDenomUnitOperBeneficiario() {
        return denomUnitOperBeneficiario;
    }

    /**
     * Sets the value of the denomUnitOperBeneficiario property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDenomUnitOperBeneficiario(String value) {
        this.denomUnitOperBeneficiario = value;
    }

    /**
     * Gets the value of the indirizzoBeneficiario property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIndirizzoBeneficiario() {
        return indirizzoBeneficiario;
    }

    /**
     * Sets the value of the indirizzoBeneficiario property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIndirizzoBeneficiario(String value) {
        this.indirizzoBeneficiario = value;
    }

    /**
     * Gets the value of the civicoBeneficiario property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCivicoBeneficiario() {
        return civicoBeneficiario;
    }

    /**
     * Sets the value of the civicoBeneficiario property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCivicoBeneficiario(String value) {
        this.civicoBeneficiario = value;
    }

    /**
     * Gets the value of the capBeneficiario property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCapBeneficiario() {
        return capBeneficiario;
    }

    /**
     * Sets the value of the capBeneficiario property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCapBeneficiario(String value) {
        this.capBeneficiario = value;
    }

    /**
     * Gets the value of the localitaBeneficiario property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLocalitaBeneficiario() {
        return localitaBeneficiario;
    }

    /**
     * Sets the value of the localitaBeneficiario property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLocalitaBeneficiario(String value) {
        this.localitaBeneficiario = value;
    }

    /**
     * Gets the value of the provinciaBeneficiario property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProvinciaBeneficiario() {
        return provinciaBeneficiario;
    }

    /**
     * Sets the value of the provinciaBeneficiario property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProvinciaBeneficiario(String value) {
        this.provinciaBeneficiario = value;
    }

    /**
     * Gets the value of the nazioneBeneficiario property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNazioneBeneficiario() {
        return nazioneBeneficiario;
    }

    /**
     * Sets the value of the nazioneBeneficiario property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNazioneBeneficiario(String value) {
        this.nazioneBeneficiario = value;
    }

}
