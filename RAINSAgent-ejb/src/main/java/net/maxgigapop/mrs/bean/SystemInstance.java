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

    @OneToMany(mappedBy="systemInstance", cascade = {CascadeType.ALL})
    protected List<SystemDelta> systemDeltas = null;    
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<SystemDelta> getSystemDeltas() {
        return systemDeltas;
    }

    public void setSystemDeltas(List<SystemDelta> systemDeltas) {
        this.systemDeltas = systemDeltas;
    }

    public boolean hasSystemDelta(SystemDelta aDelta) {
        if (systemDeltas == null || systemDeltas.isEmpty())
            return false;
        for (SystemDelta delta: systemDeltas) {
            if (delta.equals(aDelta)) 
                return true;
        }
        return false;
    }
    
    public void addSystemDelta(SystemDelta aDelta) {
        if (systemDeltas == null)
            systemDeltas = new ArrayList<>();
        if (!hasSystemDelta(aDelta)) {
            try {
                ModelPersistenceManager.save(aDelta);
            } catch (Exception e) {
                throw new EJBException(String.format("%s faled to save %s, due to %s", this.toString(), aDelta.toString(), e.getMessage()));
            }
            systemDeltas.add(aDelta);
        }
    }

    public void removeSystemDelta(SystemDelta aDelta) {
        if (systemDeltas != null) {
            systemDeltas.remove(aDelta);
            //$$ catch and throw exception
            ModelPersistenceManager.delete(aDelta);
        }
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
    
}
