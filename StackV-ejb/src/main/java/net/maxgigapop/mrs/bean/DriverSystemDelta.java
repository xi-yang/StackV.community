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

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

/**
 *
 * @author xyang
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class DriverSystemDelta extends DeltaBase {

    @ManyToOne
    @JoinColumn(name = "systemDeltaId")
    protected SystemDelta systemDelta = null;

    @ManyToOne
    @JoinColumn(name = "driverInstanceId")
    protected DriverInstance driverInstance = null;

    @OneToOne
    @JoinColumn(name = "referenceVersionItemId")
    protected VersionItem referenceVersionItem = null;

    private String referenceUUID = null;

    private String status = null;
    
    public SystemDelta getSystemDelta() {
        return systemDelta;
    }

    public void setSystemDelta(SystemDelta systemDelta) {
        this.systemDelta = systemDelta;
    }

    public DriverInstance getDriverInstance() {
        return driverInstance;
    }

    public void setDriverInstance(DriverInstance driverInstance) {
        this.driverInstance = driverInstance;
    }

    public VersionItem getReferenceVersionItem() {
        return referenceVersionItem;
    }

    public void setReferenceVersionItem(VersionItem referenceVersionItem) {
        this.referenceVersionItem = referenceVersionItem;
    }

    public String getReferenceUUID() {
        return referenceUUID;
    }

    public void setReferenceUUID(String referenceUUID) {
        this.referenceUUID = referenceUUID;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "net.maxgigapop.mrs.model.DriverSystemDelta[ id=" + id + " ]";
    }

}
