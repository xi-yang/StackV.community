/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.rest.api.model;

import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author max
 */
@XmlRootElement(name = "vm-install")
@XmlAccessorType(XmlAccessType.FIELD)
public class ApiVMInstall {

    @XmlElement(required = true)
    protected Map<String, String> properties = new HashMap<>();
    protected Map<String, String> subnets = new HashMap<>();
    protected Map<String, String> volumes = new HashMap<>();

    public Map<String, String> getProperties() {
        return properties;
    }

    public Map<String, String> getSubnets() {
        return subnets;
    }

    public Map<String, String> getVolumes() {
        return volumes;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public String getTopologyUri() throws Exception {
        if (!properties.containsKey("topologyUri")) {
            throw new Exception("Can't find topologyUri");
        } else {
            return properties.get("topologyUri");
        }
    }
//    @Override
//    public String toString() {
//        return "net.maxgigapop.mrs.model.DriverInstance[ topologyUri=" + topologyUri + " ]";
//    }

}
