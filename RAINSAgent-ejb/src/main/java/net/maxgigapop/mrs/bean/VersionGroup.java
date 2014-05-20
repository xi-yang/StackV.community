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

/**
 *
 * @author xyang
 */
@Entity
public class VersionGroup implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @OneToMany(mappedBy="versionGroup", cascade = {CascadeType.ALL})
    List<VersionItem> versionItems;
    
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
        if (!(object instanceof VersionGroup)) {
            return false;
        }
        VersionGroup other = (VersionGroup) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    public List<VersionItem> getVersionItems() {
        return versionItems;
    }

    public void setVersionItems(List<VersionItem> versionItems) {
        this.versionItems = versionItems;
    }

    @Override
    public String toString() {
        return "net.maxgigapop.mrs.bean.ModelVersionGroup[ id=" + id + " ]";
    }
    
}
