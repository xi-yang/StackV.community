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
@XmlRootElement(name = "delta")
@XmlAccessorType(XmlAccessType.FIELD)
public class ApiDeltaBase {

    @XmlElement(required = true)
    protected String id = "";
    @XmlElement(required = true)
    protected String creationTime;
    @XmlElement(required = true)
    protected String referenceVersion = "";
    @XmlElement(required = true)
    protected String modelReduction = null;
    @XmlElement(required = true)
    protected String modelAddition = null;
//    @XmlElement(required=true) 
//    protected String status = "";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public String getModelAddition() {
        return modelAddition;
    }

    public void setModelAddition(String modelAddition) {
        this.modelAddition = modelAddition;
    }

    public String getModelReduction() {
        return modelReduction;
    }

    public void setModelReduction(String modelReduction) {
        this.modelReduction = modelReduction;
    }

    public String getReferenceVersion() {
        return referenceVersion;
    }

    public void setReferenceVersion(String referenceVersion) {
        this.referenceVersion = referenceVersion;
    }

//    public String getStatus() {
//        return status;
//    }
//
//    public void setStatus(String status) {
//        this.status = status;
//    }
    @Override
    public String toString() {
        return "net.maxgigapop.versans.model.DeltaBase[ id=" + id + " ]";
    }
}
