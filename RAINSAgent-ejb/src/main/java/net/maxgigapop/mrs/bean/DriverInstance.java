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
import javax.persistence.Transient;
import net.maxgigapop.mrs.bean.persist.PersistentEntity;

/**
 *
 * @author xyang
 */
//@TODO: create a POJO to hold static list of DriverInstance. All access to the list has to go though this POJO.
@Entity
@Table(name = "driver_instance")
public class DriverInstance extends PersistentEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    private String topologyUri = "";
    
    private String driverEjbPath = "";

    @Transient
    private VersionItem headVersionItem = null;
    
    // incoming from subsystems
    //@TODO: change to list of driverModels instead
    // - remove the DriverDelta entity
    // - each model has versionId assigned by driverSystem
    // - new and persist viewItem only if a Model is pulled to upper layer
    // - use cached head driverModel to create union instead of create headVG
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

    public String getDriverEjbPath() {
        return driverEjbPath;
    }

    public void setDriverEjbPath(String driverEjbPath) {
        this.driverEjbPath = driverEjbPath;
    }

    //@TODO: used by Union model logic
    public VersionItem getHeadVersionItem() {
        //@TODO: create currentVersionItem from DB if null
        return headVersionItem;
    }

    //@TODO: used by DriverModelPuller to cache VI
    public void setHeadVersionItem(VersionItem currentVersionItem) {
        this.headVersionItem = headVersionItem;
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
