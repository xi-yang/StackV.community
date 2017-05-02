/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2013

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

package net.maxgigapop.mrs.bean;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

/**
 *
 * @author xyang
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class ServiceDelta extends DeltaBase {

    @ManyToOne
    @JoinColumn(name = "serviceInstanceId")
    protected ServiceInstance serviceInstance = null;

    private String referenceUUID;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "systemDeltaIdForService")
    protected SystemDelta systemDelta = null;

    private String status = "INIT";

    public String getReferenceUUID() {
        return referenceUUID;
    }

    //@TODO Add status variable to track service delta processing
    public void setReferenceUUID(String referenceUUID) {
        this.referenceUUID = referenceUUID;
    }

    public ServiceInstance getServiceInstance() {
        return serviceInstance;
    }

    public void setServiceInstance(ServiceInstance serviceInstance) {
        this.serviceInstance = serviceInstance;
    }

    public SystemDelta getSystemDelta() {
        return systemDelta;
    }

    public void setSystemDelta(SystemDelta systemDelta) {
        this.systemDelta = systemDelta;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ServiceDelta clone() {
        ServiceDelta cloned = new ServiceDelta();
        if (this.modelAddition != null) {
            cloned.modelAddition = this.modelAddition.clone();
        }
        if (this.modelReduction != null) {
            cloned.modelReduction = this.modelReduction.clone();
        }
        //just copy reference
        if (this.systemDelta != null) {
            cloned.systemDelta = this.systemDelta;
        }
        cloned.referenceUUID = this.referenceUUID;
        return cloned;
    }

    @Override
    public String toString() {
        return "net.maxgigapop.mrs.model.ServiceDelta[ id=" + id + " ]";
    }
}
