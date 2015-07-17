/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
    @JoinColumn(name = "systemDeltaId")
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
        if (this.modelAddition != null)
            cloned.modelAddition = this.modelAddition.clone();
        if (this.modelReduction != null)
            cloned.modelReduction = this.modelReduction.clone();
        //just copy reference
        if (this.systemDelta != null)
            cloned.systemDelta = this.systemDelta;
        return cloned;
    }
    
    @Override
    public String toString() {
        return "net.maxgigapop.mrs.model.ServiceDelta[ id=" + id + " ]";
    }
}
