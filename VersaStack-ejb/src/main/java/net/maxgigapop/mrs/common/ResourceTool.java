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

    public static String getResourceUri(String name, String pattern) {
        String uri;
        //check if name starts with versaStackPrefix
        if (name.startsWith(versaStackPrefix)) {
            //string already starts with right prefix we can remove
            String tmp = name.replace(versaStackPrefix,"");
            //check for the correct pattern
            if(tmp.startsWith(pattern)){
                return name;
            }
            else
            {
                name = pattern + name;
            }
            
        } else {
            //name does not start with prefix
            //check if it at least starts with pattern
            if(name.startsWith(pattern)){
                //just append prefix
                name = versaStackPrefix + name;
            }
            else
            {
                name = versaStackPrefix + pattern + name;
            }
        }
        return name;
    }

    public static String getResourceName(String name, String pattern) {
        //remove the topologyUri first
        if (name.startsWith(versaStackPrefix)) {
            name = name.replace(versaStackPrefix, "");
            
            if (name.startsWith(pattern)){
               name = name.replace(pattern,"");
            }
        }
        return name;
    }
}
