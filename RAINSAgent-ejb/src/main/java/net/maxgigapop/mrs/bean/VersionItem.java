/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.bean;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import net.maxgigapop.mrs.bean.persist.PersistentEntity;

/**
 *
 * @author xyang
 */
@Entity
public class VersionItem extends PersistentEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "versionGroupId")
    private VersionGroup versionGroup;
    
    @OneToOne
    @JoinColumn(name = "modelRefId")
    private ModelBase modelRef;
    
    @OneToOne
    @JoinColumn(name = "driverInstanceId")
    private DriverInstance driverInstance;
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
        if (!(object instanceof VersionItem)) {
            return false;
        }
        VersionItem other = (VersionItem) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    public VersionGroup getVersionGroup() {
        return versionGroup;
    }

    public void setVersionGroup(VersionGroup versionGroup) {
        this.versionGroup = versionGroup;
    }

    public ModelBase getModelRef() {
        return modelRef;
    }

    public void setModelRef(ModelBase modelRef) {
        this.modelRef = modelRef;
    }

    public DriverInstance getDriverInstance() {
        return driverInstance;
    }

    public void setDriverInstance(DriverInstance driverInstance) {
        this.driverInstance = driverInstance;
    }

    @Override
    public String toString() {
        return "net.maxgigapop.mrs.bean.VersionItem[ id=" + id + " ]";
    }
    
}
