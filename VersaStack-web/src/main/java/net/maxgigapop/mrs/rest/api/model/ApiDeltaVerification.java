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
@XmlRootElement(name = "deltaVerification")
@XmlAccessorType(XmlAccessType.FIELD)
public class ApiDeltaVerification {
    @XmlElement(required = true)
    protected String referenceUUID = "";
    @XmlElement(required = true)
    protected String creationTime;
    @XmlElement(required = true)
    protected String modelReduction = null;
    @XmlElement(required = true)
    protected String modelAddition = null;
    @XmlElement(required = true)
    protected String verifiedReduction = null;
    @XmlElement(required = true)
    protected String verifiedAddition = null;

    public String getReferenceUUID() {
        return referenceUUID;
    }

    public void setReferenceUUID(String referenceUUID) {
        this.referenceUUID = referenceUUID;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public String getModelReduction() {
        return modelReduction;
    }

    public void setModelReduction(String modelReduction) {
        this.modelReduction = modelReduction;
    }

    public String getModelAddition() {
        return modelAddition;
    }

    public void setModelAddition(String modelAddition) {
        this.modelAddition = modelAddition;
    }

    public String getVerifiedReduction() {
        return verifiedReduction;
    }

    public void setVerifiedReduction(String verifiedReduction) {
        this.verifiedReduction = verifiedReduction;
    }

    public String getVerifiedAddition() {
        return verifiedAddition;
    }

    public void setVerifiedAddition(String verifiedAddition) {
        this.verifiedAddition = verifiedAddition;
    }

    
    @Override
    public String toString() {
        return "net.maxgigapop.mrs.rest.api.model.ApiDeltaBase[ referenceUUID=" + referenceUUID + " ]";
    }
}
