/*
 * Copyright (c) 2013-2018 University of Maryland
 * Created by: SaiArvind Ganganapalle 2017
 * Modified by: SaiArvind Ganganapalle 2017-2018

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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * This class provides the methods to lease addresses (IPs or MACs)
 * from the IPA ALM plugin. In addition, for every leased address, an EAS 
 * task will be created in order to release the address when the service 
 * instance is deleted.
 * @author saiarvind
 */
public class IpaAlm {
    public static final StackLogger logger = new StackLogger(IpaAlm.class.getName(), "IpaAlm");
    
    JSONParser parser = new JSONParser();
    
    KcTokenHandler kcTokenHandler = new KcTokenHandler();
    
    // using HTTP not HTTPS due to SSL cert checking
    String ipaRestBaseUrl = "http://localhost:8080/StackV-web/restapi/app/acl/ipa/request";
    
    // NEED THE KEYCLOAK AUTHORIZATION TOKEN IN THE HEADERS
    String kcToken;
    
    /**
     * 
     * @param username - KeyCloak username
     * @param passwd - KeyCloak password
     */
    public IpaAlm(String username, String passwd) {
        kcToken = kcTokenHandler.setAndGetToken(username, passwd);
        ipaLogin();
    }
    
   
    private boolean ipaLogin() {        
        boolean loggedIn = false;
        // using HTTP not HTTPS due to SSL cert checking
        String ipaLoginUrl = "http://localhost:8080/StackV-web/restapi/app/acl/ipa/login";
        try {
            URL ipaurl = new URL(ipaLoginUrl);
            //HttpsURLConnection conn = (HttpsURLConnection) ipaurl.openConnection();
            HttpURLConnection conn = (HttpURLConnection) ipaurl.openConnection();
            conn.setRequestProperty("Authorization", "bearer " + kcToken);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.connect();                       
            
            // if the request is successful
            if (200 <= conn.getResponseCode() && conn.getResponseCode() <= 299) {  
                loggedIn = true;
            } else { // if the request fails
                String errorStream = "";
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    String inputLine;                    
                    while ((inputLine = in.readLine()) != null) {
                        errorStream += inputLine;
                    }
                }
            }
            
        } catch (MalformedURLException ex) {
            Logger.getLogger(IpaAlm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(IpaAlm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        
        return loggedIn;
    }
    
    /**
     * Posts the given JSON object to the IPA server and returns the JSON response
     * @param ipaJSON
     * @return 
     */
    private JSONObject runIpaRequest(JSONObject ipaJSON) {
        JSONObject resultJSON = new JSONObject();
        
        try {
            URL ipaurl = new URL(ipaRestBaseUrl);
            // HttpsURLConnection conn = (HttpsURLConnection) ipaurl.openConnection();
            HttpURLConnection conn = (HttpURLConnection) ipaurl.openConnection();
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "bearer " + kcToken);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.connect();   
            
            DataOutputStream wr = new DataOutputStream((conn.getOutputStream()));                    
            wr.writeBytes(ipaJSON.toJSONString());
            wr.flush();
            conn.connect();
            
            StringBuilder responseStr;
            // if the request is successful
            if (200 <= conn.getResponseCode() && conn.getResponseCode() <= 299) {               
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String inputLine;
                    responseStr = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        responseStr.append(inputLine);
                    }
                }
                resultJSON = (JSONObject) parser.parse(responseStr.toString());
                
            } else { // if the request fails
                String errorStream = "";
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    String inputLine;                    
                    while ((inputLine = in.readLine()) != null) {
                        errorStream += inputLine;
                    }
                    resultJSON = (JSONObject) parser.parse(errorStream);
                } catch (ParseException ex) {
                    Logger.getLogger(IpaAlm.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
        } catch (MalformedURLException ex) {
            Logger.getLogger(IpaAlm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IOException | ParseException ex) {
            Logger.getLogger(IpaAlm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        
        return resultJSON;
    }
    
    /**
     * Creates the JSON object for leasing an address.
     * @param clientId
     * @param poolName
     * @param poolType
     * @return 
     */
    private JSONObject createLease(String clientId, String poolName, String poolType, String specificAddr) {
        JSONObject leaseJSON = new JSONObject();
        leaseJSON.put("id", 0);
        leaseJSON.put("method", "alm_lease");
        
        JSONArray paramsArr = new JSONArray();
        paramsArr.add(new JSONArray());
        
        JSONObject paramsArrArgs = new JSONObject();
        paramsArrArgs.put("clientid", clientId);
        paramsArrArgs.put("poolname", poolName);
        paramsArrArgs.put("almpooltype", poolType);
        
        // if the specific address is provided then pass that in as a parameter
        if (specificAddr != null && !specificAddr.isEmpty()) {
            paramsArrArgs.put("requiredaddress", specificAddr);
        }
        
        paramsArr.add(paramsArrArgs);
        
        leaseJSON.put("params", paramsArr);
        
        return runIpaRequest(leaseJSON);
    }
    
    
    /**
     * Creates the JSON object for revoking a leased address.
     * @param clientId
     * @param poolName
     * @param poolType
     * @param leasedAddr
     * @return 
     */
    private JSONObject revokeLease(String clientId, String poolName, String poolType, String leasedAddr) {
        JSONObject leaseJSON = new JSONObject();
        leaseJSON.put("id", 0);
        leaseJSON.put("method", "alm_release");
        
        JSONArray paramsArr = new JSONArray();
        paramsArr.add(new JSONArray());
        
        JSONObject paramsArrArgs = new JSONObject();
        paramsArrArgs.put("clientid", clientId);
        paramsArrArgs.put("poolname", poolName);
        paramsArrArgs.put("almpooltype", poolType);
        paramsArrArgs.put("leasedaddress", leasedAddr);
        paramsArr.add(paramsArrArgs);
        
        leaseJSON.put("params", paramsArr);
        
        
        return runIpaRequest(leaseJSON);
    }
    
    private JSONObject showAlmLease(String poolName, String commonName) {
        JSONObject leaseJSON = new JSONObject();
        leaseJSON.put("id", 0);
        leaseJSON.put("method", "almleases_show");
        
        JSONArray paramsArr = new JSONArray();
        paramsArr.add(new JSONArray());
        
        JSONObject paramsArrArgs = new JSONObject();
        paramsArrArgs.put("cn", commonName);
        paramsArr.add(paramsArrArgs);
        
        leaseJSON.put("params", paramsArr);
        
        
        return runIpaRequest(leaseJSON);
    }
    
    /**
     * Returns the leased address if the operation was successful, null otherwise
     * @param clientId
     * @param poolName
     * @param poolType
     * @param specificAddr
     * @return 
     */
    public String leaseAddr(String clientId, String poolName, String poolType, String specificAddr, String topUri) {
        // return null if an address cannot be leased
        String addr = null;
        String dn = null;
        
        // pass the parameters and run the request
        JSONObject leaseResponseJSON = createLease(clientId, poolName, poolType, specificAddr);
        
        
        // parse the JSON response for the address
        // if there is no error
        if (leaseResponseJSON.get("error") == null && leaseResponseJSON.get("result") != null) {
            JSONObject resultJSON = (JSONObject) leaseResponseJSON.get("result");
            if (resultJSON.get("value").equals("successfully lease!")) {
                // get the inner result object
                JSONObject innerResultJSON = (JSONObject) resultJSON.get("result");
                
                // get the dn needed for creating the EAS task
                dn = (String) innerResultJSON.get("dn");
                
                // get the almstatments array
                JSONArray almstmtsArr = (JSONArray) innerResultJSON.get("almstatements");
                Iterator<String> iterator = almstmtsArr.iterator();
                while (iterator.hasNext()) {
                    String almElement = iterator.next();
                    if (almElement.contains("leasedaddr")) {
                        // get the string directly after the space after "leasedaddr"
                        addr = almElement.substring(11);
                        break; // as soon as the leased address is found - break out of the loop
                    }
                }
            }
        }
        
        // create the EAS Task
        boolean easTaskCreated = createEasTask(dn, topUri);
        
        // if address was leased successfully and the the task was created
        // then return the address
        if (addr != null && addr.length() > 0 && easTaskCreated) {
            logger.warning("leaseAddr", "***address leased: " + addr);
            return addr;
        } else if (addr != null && addr.length() > 0 && !easTaskCreated) {
            // if the address was leased succesfully but the task creation failed
            // then release (revoke) the address and return LEASE_FAILED
            revokeLeasedAddr(clientId, poolName, poolType, addr);
            logger.warning("leaseAddr", "***LEASING FAILED DUE TO TASK CREATION FAILURE***: " + easTaskCreated);
            return "LEASE_FAILED";
            // NOTE: do not have to check if the address leasing failed and
            // the eas task created succeeded since sucessful leasing is 
            // a prerequiste for task creation
        } else {
            logger.warning("leaseAddr", "***LEASING FAILED***");
            return "LEASE_FAILED";
        }
    }
    
    /**
     * Returns true if the address was successfully revoked, false otherwise
     * @param clientId
     * @param poolName
     * @param poolType
     * @param leasedAddr
     * @return 
     */
    public boolean revokeLeasedAddr(String clientId, String poolName, String poolType, String leasedAddr) {
        boolean revoked = false;        
                     
        // pass the parameters and run the request
        JSONObject revokeResponseJSON = revokeLease(clientId, poolName, poolType, leasedAddr);
        
        // parse the JSON response to check if the revocation was successful
        // if there is no error
        if (revokeResponseJSON.get("error") == null && revokeResponseJSON.get("result") != null) {
            JSONObject resultJSON = (JSONObject) revokeResponseJSON.get("result");
            if (resultJSON.get("value").equals("successfully release!")) {
                revoked = true;
            }
        } 
                
        
        return revoked;
    }
    
    public boolean checkIfAddrLeased(String poolName, String leasedAddr) {
        boolean leased = false;
        
        // pass the parameters and run the request
        String commonName = poolName + "-" + leasedAddr;
        JSONObject checkAddrJSON = showAlmLease(poolName, commonName);
        
        // parse the JSON response to check if the address has been leased
        // if there is no error - then the address was found indicating it has been leased
        if (checkAddrJSON.get("error") == null && checkAddrJSON.get("result") != null) {
            leased = true;
        }
        
        return leased;
    }
    
    private boolean createEasTask(String dn, String topUri) {
        String cn = topUri + "_alm-address";
        JSONObject createEasTaskResponseJSON = createEasTaskJSON(dn,cn);
        
         // parse the JSON response for the address
        // if there is no error
        if (createEasTaskResponseJSON.get("error") == null && createEasTaskResponseJSON.get("result") != null) {
            JSONObject resultJSON = (JSONObject) createEasTaskResponseJSON.get("result");
            JSONObject innerResultJSON = (JSONObject) resultJSON.get("result");
            if (innerResultJSON.get("value").equals(cn)) {
                logger.trace("creatEasTask", "EAS Task creation success");
                return true;
            }
        }
        
        logger.warning("createEasTask", "EAS Task creation failed. Response object: " + createEasTaskResponseJSON.toJSONString());
        return false;
    }
    
    private JSONObject createEasTaskJSON(String dn, String cn) {
        JSONObject leaseJSON = new JSONObject();
        leaseJSON.put("id", 0);
        leaseJSON.put("method", "easTask_add");
                      
        JSONArray paramsArr = new JSONArray();
        paramsArr.add(new JSONArray());
        
        JSONObject paramsArrArgs = new JSONObject();
        paramsArrArgs.put("cn", cn);
        paramsArrArgs.put("eastasktype", "alm-lease");
        paramsArrArgs.put("eastaskaction", "delete");
        paramsArrArgs.put("eastaskstatus", "INIT");
        
        JSONArray easTaskTriggers = new JSONArray();
        JSONObject trigger = new JSONObject();        
        trigger.put("type", "ldap-query");
        trigger.put("dn", "cn=TBD,cn=TBD,cn=TBD,cn=orchestrators,cn=stackv,dc=TBD,dc=TBD,dc=TBD");      
        trigger.put("exists", false);
        easTaskTriggers.add(trigger);
        
        paramsArrArgs.put("eastasktriggers", easTaskTriggers.toJSONString());
        paramsArrArgs.put("eastasklockedby", "lockedByTransaction");
        paramsArrArgs.put("eastasklockexpires", "150000000");
        paramsArrArgs.put("eastaskresourcerefdn", dn);
        JSONObject taskOptions = new JSONObject();
        taskOptions.put("DispatchGroup", "group");
        paramsArrArgs.put("eastaskoptions", taskOptions.toJSONString());
        
       
        paramsArr.add(paramsArrArgs);
        
        leaseJSON.put("params", paramsArr);
        
        return runIpaRequest(leaseJSON);
    }
}
