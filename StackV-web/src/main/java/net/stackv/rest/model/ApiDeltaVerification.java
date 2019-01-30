/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2016

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 */

package net.stackv.rest.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
        return "net.stackv.rest.model.ApiDeltaVerification[ referenceUUID=" + referenceUUID + " ]";
    }
}
