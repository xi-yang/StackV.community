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
@XmlRootElement(name = "net-create")
@XmlAccessorType(XmlAccessType.FIELD)
public class ApiNetCreation {

    @XmlElement(required = true)
    protected Map<String, String> properties = new HashMap<>();

    public Map<String, String> getProperties() {
        return properties;
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
