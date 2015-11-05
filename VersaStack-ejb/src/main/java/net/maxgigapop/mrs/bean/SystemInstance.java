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
import java.util.concurrent.Future;
import javax.ejb.EJBException;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import net.maxgigapop.mrs.bean.persist.*;

/**
 *
 * @author xyang
 */
@Entity
@Table(name = "system_instance")
public class SystemInstance extends PersistentEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String referenceUUID;
            
    @OneToOne(cascade = {CascadeType.ALL})
    @JoinColumn(name = "systemDeltaId")
    protected SystemDelta systemDelta = null;    

    @Transient 
    Future<String> commitStatus = null;
    
    @Transient
    private boolean commitFlag = false;
    
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

    public SystemDelta getSystemDelta() {
        return systemDelta;
    }

    public void setSystemDelta(SystemDelta systemDelta) {
        this.systemDelta = systemDelta;
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
        if (!(object instanceof SystemInstance)) {
            return false;
        }
        SystemInstance other = (SystemInstance) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "net.maxgigapop.mrs.model.SystemInstance[ id=" + id + " ]";
    }

    public Future<String> getCommitStatus() {
        return commitStatus;
    }

    public void setCommitStatus(Future<String> commitStatus) {
        this.commitStatus = commitStatus;
    }
    
    public boolean getCommitFlag(){
        return commitFlag;
    }
    
    public void setCommitFlag(boolean commitFlag){
        this.commitFlag = commitFlag;
    }
}