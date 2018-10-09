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
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author saiarvind
 */
public class KcTokenHandler {
    
    // transfer these to the Wildfly properties?
    private final String kc_client_secret = "ae53fbea-8812-4c13-918f-0065a1550b7c";
    
    // server + context (maxgigapop.net:####/auth)
    private final String kc_url = System.getProperty("kc_url");
    
    private final StackLogger logger = new StackLogger("net.maxgigapop.mrs.common.KcTokenHandler", "KcTokenHandler");
    JSONParser parser = new JSONParser();
    private String accessToken = null;
    long accessCreationTime;
    
    
    /**
     * Returns the accessToken after trying to get the token via the credentials
     * provided. If username or password are empty, returns null.
     * @param user
     * @param pwd
     * @return String - keycloak token
     */
    public String setAndGetToken(String user, String pwd) {
        if (user.isEmpty() || pwd.isEmpty()) {
            return null;
        } else {
            setToken(user, pwd);
            return getToken();
        }
    }
    
    /**
     * Returns the accessToken. If the user has not called the 
     * @return String -  accessToken
     */
    public String getToken() {
        return accessToken;
    }
    
    /**
     * Set the accessToken. If the request was a failure, the accessToken is set
     * to null. Otherwise the accessToken is set the value of the new token
     * @param user
     * @param pwd 
     */
    private void setToken(String user, String pwd) {
        String method = "login";
        try {
            URL url = new URL(kc_url + "/realms/StackV/protocol/openid-connect/token");            
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            String data = "username=" + user + "&password=" + pwd + "&grant_type=password&client_id=StackV&client_secret=" + kc_client_secret;
            try (OutputStream os = conn.getOutputStream(); BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"))) {
                writer.write(data);
                writer.flush();
            }

            conn.connect();
            StringBuilder responseStr;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                responseStr = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    responseStr.append(inputLine);
                }
            }
            Object obj = parser.parse(responseStr.toString());
            JSONObject result = (JSONObject) obj;

            String token = (String) result.get("access_token");
            
            if (token == null) {
                accessToken = null;
            } else {
                accessCreationTime = System.nanoTime();                                 
                accessToken = token;                
            }
             
            
        } catch (ParseException | IOException ex) {
            logger.catching(method, ex);
        }        
    }
    
}
