/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.bean;

import java.io.Serializable;
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
