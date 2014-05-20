/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.bean;

import java.io.Serializable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 *
 * @author xyang
 */
@Entity
@Table(name = "driver_instance")
public class DriverInstance implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    private String topologyUri;

    // incoming from subsystems
    @OneToMany(mappedBy="driverInstance", cascade = {CascadeType.ALL})
    private List<DriverDelta> driverDeltas;    
    
    // outgoing to subsystems
    @OneToMany(mappedBy="driverInstance", cascade = {CascadeType.ALL})
    private List<DriverSystemDelta> driverSystemDeltas;    

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTopologyUri() {
        return topologyUri;
    }

    public void setTopologyUri(String topologyUri) {
        this.topologyUri = topologyUri;
    }

    public List<DriverDelta> getDriverDeltas() {
        return driverDeltas;
    }

    public void setDriverDeltas(List<DriverDelta> driverDeltas) {
        this.driverDeltas = driverDeltas;
    }

    public List<DriverSystemDelta> getDriverSystemDeltas() {
        return driverSystemDeltas;
    }

    public void setDriverSystemDeltas(List<DriverSystemDelta> driverSystemDeltas) {
        this.driverSystemDeltas = driverSystemDeltas;
    }


    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DriverInstance)) {
            return false;
        }
        DriverInstance other = (DriverInstance) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "net.maxgigapop.mrs.model.DriverInstance[ id=" + id + " ]";
    }
}
