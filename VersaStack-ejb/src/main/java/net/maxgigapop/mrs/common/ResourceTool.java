/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.common;

/**
 *
 * @author muzcategui
 */
public class ResourceTool {

    public static String getResourceUri(String topologyUri, String name) {
        String uri;
        if (name.startsWith("urn:ogf:network:")) {
            return name;
        } else {
            return topologyUri + name;
        }
    }

    public static String getResourceName(String topologyUri, String name) {
        //remove the topologyUri first
        if (name != null) {
            if (name.startsWith(topologyUri)) {
                name = name.replace(topologyUri, "");
            } else if (name.startsWith("urn:ogf:network:")) {//remove the urn:ogf:network: if it is left
                name = name.replace("urn:ogf:network:", "");
            }
        }
        return name;
    }

}
