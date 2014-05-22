/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.bean;

import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJBException;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import net.maxgigapop.mrs.bean.persist.PersistentEntity;

/**
 *
 * @author xyang
 */
@Entity
public class VersionGroup extends PersistentEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    //reference ID for the caller
    private Long referenceId = 0L;
    
    @ManyToMany
    @JoinTable(
            name = "version_group_item",
            joinColumns = {
                @JoinColumn(name = "group_id", referencedColumnName = "id")},
            inverseJoinColumns = {
                @JoinColumn(name = "item_id", referencedColumnName = "id")})
    private List<VersionItem> versionItems = null;

    private String status = "";
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
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
        if ((this.id != null && other.id != null) || (this.id.equals(other.id))) {
            return true;
        } else if (this.getVersionItems() == null || other.getVersionItems() == null) {
            return false;
        } else if (this.getVersionItems().size() != other.getVersionItems().size()) {
            return false;
        }
        for (int i = 0; i < this.getVersionItems().size(); i++) {
            if (!this.getVersionItems().get(i).equals(other.getVersionItems().get(i))) {
                return false;
            }
        }
        return true;
    }

    public List<VersionItem> getVersionItems() {
        return versionItems;
    }

    public void setVersionItems(List<VersionItem> versionItems) {
        this.versionItems = versionItems;
    }

    public void addVersionItem(VersionItem versionItem) {
        if (this.versionItems == null) {
            this.versionItems = new ArrayList<VersionItem>();
        }
        this.versionItems.add(versionItem);
    }
    
    public VersionItem getVersionItemByDriverInstance(DriverInstance di) {
        for (VersionItem vi: this.getVersionItems()) {
            if (vi.getDriverInstance().equals(di)) {
                return vi;
            }
        }
        return null;
    }
    
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "net.maxgigapop.mrs.bean.VersionGroup[ id=" + id + " ]";
    }

    public ModelBase createUnionModel() {
        ModelBase newModel = new ModelBase();
        newModel.setOntModel(ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF));
        if (this.getVersionItems() == null || this.getVersionItems().isEmpty()) {
            throw new EJBException(String.format("%s is empty when calling method createUnionModel", this));
        }
        for (VersionItem vi: this.getVersionItems()) {
            if (vi.getModelRef() == null || vi.getModelRef().getOntModel() == null) {
                throw new EJBException(String.format("%s method createUnionModel encounters empty %s", this, vi));
            }
            newModel.getOntModel().addSubModel(vi.getModelRef().getOntModel());
        }
        //?? rebind / rerun inference for referenceModel
        return newModel;
    }
}
