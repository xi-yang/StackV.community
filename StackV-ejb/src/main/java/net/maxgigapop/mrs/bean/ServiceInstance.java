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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;
import net.maxgigapop.mrs.bean.persist.PersistentEntity;

/**
 *
 * @author xyang
 */
@Entity
@Table(name = "service_instance")
public class ServiceInstance extends PersistentEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String referenceUUID;

    @OneToMany(mappedBy = "serviceInstance", cascade = {CascadeType.ALL})
    protected List<ServiceDelta> serviceDeltas = null;

    @ElementCollection
    @JoinTable(name = "service_instance_property", joinColumns = @JoinColumn(name = "serviceInstanceId"))
    @MapKeyColumn(name = "property")
    @Lob
    @Column(name = "value")
    private Map<String, String> properties = new HashMap<String, String>();

    String status = "INIT";

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

    public List<ServiceDelta> getServiceDeltas() {
        return serviceDeltas;
    }

    public void setServiceDeltas(List<ServiceDelta> serviceDeltas) {
        this.serviceDeltas = serviceDeltas;
    }

    public void addServiceDeltaWithoutSave(ServiceDelta delta) {
        if (!this.serviceDeltas.isEmpty()) {
            delta.setOrderInt(this.serviceDeltas.get(this.serviceDeltas.size()-1).getOrderInt()+1);
        }
        this.serviceDeltas.add(delta);
    }

    @SuppressWarnings("unused")
    @PostLoad
    public void postLoad() {
        if (serviceDeltas == null || serviceDeltas.size() < 2) {
            return;
        }
        // sort serviceDeltas by ascending order
        Collections.sort(serviceDeltas, new Comparator<DeltaBase>() {
            @Override
            public int compare(DeltaBase delta1, DeltaBase delta2) {
                return delta1.getOrderInt() < delta2.getOrderInt() ? -1 : (delta1.getOrderInt() > delta2.getOrderInt()) ? 1 : 0;
            }
        });
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public String getProperty(String key) {
        if (!this.properties.containsKey(key)) {
            return null;
        }
        return this.properties.get(key);
    }

    public void putProperty(String key, String value) {
        this.properties.put(key, value);
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
        if (!(object instanceof ServiceInstance)) {
            return false;
        }
        ServiceInstance other = (ServiceInstance) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "net.maxgigapop.mrs.model.ServiceInstance[ id=" + id + " ]";
    }

}
