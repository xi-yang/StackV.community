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
import javax.persistence.OneToOne;

/**
 *
 * @author xyang
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class ServiceDelta extends DeltaBase {
    @OneToOne(mappedBy = "serviceDelta", cascade = {CascadeType.ALL})
    protected ServiceModel serviceModel = null;

    public ServiceModel getServiceModel() {
        return serviceModel;
    }

    public void setServiceModel(ServiceModel serviceModel) {
        this.serviceModel = serviceModel;
    }

    @Override
    public String toString() {
        return "net.maxgigapop.mrs.model.ServiceDelta[ id=" + id + " ]";
    }
}
