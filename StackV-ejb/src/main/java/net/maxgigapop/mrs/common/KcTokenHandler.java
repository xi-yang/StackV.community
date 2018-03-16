/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.SocketTimeoutException;
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
    String accessToken = null;
    long accessCreationTime;
    String refreshToken = null;
    int recur = 0;
    
    
    
    public String getToken(String user, String pwd) {
        if (user.isEmpty() || pwd.isEmpty()) {
            return null;
        } else {
            return setAndGetToken(user, pwd);            
        }
    }
    
    
    private String setAndGetToken(String user, String pwd) {
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

            if (recur != 0) {
                logger.status(method, "Refresh achieved after " + recur + " retry");
            }
            
            String tokString = (String) result.get("access_token");
            if (tokString == null) {
                return "Error: could not get token";
            }
            
            accessCreationTime = System.nanoTime();                                 
            accessToken = tokString;
        } catch (ParseException | IOException ex) {
            logger.catching(method, ex);
        }
        return accessToken;
    }

    public String refreshToken(String refreshToken) {
        if (refreshToken != null) {
            long elapsed = (System.nanoTime() - accessCreationTime) / 1000000;
            if (elapsed > 45000) {
                accessToken = refreshTokenSub(0);
            }
        }
        return accessToken;
    }


    private String refreshTokenSub(int recur) {
        String method = "refreshToken";
        if (recur == 10) {
            logger.error(method, "Keycloak refresh connection failure!");
            return null;
        }

        try {
            URL url = new URL(kc_url + "/realms/StackV/protocol/openid-connect/token");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            // restapi
            //String encode = "cmVzdGFwaTpjMTZkMjRjMS0yNjJmLTQ3ZTgtYmY1NC1hZGE5YmQ4ZjdhY2E=";
            // StackV
            String encode = "U3RhY2tWOmFlNTNmYmVhLTg4MTItNGMxMy05MThmLTAwNjVhMTU1MGI3Yw==";

            conn.setRequestProperty("Authorization", "Basic " + encode);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            String data = "grant_type=refresh_token&refresh_token=" + refreshToken;
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

            if (recur != 0) {
                logger.status(method, "Refresh achieved after " + recur + " retry");
            }
            accessCreationTime = System.nanoTime();
            return (String) result.get("access_token");
        } catch (SocketTimeoutException | java.net.ConnectException ex) {
            // Keycloak connection timeout
            try {
                recur++;
                logger.warning(method, "Keycloak refresh timeout #" + recur);
                Thread.sleep(3000);
                return refreshTokenSub(recur);
            } catch (InterruptedException ex1) {
                logger.catching(method, ex);
            }
        } catch (ParseException | IOException ex) {
            logger.catching(method, ex);
        }
        return null;
    }
}
