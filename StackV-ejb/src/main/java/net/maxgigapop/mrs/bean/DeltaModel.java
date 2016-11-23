/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2013

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
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
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
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
