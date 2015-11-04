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
    
    private static final String versaStackPrefix = "urn:ogf:network:";

    public static String getResourceUri(String topologyUri, String name) {
        String uri;
        if (name.startsWith(versaStackPrefix)) {
            return name;
        } else {
            return versaStackPrefix + name;
        }
    }

    public static String getResourceName(String topologyUri, String name) {
        //remove the topologyUri first
        if (name != null && name.startsWith(versaStackPrefix)) {
            name = name.replace(versaStackPrefix, "");
        }
        return name;
    }
}
