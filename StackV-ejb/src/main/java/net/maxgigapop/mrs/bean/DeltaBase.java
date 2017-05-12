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

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
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
import org.hibernate.annotations.GenericGenerator;

/**
 *
 * @author xyang
 */
@Entity
@Table(name = "delta")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class DeltaBase extends PersistentEntity implements Serializable {
    protected static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    protected String id;

    @Column(name = "index_column", nullable = false)
    Integer index = 0;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "modelAdditionId")
    protected DeltaModel modelAddition = null;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "modelReductionId")
    protected DeltaModel modelReduction = null;
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public DeltaBase clone() {
        DeltaBase cloned = new DeltaBase();
        if (this.modelAddition != null) {
            cloned.modelAddition = this.modelAddition.clone();
        }
        if (this.modelReduction != null) {
            cloned.modelReduction = this.modelReduction.clone();
        }
        return cloned;
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
}
