/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.rest.api.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author max
 */

@XmlRootElement(name="driverInstance")
@XmlAccessorType(XmlAccessType.FIELD)
public class ApiDriverInstance {
    @XmlElement(required=true)
    private String topologyUri = "";
    @XmlElement(required=true)
    private String driverEjbPath = "";
    
    public String getTopologyUri() {
        return topologyUri;
    }

    public void setTopologyUri(String topologyUri) {
        this.topologyUri = topologyUri;
    }

    public String getDriverEjbPath() {
        return driverEjbPath;
    }

    public void setDriverEjbPath(String driverEjbPath) {
        this.driverEjbPath = driverEjbPath;
    }
    @Override
    public String toString() {
        return "net.maxgigapop.mrs.model.DriverInstance[ topologyUri=" + topologyUri + " ]";
    }


}
