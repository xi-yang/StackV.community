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

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

/**
 *
 * @author xyang
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class SystemDelta extends DeltaBase {

    @OneToOne
    @JoinColumn(name = "referenceVersionGroupId")
    protected VersionGroup referenceVersionGroup;

    @OneToMany(mappedBy = "systemDelta", cascade = {CascadeType.ALL})
    protected List<DriverSystemDelta> driverSystemDeltas = null;

    @OneToOne
    @JoinColumn(name = "serviceDeltaIdForSystem")
    protected ServiceDelta serviceDelta = null;

    public List<DriverSystemDelta> getDriverSystemDeltas() {
        return driverSystemDeltas;
    }

    public void setDriverSystemDeltas(List<DriverSystemDelta> driverSystemDeltas) {
        this.driverSystemDeltas = driverSystemDeltas;
    }

    public VersionGroup getReferenceVersionGroup() {
        return referenceVersionGroup;
    }

    public void setReferenceVersionGroup(VersionGroup referenceVersionGroup) {
        this.referenceVersionGroup = referenceVersionGroup;
    }

    public ServiceDelta getServiceDelta() {
        return serviceDelta;
    }

    public void setServiceDelta(ServiceDelta serviceDelta) {
        this.serviceDelta = serviceDelta;
    }

    @Override
    public String toString() {
        return "net.maxgigapop.mrs.model.SystemDelta[ id=" + id + " ]";
    }
}
