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
    
    String ipaRestBaseUrl = "https://localhost:8080/StackV-web/restapi/app/acl/ipa/request";
    
    // NEED THE KEYCLOAK AUTHORIZATION TOKEN IN THE HEADERS
    
    public boolean ipaLogin() {
        boolean loggedIn = false;
        String ipaLoginUrl = "https://localhost:808/StackV-web/restapi/app/acl/ipa/login";
        try {
            URL ipaurl = new URL(ipaLoginUrl);
            HttpsURLConnection conn = (HttpsURLConnection) ipaurl.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.connect();                       
            
            // if the request is successful
            if (200 <= conn.getResponseCode() && conn.getResponseCode() <= 299) {  
                System.out.println(conn.getHeaderFields());
                
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
    
    public JSONObject runIpaRequest(JSONObject ipaJSON) {
        JSONObject resultJSON = new JSONObject();
        
        try {
            URL ipaurl = new URL(ipaRestBaseUrl);
            HttpsURLConnection conn = (HttpsURLConnection) ipaurl.openConnection();
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "text/plain");
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
                System.out.println(conn.getHeaderFields());
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
     * 
     * @param poolName - common name (cn parameter)
     * @param poolType
     * @param almRange
     * @return 
     */
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
    
    public String leaseMacAddr() {
        String macAddr = "";
        
        return macAddr;
    }
    
    public String leaseIPAddr() {
        String ipAddr = "";
        
        return ipAddr;
    }
    
    public void createAlmPool() {
        
    }
    
    public void delAlmPool() {
        
    }
}
