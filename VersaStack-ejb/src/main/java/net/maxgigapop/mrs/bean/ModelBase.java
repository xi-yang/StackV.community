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
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Date;
import java.util.UUID;
import javax.ejb.EJBException;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import net.maxgigapop.mrs.bean.persist.PersistentEntity;
import net.maxgigapop.mrs.common.ModelUtil;
import org.hibernate.annotations.GenericGenerator;

/**
 *
 * @author xyang
 */
@Entity
@Table(name = "model")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class ModelBase extends PersistentEntity implements Serializable {
    protected static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    protected String id;

    
    protected Date creationTime;
    protected Long cxtVersion = 0L;
    protected String cxtVersionTag = "";
    protected boolean committed = false;
    
    @Lob
    protected String ttlModel = "";
    
    @Transient
    protected OntModel ontModel = null;

    public ModelBase() {
        this.creationTime = new java.util.Date();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Date creationTime) {
        this.creationTime = creationTime;
    }

    public String getCxtVersionTag() {
        return cxtVersionTag;
    }

    public Long getCxtVersion() {
        return cxtVersion;
    }

    public void setCxtVersion(Long cxtVersion) {
        this.cxtVersion = cxtVersion;
    }

    public void setCxtVersionTag(String cxtVersionTag) {
        this.cxtVersionTag = cxtVersionTag;
    }

    public boolean isCommitted() {
        return committed;
    }

    public void setCommitted(boolean committed) {
        this.committed = committed;
    }

    public String getTtlModel() {
        return ttlModel;
    }

    public void setTtlModel(String ttlModel) {
        this.ttlModel = ttlModel;
    }

    public OntModel getOntModel() {
        return ontModel;
    }

    public void setOntModel(OntModel ontModel) {
        this.ontModel = ontModel;
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
        if (!(object instanceof ModelBase)) {
            return false;
        }
        ModelBase other = (ModelBase) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        try {
            if (ontModel != null && (ttlModel == null || ttlModel.isEmpty())) {
                ttlModel = ModelUtil.marshalOntModel(ontModel);
            }
        } catch (Exception e) {
            ;
        }
        return "net.maxgigapop.mrs.model.MrsModel[ id=" + id + " ]\n Model = " + ttlModel;
    }

    public ModelBase clone() {
        ModelBase cloned = new ModelBase();
        cloned.setCommitted(this.committed);
        cloned.setCxtVersion(this.cxtVersion);
        cloned.setCxtVersionTag(this.cxtVersionTag);
        cloned.setTtlModel(this.ttlModel);
        if (this.ontModel != null) {
            cloned.setOntModel(ModelUtil.cloneOntModel(this.ontModel));
        }
        cloned.setPersistent(this.isPersistent());
        return cloned;
    }

    @SuppressWarnings("unused")
    @PostLoad
    public void postLoad() {
        if (ttlModel.isEmpty()) {
            ontModel = null;
        } else {
            try {
                ontModel = ModelUtil.unmarshalOntModel(ttlModel);
            } catch (Exception e) {
                // logging
                ontModel = null;
            }
        }
    }

    @SuppressWarnings("unused")
    @PrePersist
    @PreUpdate
    public void saveOrUpdate() {
        if (ontModel != null) {
            try {
                ttlModel = ModelUtil.marshalOntModel(ontModel);
            } catch (Exception e) {
                // logging
            }
        }
    }

    public OntModel applyDelta(DeltaBase delta) {
        if (this.ontModel == null) {
            throw new EJBException("applyDelta encounters null this.ontModel");
        }
        if (delta == null || (delta.getModelReduction() == null && delta.getModelAddition() == null)) {
            throw new EJBException("applyDelta encounters null/empty delta");
        }
        if (delta.getModelReduction() != null && delta.getModelReduction().getOntModel() != null) {
            this.ontModel.remove(delta.getModelReduction().getOntModel());
        }
        if (delta.getModelAddition() != null && delta.getModelAddition().getOntModel() != null) {
            this.ontModel.add(delta.getModelAddition().getOntModel());
        }
        return this.ontModel;
    }

    public OntModel dryrunDelta(DeltaBase delta) {
        if (this.ontModel == null) {
            throw new EJBException("dryrunDelta encounters null this.ontModel");
        }
        if (delta == null || (delta.getModelReduction() == null && delta.getModelAddition() == null)) {
            throw new EJBException("dryrunDelta encounters null/empty delta");
        }
        OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        om.add(this.ontModel);
        if (delta.getModelReduction() != null && delta.getModelReduction().getOntModel() != null) {
            om.remove(delta.getModelReduction().getOntModel());
        }
        if (delta.getModelAddition() != null && delta.getModelAddition().getOntModel() != null) {
            om.add(delta.getModelAddition().getOntModel());
        }
        return om;
    }

    //calculate the delta that makes the otherOntModel become this.ontModel
    public DeltaBase diffFromModel(OntModel otherOntModel) {
        DeltaBase delta = new DeltaBase();
        OntModel modelA = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        modelA.add(this.ontModel);
        modelA = (OntModel) modelA.remove(otherOntModel);
        OntModel modelR = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        modelR.add(otherOntModel);
        modelR = (OntModel) modelR.remove(this.ontModel);
        DeltaModel deltaModelA = new DeltaModel();
        deltaModelA.setOntModel(modelA);
        DeltaModel deltaModelR = new DeltaModel();
        deltaModelR.setOntModel(modelR);
        delta.setModelAddition(deltaModelA);
        delta.setModelReduction(deltaModelR);
        deltaModelA.setIsAddition(true);
        deltaModelA.setDelta(delta);
        deltaModelR.setIsAddition(false);
        deltaModelR.setDelta(delta);
        return delta;
    }

    //calculate the delta that makes this.ontModel becomes the otherOntModel 
    public DeltaBase diffToModel(OntModel otherOntModel) {
        DeltaBase delta = new DeltaBase();
        OntModel modelR = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        modelR.add(this.ontModel);
        modelR = (OntModel) modelR.remove(otherOntModel);
        OntModel modelA = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        modelA.add(otherOntModel);
        modelA = (OntModel) modelA.remove(this.ontModel);
        DeltaModel deltaModelA = new DeltaModel();
        deltaModelA.setOntModel(modelA);
        DeltaModel deltaModelR = new DeltaModel();
        deltaModelR.setOntModel(modelR);
        delta.setModelAddition(deltaModelA);
        delta.setModelReduction(deltaModelR);
        deltaModelA.setIsAddition(true);
        deltaModelA.setDelta(delta);
        deltaModelR.setIsAddition(false);
        deltaModelR.setDelta(delta);
        return delta;
    }
}
