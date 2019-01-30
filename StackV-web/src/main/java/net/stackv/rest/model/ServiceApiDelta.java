/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2015

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

import java.util.Date;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author xyang
 */
@XmlRootElement(name = "serviceDelta")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceApiDelta {

    @XmlElement(required = true)
    protected String workerClassPath = "";
    @XmlElement(required = true)
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
