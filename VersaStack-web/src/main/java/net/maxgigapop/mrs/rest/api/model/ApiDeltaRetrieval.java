/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.rest.api.model;

import java.util.Date;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author max
 */
@XmlRootElement(name = "deltaRetrieval")
@XmlAccessorType(XmlAccessType.FIELD)
public class ApiDeltaRetrieval {
    @XmlElement(required = true)
    protected String referenceUUID = "";
    @XmlElement(required = true)
    protected String serviceModelReduction = null;
    @XmlElement(required = true)
    protected String serviceModelAddition = null;
    @XmlElement(required = true)
    protected String systemModelAddition = null;
    @XmlElement(required = true)
    protected String systemModelReduction = null;

    public String getReferenceUUID() {
        return referenceUUID;
    }

    public void setReferenceUUID(String referenceUUID) {
        this.referenceUUID = referenceUUID;
    }

    public String getServiceModelReduction() {
        return serviceModelReduction;
    }

    public void setServiceModelReduction(String serviceModelReduction) {
        this.serviceModelReduction = serviceModelReduction;
    }

    public String getServiceModelAddition() {
        return serviceModelAddition;
    }

    public void setServiceModelAddition(String serviceModelAddition) {
        this.serviceModelAddition = serviceModelAddition;
    }

    public String getSystemModelAddition() {
        return systemModelAddition;
    }

    public void setSystemModelAddition(String systemModelAddition) {
        this.systemModelAddition = systemModelAddition;
    }

    public String getSystemModelReduction() {
        return systemModelReduction;
    }

    public void setSystemModelReduction(String systemModelReduction) {
        this.systemModelReduction = systemModelReduction;
    }

    @Override
    public String toString() {
        return "net.maxgigapop.mrs.rest.api.model.ApiDeltaRetrieval[ referenceUUID=" + referenceUUID + " ]";
    }
}
