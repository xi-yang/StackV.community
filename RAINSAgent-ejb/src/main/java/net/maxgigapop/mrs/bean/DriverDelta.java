/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.bean;

import java.util.List;
import javax.persistence.CascadeType;
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
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class DriverDelta extends DeltaBase {
    @ManyToOne
    @JoinColumn(name = "driverInstanceId")
    protected DriverInstance driverInstance = null;

    @OneToOne(mappedBy = "driverDelta", cascade = {CascadeType.ALL})
    protected DriverModel driverModel = null;

    public DriverInstance getDriverInstance() {
        return driverInstance;
    }

    public void setDriverInstance(DriverInstance driverInstance) {
        this.driverInstance = driverInstance;
    }
    
    public DriverModel getDriverModel() {
        return driverModel;
    }

    public void setDriverModel(DriverModel driverModel) {
        this.driverModel = driverModel;
    }

    @Override
    public String toString() {
        return "net.maxgigapop.mrs.model.DriverDelta[ id=" + id + " ]";
    }
}
