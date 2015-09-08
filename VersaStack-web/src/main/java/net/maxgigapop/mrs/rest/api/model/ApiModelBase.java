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
import javax.xml.bind.annotation.XmlSchemaType;

/**
 *
 * @author max
 */
@XmlRootElement(name="model")
@XmlAccessorType(XmlAccessType.FIELD)
public class ApiModelBase {
    
    @XmlElement(required=true) 
    protected Long id = 0L;
    @XmlElement(required=true) 
    protected String creationTime;
    @XmlElement(required=true) 
    protected String version = "";
    @XmlElement(required=true) 
    protected String ttlModel = "";
    @XmlElement(required=true) 
    protected String status = "";

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTtlModel() {
        return ttlModel;
    }

    public void setTtlModel(String ttlModel) {
        this.ttlModel = ttlModel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "net.maxgigapop.versastack.model.ModelBase[ id=" + id + " ]";
    }
}
