/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.driver.openstack;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 *
 * @author james
 */
public class OpenStackRESTClient {
    
        // Authorize with OpenStack and get a token id
    public static String getToken(String host, String tenant, String username, String password) throws IOException {
        
        String url = String.format("http://%s:5000/v2.0/tokens", host);
        String body = String.format("{\"auth\": {\"tenantName\": \"%s\", \"passwordCredentials\": {\"username\": \"%s\", \"password\": \"%s\"}}}", tenant, username, password);
        
        URL obj = new URL(url);  
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        String responseStr = sendPOST(obj, con, body);
        JSONObject responseJSON = (JSONObject) JSONValue.parse(responseStr);
        String tokenId = (String) ((JSONObject)((JSONObject)responseJSON.get("access")).get("token")).get("id");
        
        return tokenId;        
    }
    
    public static String getTenantId(String host, String tenantName, String token) throws IOException {
        
        String url = String.format("http://%s:35357/v2.0/tenants", host);
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        String responseStr = sendGET(obj, con, "python-keystoneclient", token);
        //System.out.println(responseStr);
        JSONObject responseJSON = (JSONObject) JSONValue.parse(responseStr);
        JSONArray tenants = (JSONArray) responseJSON.get("tenants");
        
        String tenantId = "";
        for (Object tenant : tenants) {
            if (((JSONObject) tenant).get("name").equals(tenantName)) {
                tenantId = (String) ((JSONObject) tenant).get("id");
            }
        }
        
        return tenantId;        
    }
    
    // Query the OpenStack API for Nova information
    public static JSONArray pullNovaConfig(String host, String tenantId, String token) throws IOException {
        
        // Get list of compute nodes
        String url = String.format("http://%s:8774/v2/%s/os-hosts", host, tenantId);
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        String responseStr = sendGET(obj, con, "", token);
        
        JSONObject responseJSON = (JSONObject) JSONValue.parse(responseStr);
        
        JSONArray novaDescription = new JSONArray();
                
        //novaDescription.add(responseJSON);
        JSONArray nodes = (JSONArray) responseJSON.get("hosts");
        
      
        
        for (Object h : nodes) {
            if(((JSONObject) h).get("service").equals("compute")) {
                String nodeName = (String) ((JSONObject) h).get("host_name");
                
                url = String.format("http://%s:8774/v2/%s/os-hosts/%s", host, tenantId, nodeName);
                obj = new URL(url);
                con = (HttpURLConnection) obj.openConnection();

                responseStr = sendGET(obj, con, "", token);
                responseJSON = (JSONObject) JSONValue.parse(responseStr);
                
                novaDescription.add(responseJSON);
            } else if(((JSONObject) h).get("service").equals("network")) {
                String hostName = (String) ((JSONObject) h).get("host_name");
                JSONObject networkNode = (JSONObject) JSONValue.parse(String.format("{\"network_host\": {\"host_name\": \"%s\", }}", hostName));
                novaDescription.add(networkNode);
            }
        }
        
        return novaDescription;             
    }
    
    public static JSONArray pullNovaVM(String host, String tenantId, String token) throws IOException {
        
        String url = String.format("http://%s:8774/v2/%s/servers/detail", host, tenantId);
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        String responseStr = sendGET(obj, con, "", token);
        //System.out.println(responseStr);
        
        JSONObject responseJSON = (JSONObject) JSONValue.parse(responseStr);
        JSONArray servers = (JSONArray) responseJSON.get("servers");
                
        return servers;        
    }
    
    public static JSONObject pullNeutron(String host, String tenantId, String token) throws IOException {
        
        String url = String.format("http://%s:9696/v2.0/subnets", host, tenantId);
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        String responseStr = sendGET(obj, con, "", token);
        System.out.println(responseStr);
              
        url = String.format("http://%s:9696/v2.0/networks", host, tenantId);
        obj = new URL(url);
        con = (HttpURLConnection) obj.openConnection();

        responseStr = sendGET(obj, con, "", token);
        System.out.println(responseStr);
        
        url = String.format("http://%s:9696/v2.0/ports", host, tenantId);
        obj = new URL(url);
        con = (HttpURLConnection) obj.openConnection();

        responseStr = sendGET(obj, con, "", token);
        System.out.println(responseStr);
        JSONObject ports = (JSONObject) JSONValue.parse(responseStr);
       
        return ports;
    }
    
    private static String sendPOST(URL url, HttpURLConnection con, String body) throws IOException {
        
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-type", "application/json");
        con.setRequestProperty("Accept", "application/json");

        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(body);
        wr.flush();
        wr.close();

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'POST' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder responseStr = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            responseStr.append(inputLine);
        }
        in.close();
        
        return responseStr.toString();
        
    }
    
    private static String sendGET(URL url, HttpURLConnection con, String userAgent, String tokenId) throws IOException {
        
        con.setRequestMethod("GET");        
        con.setRequestProperty("User-Agent", userAgent);
        con.setRequestProperty("X-Auth-Token", tokenId);

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);
        
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder responseStr = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            responseStr.append(inputLine);
        }
        in.close();
        
        return responseStr.toString();
    }

}