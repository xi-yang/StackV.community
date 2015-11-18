/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.common;

<<<<<<< HEAD
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJBException;

/**
 *
 * @author muzcategui
 */
=======
>>>>>>> zwang126-M6-Openstack-fix
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJBException;

/**
 *
 * @author muzcategui
 */
public class ResourceTool {

    private static final String versaStackPrefix = "urn:ogf:network:";

    public static String getResourceUri(String name, String pattern, String... patterParam) {
        String uri;
        //check if name starts with versaStackPrefix
        if (name.startsWith(versaStackPrefix)) {
            //string already starts with right prefix we can remove
            return name;

        } else {
            //name does not start with prefix
            //check if it at least matches pattern           
            if (patternMatch(name,pattern) == true) {
                //just append prefix
                name = versaStackPrefix + name;
            } else {
                pattern = String.format(pattern, patterParam);
                name = versaStackPrefix + pattern;
            }
            return name;
        }
    }

    public static String getResourceName(String name, String pattern) {
        //remove the topologyUri first
        if (name.startsWith(versaStackPrefix)) {
            name = name.replace(versaStackPrefix, "");
            if (patternMatch(name, pattern) == true) {
                return getShortName(name);
            } else {
                return versaStackPrefix + name;
            }
        } else {
            throw new EJBException(String.format("Resource %s does not contain valid"
                    + " URI", name));
        }
    }

    private static boolean patternMatch(String name,String pattern) {

        //if we have "vpc+%s:subnet+%s" convert to "vpc[+].+:subnet[+].+ 
        pattern = pattern.replaceAll("[+]", "[+]");
        pattern =pattern.replaceAll("%s",".+");
        
        Pattern pat = Pattern.compile(pattern);
        Matcher match = pat.matcher(name);
        return match.matches(); 
    }

    private static String getShortName(String name) {
        //TODO not a good logic
        String params[] = name.split("[+]");
        
        //get the last one of params this will be the short name
        String shortName = params[params.length -1];
        
        return shortName;
    }
}
