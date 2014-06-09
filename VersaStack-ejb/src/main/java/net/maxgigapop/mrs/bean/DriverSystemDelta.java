/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.bean;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

/**
 *
 * @author xyang
 */

@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public class DriverSystemDelta extends DeltaBase {
    @ManyToOne
    @JoinColumn(name = "systemDeltaId")
    protected SystemDelta systemDelta = null;

    @ManyToOne
    @JoinColumn(name = "driverInstanceId")
    protected DriverInstance driverInstance = null;

    @OneToOne
    @JoinColumn(name = "referenceVersionItemId")
    protected VersionItem referenceVersionItem = null;
    
    @OneToOne
    @JoinColumn(name = "targetVersionItemId")
    protected VersionItem targetVersionItem = null;

    public SystemDelta getSystemDelta() {
        return systemDelta;
    }

    public void setSystemDelta(SystemDelta systemDelta) {
        this.systemDelta = systemDelta;
    }

    public DriverInstance getDriverInstance() {
        return driverInstance;
    }

    public void setDriverInstance(DriverInstance driverInstance) {
        this.driverInstance = driverInstance;
    }

    public VersionItem getReferenceVersionItem() {
        return referenceVersionItem;
    }

    public void setReferenceVersionItem(VersionItem referenceVersionItem) {
        this.referenceVersionItem = referenceVersionItem;
    }

    public VersionItem getTargetVersionItem() {
        return targetVersionItem;
    }

    public void setTargetVersionItem(VersionItem targetVersionItem) {
        this.targetVersionItem = targetVersionItem;
    }

    @Override
    public String toString() {
        return "net.maxgigapop.mrs.model.DriverSystemDelta[ id=" + id + " ]";
    }
    
 }
