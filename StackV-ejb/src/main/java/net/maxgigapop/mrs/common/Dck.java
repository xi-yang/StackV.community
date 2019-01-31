/*
* Copyright (c) 2013-2018 University of Maryland
* Created by: SaiArvind Ganganapalle 2018
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and/or hardware specification (the “Work”) to deal in the
* Work without restriction, including without limitation the rights to use,
* copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
* the Work, and to permit persons to whom the Work is furnished to do so,
* subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Work.
*
* THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
* THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS
* IN THE WORK.
 */
package net.maxgigapop.mrs.common;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author saiarvind
 */
public class Dck {
    
    public static final StackLogger logger = new StackLogger(Dck.class.getName(), "Dck");
    
    JSONParser parser = new JSONParser();
    
    IPATool ipaTool; 
    
    public Dck() {
        ipaTool = new IPATool();
    }
    
    /**
     * Posts the given JSON object to the IPA server and returns the JSON response
     * @param ipaJSON
     * @return 
     */
    private JSONObject runIpaRequest(JSONObject ipaJSON) {
        JSONObject resultJSON = new JSONObject();
        if (ipaTool == null) {
            ipaTool = new IPATool();
        }
        
        String response = ipaTool.ipaEndpoint(ipaJSON.toJSONString());
        
        try {
            resultJSON = (JSONObject) parser.parse(response);
        } catch (ParseException ex) {            
            logger.error("runIpaRequest", "IPA Request failed. Exception: " + ex);
            resultJSON.put("error", true);
        }
               
        return resultJSON;
    }
    
    /**
     * Adds a new DCK container for a new domain.
     * @param cn - required
     * @param description - optional
     * @param name - optional
     * @param dckOption - optional
     * @param aci - optional 
     * @param fqdn - optional
     */
    public String addDCKDomain(String cn, String description, String name, String dckOption, String aci, String fqdn) {
        String result = "";
        if (cn.isEmpty()) {
            // cn is required - return error string if empty
            result = "CN-IS-REQUIRED";
            return result;
        }
        
        JSONObject dckResponseJSON = createDCKDomain(cn, description, name, dckOption, aci, fqdn);
       
        // parse the JSON response for the address
        // if there is no error
        if (dckResponseJSON.get("error") == null && dckResponseJSON.get("result") != null) {
            result = "SUCCESS";
        }
        
        return result;
    }
    
    
    /**
     * Adds either new VM container or VM attributes. If attrs is set to null, adds
     * a VM container, otherwise adds attributes to the VM.
     * NOTE: assumes the correct LDAP directory structure has been made and already
     * exists
     * 
     * containerAttrs structure:
     * {
     *  containerAttr1:value1,
     *  containerAttr2:value2
     * }
     * 
     * vmAttrs structure:
     * {
     *  vmAttr1CN: value1,
     *  vmAttr2CN: value2
     * }
     * @param cn
     * @param domainTopUri
     * @param subDir
     * @param containerAttrsString (is the top level entry - objectclass is dckConfig not dckContainer)
     * @param vmAttrsString (sublevel/nested entries
     * @return 
     */
    public String addDCKVMEntry(String cn, String domainTopUri, String subDir, String containerAttrsString, String vmAttrsString) {        
        System.out.println("Dck.java - cn: " + cn + ", domain top uri: " + domainTopUri);        
        JSONObject containerAttrs = null;
        JSONObject vmAttrs = null;
        try {
            containerAttrs = (JSONObject) parser.parse(containerAttrsString);
            vmAttrs = (JSONObject) parser.parse(vmAttrsString);
        } catch (ParseException ex) {
            logger.error("addDCKVMEntry", "Invalid JSON: " + ex);
        }
        
        String result = "";
        if (cn.isEmpty()) {
            // cn is required - return error string if empty
            result = "cn-IS-REQUIRED";
            return result;
        }
        if (domainTopUri.isEmpty()) {
            // domainTopUri is required - return error string if empty
            result = "domainTopUri-IS-REQUIRED";
            return result;
        }
        
        // first create VM container
        String containerPath = createDCKPath(subDir);
        System.out.println("**Dck.java - addDckVMEntry - containerPAth: " + containerPath);
        JSONObject vmContainerResponseJSON = md2AddEntry(cn, "dckConfig", containerPath, domainTopUri, containerAttrs);
        // check the response to make sure the container was created - if not return an error result
        System.out.println("***addDCKVMEntry - vmContainerResponseJSON: " + vmContainerResponseJSON.toString());
        
        System.out.println("***addDCKVMEntry - vmAttrs: " + vmAttrs.toString());
        
        // once the VM dck container has been created, then add all the attributes of the VM stored in attrs
        System.out.println("***addDCKVMEntry - vmAttrs.keySet(): " + vmAttrs.keySet().toString());
        Iterator<String> iterator = vmAttrs.keySet().iterator();
        String attributePath = containerPath + cn;
        System.out.println("**Dck.java - addDckVMEntry - attributePath: " + attributePath);
        while (iterator.hasNext()) {
            String vmAttrCN = iterator.next();
            System.out.println("***addDCKVMEntry - vmAttrCN: " + vmAttrCN);
            System.out.println("***addDCKVMEntry - vmAttrs.get(vmAttrCN): " + vmAttrs.get(vmAttrCN));
            JSONObject vmAttrCNAttrs = new JSONObject();
            vmAttrCNAttrs.put("dckOption", vmAttrs.get(vmAttrCN));
            JSONObject vmAttrResponseJSON = md2AddEntry(vmAttrCN, "dckConfig", attributePath, domainTopUri, vmAttrCNAttrs);
            
            // check the result to make sure the attribute was added - if not rollback any changes
            System.out.println("***addDCKVMEntry - vmAttrResponseJSON: " + vmAttrResponseJSON.toString());
        }
        
        return result;
    }
    
    /**
     * Creates and returns the DCK VM path. Specifying subDir will create a path 
     * with the subDir
     * @param subDir
     * @return 
     */
    private String createDCKPath(String subDir) {
        return "/automation/servers/" + (subDir.isEmpty() ? "" : subDir + "/");
    }
    
    private JSONObject createDCKDomain(String cn, String description, String name, String dckOption, String aci, String fqdn) {
        JSONObject dckJSON = new JSONObject();
        
        dckJSON.put("id", 0);
        dckJSON.put("method", "dck_add");
        
        JSONArray paramsArr = new JSONArray();
        paramsArr.add(new JSONArray());
        
        JSONObject paramsArrArgs = new JSONObject();
        paramsArrArgs.put("cn", cn);
        
        if (description != null && !description.isEmpty()) {
            paramsArrArgs.put("description", description);
        }
        
        if (name != null && !name.isEmpty()) {
            paramsArrArgs.put("name", name);
        }
        
        if (dckOption != null && !dckOption.isEmpty()) {
            paramsArrArgs.put("dckOption", dckOption);
        }
        
        if (aci != null && !aci.isEmpty()) {
            paramsArrArgs.put("aci", aci);
        }
        
        if (fqdn != null && !fqdn.isEmpty()) {
            paramsArrArgs.put("fqdn", fqdn);
        }
        
        paramsArr.add(paramsArrArgs);
        
        dckJSON.put("params", paramsArr);
        
        return runIpaRequest(dckJSON);
    }
    
    
    /**
     * Adds a new LDAP entry into the DCK domains container
     * @return 
     */
    private JSONObject md2AddEntry(String cn, String objectClass, String path, String domainTopUri, JSONObject attrs) {
        // cn, objectclass, the domain top uri are all required
        if (isStrNullOrEmpty(cn) || isStrNullOrEmpty(path) || isStrNullOrEmpty(domainTopUri)) {
            return null;
        }
        
        JSONObject md2JSON = new JSONObject();
        
        md2JSON.put("id", 0);
        md2JSON.put("method", "md2_add_entry");
        
        JSONArray paramsArr = new JSONArray();
        paramsArr.add(new JSONArray()); // empty array
        
        // create the entryjson string escaped object with the parameters and attrs
        JSONObject entryJson = new JSONObject();
        entryJson.put("cn", cn);
        entryJson.put("objectclass", objectClass);
        entryJson.put("path", path);
        System.out.println("**DCK - md2AddEntry - path: " + path);
        entryJson.put("domain", domainTopUri);
        
        // check if attrs is null - if so only a VM container is being created
        // if not null, then attributes are being added to an existing VM container
        if (attrs != null) {
            // now have to put all the attrs in the entryjson         
            Iterator<String> iterator = attrs.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                entryJson.put(key, attrs.get(key));
            }            
        }
        
        JSONObject paramsArrArgs = new JSONObject();
        paramsArrArgs.put("entryjson", entryJson.toJSONString());
        
        paramsArr.add(paramsArrArgs);
        
        md2JSON.put("params", paramsArr);
        
        return runIpaRequest(md2JSON);
    }
    
    
    private boolean isStrNullOrEmpty(String s) {
        return (s == null || s.isEmpty());
    }
    
}
