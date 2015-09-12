/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.bean;

import com.hp.hpl.jena.ontology.OntModel;
import java.sql.Date;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import net.maxgigapop.mrs.common.ModelUtil;

/**
 *
 * @author xyang
 */
@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public class DeltaModel extends ModelBase {
    protected boolean isAddition = true; // true = addition; false = reduction
    
    @OneToOne
    @JoinColumn(name = "deltaId")
    protected DeltaBase delta = null;

    public boolean isIsAddition() {
        return isAddition;
    }

    public void setIsAddition(boolean isAddition) {
        this.isAddition = isAddition;
    }

    public DeltaBase getDelta() {
        return delta;
    }

    public void setDelta(DeltaBase delta) {
        this.delta = delta;
    }
    
    public DeltaModel clone() {
        DeltaModel cloned = new DeltaModel();
        cloned.setCommitted(this.committed);
        cloned.setCxtVersion(this.cxtVersion);
        cloned.setCxtVersionTag(this.cxtVersionTag);
        cloned.setTtlModel(this.ttlModel);
        if (this.ontModel != null) {
            cloned.setOntModel(ModelUtil.cloneOntModel(this.ontModel));
        }
        cloned.setPersistent(this.isPersistent());
        cloned.setIsAddition(this.isAddition);
        cloned.setDelta(this.delta);
        return cloned;    
    }
    
    @Override
    public String toString() {
        return "net.maxgigapop.mrs.model.DeltaModel[ id=" + id + " ]";
    }
}
