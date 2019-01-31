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

package net.maxgigapop.mrs.rest.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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
