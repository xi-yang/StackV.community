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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJBException;
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
import net.maxgigapop.mrs.bean.persist.ModelPersistenceManager;
import net.maxgigapop.mrs.bean.persist.PersistentEntity;
import net.maxgigapop.mrs.bean.persist.VersionItemPersistenceManager;

/**
 *
 * @author xyang
 */
//@TODO: create a POJO to hold static list of DriverInstance. All access to the list has to go though this POJO.
@Entity
@Table(name = "driver_instance")
public class DriverInstance extends PersistentEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id = 0L;

    private String topologyUri = "";

    private String driverEjbPath = "";

    @Transient
    private VersionItem headVersionItem = null;

    // incoming from subsystems
    @OneToMany(mappedBy = "driverInstance", cascade = {CascadeType.ALL})
    private List<DriverModel> driverModels;

    // outgoing to subsystems
    @OneToMany(mappedBy = "driverInstance", cascade = {CascadeType.ALL})
    private List<DriverSystemDelta> driverSystemDeltas;

    @ElementCollection
    @JoinTable(name = "driver_instance_property", joinColumns = @JoinColumn(name = "driverInstanceId"))
    @MapKeyColumn(name = "property")
    @Lob
    @Column(name = "value")
    private Map<String, String> properties = new HashMap<String, String>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTopologyUri() {
        return topologyUri;
    }

    public void setTopologyUri(String topologyUri) {
        this.topologyUri = topologyUri;
    }

    public List<DriverModel> getDriverModels() {
        return driverModels;
    }

    public void setDriverModels(List<DriverModel> driverModels) {
        this.driverModels = driverModels;
    }

    public List<DriverSystemDelta> getDriverSystemDeltas() {
        return driverSystemDeltas;
    }

    public void setDriverSystemDeltas(List<DriverSystemDelta> driverSystemDeltas) {
        this.driverSystemDeltas = driverSystemDeltas;
    }

    public boolean hasDriverSystemDelta(DriverSystemDelta aDelta) {
        if (driverSystemDeltas == null || driverSystemDeltas.isEmpty()) {
            return false;
        }
        for (DriverSystemDelta delta : driverSystemDeltas) {
            if (delta.equals(aDelta)) {
                return true;
            }
        }
        return false;
    }

    public void addDriverSystemDelta(DriverSystemDelta aDelta) {
        if (driverSystemDeltas == null) {
            driverSystemDeltas = new ArrayList<>();
        }
        if (!hasDriverSystemDelta(aDelta)) {
            if (!this.driverSystemDeltas.isEmpty()) {
                aDelta.setOrderInt(this.driverSystemDeltas.get(this.driverSystemDeltas.size() - 1).getOrderInt() + 1);
            }
            try {
                ModelPersistenceManager.save(aDelta);
            } catch (Exception e) {
                throw new EJBException(String.format("%s faled to save %s, due to %s", this.toString(), aDelta.toString(), e.getMessage()));
            }
            driverSystemDeltas.add(aDelta);
        }
    }


    @SuppressWarnings("unused")
    @PostLoad
    public void postLoad() {
        if (driverSystemDeltas == null || driverSystemDeltas.size() < 2) {
            return;
        }
        // sort driverSystemDeltas by ascending order
        Collections.sort(driverSystemDeltas, new Comparator<DeltaBase>() {
            @Override
            public int compare(DeltaBase delta1, DeltaBase delta2) {
                // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                return delta1.getOrderInt() < delta2.getOrderInt() ? -1 : (delta1.getOrderInt() > delta2.getOrderInt()) ? 1 : 0;
            }
        });
    }

    public String getDriverEjbPath() {
        return driverEjbPath;
    }

    public void setDriverEjbPath(String driverEjbPath) {
        this.driverEjbPath = driverEjbPath;
    }

    public VersionItem getHeadVersionItem() {
        if (this.id == 0L) {
            throw new EJBException(String.format("call getHeadVersionItem from unpersisted %s", this));
        }
        this.headVersionItem = VersionItemPersistenceManager.getHeadByDriverInstance(this);
        return headVersionItem;
    }

    public void setHeadVersionItem(VersionItem currentVersionItem) {
        this.headVersionItem = headVersionItem;
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
        if (!(object instanceof DriverInstance)) {
            return false;
        }
        DriverInstance other = (DriverInstance) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "net.maxgigapop.mrs.model.DriverInstance[ " + topologyUri + " ]";
    }
}
