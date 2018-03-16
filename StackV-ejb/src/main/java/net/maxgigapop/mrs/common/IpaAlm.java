/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.common;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author saiarvind
 */
public class IpaAlm {
    public static final StackLogger logger = new StackLogger(IpaAlm.class.getName(), "IpaAlm");
    
    JSONParser parser = new JSONParser();
    
    KcTokenHandler kcTokenHandler = new KcTokenHandler();
    
    
    String ipaRestBaseUrl = "https://localhost:8443/StackV-web/restapi/app/acl/ipa/request";
    
    // NEED THE KEYCLOAK AUTHORIZATION TOKEN IN THE HEADERS
    String kcToken;
    
    public IpaAlm() {
        kcToken = kcTokenHandler.getToken("xyang", "MAX123!");        
    }
    
   
    public boolean ipaLogin() {        
        boolean loggedIn = false;
        String ipaLoginUrl = "https://localhost:8443/StackV-web/restapi/app/acl/ipa/login";
        try {
            URL ipaurl = new URL(ipaLoginUrl);
            HttpsURLConnection conn = (HttpsURLConnection) ipaurl.openConnection();
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
     * Runs the given JSON object and returns the JSON response
     * @param ipaJSON
     * @return 
     */
    public JSONObject runIpaRequest(JSONObject ipaJSON) {
        JSONObject resultJSON = new JSONObject();
        
        try {
            URL ipaurl = new URL(ipaRestBaseUrl);
            HttpsURLConnection conn = (HttpsURLConnection) ipaurl.openConnection();
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
    
    /*
    public JSONObject addAlmPool(String poolName, String poolType, String almRange) {
        JSONObject almPoolJSON = new JSONObject();
        almPoolJSON.put("id", 0);
        almPoolJSON.put("method", "almpool_add");
        
        JSONArray paramsArr = new JSONArray();
        paramsArr.add(new JSONArray());
        
        JSONObject paramsArrArgs = new JSONObject();
        paramsArrArgs.put("cn", poolName);
        paramsArrArgs.put("almpooltype", poolType);
        paramsArrArgs.put("almrange", almRange);
        paramsArr.add(paramsArrArgs);
        
        almPoolJSON.put("params", paramsArr);
        
        return runIpaRequest(almPoolJSON);
    }
    
    public JSONObject deletePool(String poolName) {
        JSONObject delPoolJSON = new JSONObject();
        delPoolJSON.put("id", 0);
        delPoolJSON.put("method", "almpool_del");
        
        JSONArray paramsArr = new JSONArray();
        paramsArr.add(new JSONArray());
        JSONObject paramsArrArgs = new JSONObject();
        paramsArrArgs.put("cn", poolName);
        paramsArr.add(paramsArrArgs);
        
        delPoolJSON.put("params", paramsArr);
        
        return runIpaRequest(delPoolJSON);
    }
    */
    
    public JSONObject createLease(String clientId, String poolName, String poolType) {
        JSONObject leaseJSON = new JSONObject();
        leaseJSON.put("id", 0);
        leaseJSON.put("method", "alm_lease");
        
        JSONArray paramsArr = new JSONArray();
        paramsArr.add(new JSONArray());
        
        JSONObject paramsArrArgs = new JSONObject();
        paramsArrArgs.put("clientid", clientId);
        paramsArrArgs.put("poolname", poolName);
        paramsArrArgs.put("almpooltype", poolType);
        paramsArr.add(paramsArrArgs);
        
        leaseJSON.put("params", paramsArr);
        
        return runIpaRequest(leaseJSON);
    }
    
    public JSONObject revokeLease(String clientId, String poolName, String poolType, String leasedAddr) {
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
    
    /**
     * --NOTE MIGHT WANT TO SEE IF THE ALMSTATMENTS LIST CAN GIVEN BACK AS JSON OBJECT INSTEAD OF JSON ARRAY--
     * Returns the leased address if the operation was successful, null otherwise
     * @param clientId
     * @param poolName
     * @param poolType
     * @return 
     */
    public String leaseAddr(String clientId, String poolName, String poolType) {
        // return null if an address cannot be leased
        String addr = null;
        
        // pass the parameters and run the request
        JSONObject leaseResponseJSON = createLease(clientId, poolName, poolType);
        
        
        // parse the JSON response for the address
        // if there is no error
        if (leaseResponseJSON.get("error") == null && leaseResponseJSON.get("result") != null) {
            JSONObject resultJSON = (JSONObject) leaseResponseJSON.get("result");
            if (resultJSON.get("value").equals("successfully lease!")) {
                // get the inner result object
                JSONObject innerResultJSON = (JSONObject) resultJSON.get("result");
                
                // get the almstatments array
                JSONArray almstmtsArr = (JSONArray) innerResultJSON.get("almstatements");
                Iterator<String> iterator = almstmtsArr.iterator();
                while (iterator.hasNext()) {
                    String almElement = iterator.next();
                    if (almElement.contains("leasedaddr")) {
                        addr = almElement.substring(11);
                        break; // as soon as the leased address is found - break out of the loop
                    }
                }
            }
        }
        
        
        return addr;
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
    
    /*
    public boolean createAlmPool(String commonName, String poolType, String range) {
        boolean poolCreated = false;
        
        return poolCreated;
    }
    
    public boolean delAlmPool(String commonName) {
        boolean poolDeleted = false;
        
        return poolDeleted;
    }
    */
}
