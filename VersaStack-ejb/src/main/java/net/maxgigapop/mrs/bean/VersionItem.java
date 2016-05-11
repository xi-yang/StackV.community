/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import net.maxgigapop.mrs.bean.persist.PersistentEntity;

/**
 *
 * @author xyang
 */
@Entity
@Table(name = "version_item")
public class VersionItem extends PersistentEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    // reference ID for the callee
    private String referenceUUID = null;

    @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinTable(
            name = "version_group_item",
            joinColumns = {
                @JoinColumn(name = "item_id", referencedColumnName = "id")},
            inverseJoinColumns = {
                @JoinColumn(name = "group_id", referencedColumnName = "id")})
    private List<VersionGroup> versionGroups = null;

    @OneToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "modelRefId")
    private ModelBase modelRef = null;

    @OneToOne
    @JoinColumn(name = "driverInstanceId")
    private DriverInstance driverInstance = null;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReferenceUUID() {
        return referenceUUID;
    }

    public void setReferenceUUID(String referenceUUID) {
        this.referenceUUID = referenceUUID;
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
        if ((this.referenceUUID != null && other.referenceUUID != null)
                && (this.referenceUUID.equals(other.referenceUUID))
                && (this.id == other.id)) {
            return true;
        }
        return false;
    }

    public List<VersionGroup> getVersionGroups() {
        return versionGroups;
    }

    public void setVersionGroups(List<VersionGroup> versionGroups) {
        this.versionGroups = versionGroups;
    }

    public void addVersionGroup(VersionGroup versionGroup) {
        if (this.versionGroups == null) {
            this.versionGroups = new ArrayList<VersionGroup>();
        }
        this.versionGroups.add(versionGroup);
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
        return String.format("net.maxgigapop.mrs.bean.VersionItem[ id=%d uuid=%s ]", id, referenceUUID);
    }

}
