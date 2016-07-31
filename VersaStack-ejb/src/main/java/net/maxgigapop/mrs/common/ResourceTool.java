/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2014

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

package net.maxgigapop.mrs.common;

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
                if (pattern.startsWith(versaStackPrefix)) {
                    name = pattern;
                } else {
                    name = versaStackPrefix + pattern;
                }
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
