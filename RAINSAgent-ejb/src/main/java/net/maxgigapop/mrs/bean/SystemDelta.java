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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

/**
 *
 * @author xyang
 */

@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public class SystemDelta extends DeltaBase {
    @ManyToOne
    @JoinColumn(name = "systemInstanceId")
    protected SystemInstance systemInstance;

    @OneToMany(mappedBy="systemDelta", cascade = {CascadeType.ALL})
    protected List<DriverSystemDelta> driverSystemDeltas;    
        
    public SystemInstance getSystemInstance() {
        return systemInstance;
    }

    public void setSystemInstance(SystemInstance systemInstance) {
        this.systemInstance = systemInstance;
    }

    public List<DriverSystemDelta> getDriverSystemDeltas() {
        return driverSystemDeltas;
    }

    public void setDriverSystemDeltas(List<DriverSystemDelta> driverSystemDeltas) {
        this.driverSystemDeltas = driverSystemDeltas;
    }

    @Override
    public String toString() {
        return "net.maxgigapop.mrs.model.SystemDelta[ id=" + id + " ]";
    }
    
    //--> save this and driverSystemDeltas
    public void saveAll () {
        
    }
 }
