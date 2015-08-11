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
 * @author xyang
 */
@XmlRootElement(name="serviceDelta")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceApiDelta {
    @XmlElement(required=true) 
    protected String workerClassPath = "";
    @XmlElement(required=true)
    protected String uuid = "";
    @XmlElement
    protected Date creationTime = null;
    @XmlElement
    protected String modelReduction = null;
    @XmlElement
    protected String modelAddition = null;
    
    public String getWorkerClassPath() {
        return workerClassPath;
    }

    public void setWorkerClassPath(String workerClassPath) {
        this.workerClassPath = workerClassPath;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
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
    
}
