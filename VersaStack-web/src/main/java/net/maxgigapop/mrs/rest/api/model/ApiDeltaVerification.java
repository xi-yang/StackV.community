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
    protected String verifiedModelReduction = null;
    @XmlElement(required = true)
    protected String unverifiedModelReduction = null;
    @XmlElement(required = true)
    protected String reductionVerified = null;
    @XmlElement(required = true)
    protected String verifiedModelAddition = null;
    @XmlElement(required = true)
    protected String unverifiedModelAddition = null;
    @XmlElement(required = true)
    protected String additionVerified = null;

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

    public String getVerifiedModelReduction() {
        return verifiedModelReduction;
    }

    public void setVerifiedModelReduction(String verifiedModelReduction) {
        this.verifiedModelReduction = verifiedModelReduction;
    }

    public String getUnverifiedModelReduction() {
        return unverifiedModelReduction;
    }

    public void setUnverifiedModelReduction(String unverifiedModelReduction) {
        this.unverifiedModelReduction = unverifiedModelReduction;
    }

    public String getVerifiedModelAddition() {
        return verifiedModelAddition;
    }

    public void setVerifiedModelAddition(String verifiedModelAddition) {
        this.verifiedModelAddition = verifiedModelAddition;
    }

    public String getUnverifiedModelAddition() {
        return unverifiedModelAddition;
    }

    public void setUnverifiedModelAddition(String unverifiedModelAddition) {
        this.unverifiedModelAddition = unverifiedModelAddition;
    }

    public String getReductionVerified() {
        return reductionVerified;
    }

    public void setReductionVerified(String reductionVerified) {
        this.reductionVerified = reductionVerified;
    }

    public String getAdditionVerified() {
        return additionVerified;
    }

    public void setAdditionVerified(String additionVerified) {
        this.additionVerified = additionVerified;
    }

    @Override
    public String toString() {
        return "net.maxgigapop.mrs.rest.api.model.ApiDeltaVerification[ referenceUUID=" + referenceUUID + " ]";
    }
}
