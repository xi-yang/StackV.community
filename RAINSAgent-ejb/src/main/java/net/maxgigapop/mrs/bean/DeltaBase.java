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
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import net.maxgigapop.mrs.bean.persist.PersistentEntity;

/**
 *
 * @author xyang
 */
@Entity
@Table(name = "delta")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public class DeltaBase extends PersistentEntity implements Serializable {
    protected static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected Long id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "modelAdditionId")
    protected DeltaModel modelAddition = null;
    
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "modelReductionId")
    protected DeltaModel modelReduction = null;

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

    public DeltaModel getModelAddition() {
        return modelAddition;
    }

    public void setModelAddition(DeltaModel modelAddition) {
        this.modelAddition = modelAddition;
    }

    public DeltaModel getModelReduction() {
        return modelReduction;
    }

    public void setModelReduction(DeltaModel modelReduction) {
        this.modelReduction = modelReduction;
    }


    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DeltaBase)) {
            return false;
        }
        DeltaBase other = (DeltaBase) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "net.maxgigapop.mrs.model.DeltaBase[ id=" + id + " ]";
    }
    
    //$$
    public void saveAll() {
        
    }
}
