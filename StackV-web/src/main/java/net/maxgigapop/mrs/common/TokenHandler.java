/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Alberto Jimenez
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
 * @author rikenavadur
 */
public class TokenHandler {

    private final String kc_url = System.getProperty("kc_url");
    private final StackLogger logger = new StackLogger(TokenHandler.class.getName(), "TokenHandler");
    JSONParser parser = new JSONParser();
    String accessToken = null;
    String refreshToken = null;
    int recur = 0;

    public TokenHandler(String refresh) {
        refreshToken = refresh;
        accessToken = refreshTokenSub(0);

    }

    public void refreshToken() {
        accessToken = refreshTokenSub(0);
    }

    public String auth() {
        refreshToken();
        return "bearer " + accessToken;
    }

    private String refreshTokenSub(int recur) {
        String method = "refreshToken";
        if (recur == 10) {
            logger.error(method, "Keycloak refresh connection failure!");
            return null;
        }

        try {
            logger.trace_start(method);
            URL url = new URL(kc_url + "/realms/StackV/protocol/openid-connect/token");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            // restapi
            //String encode = "cmVzdGFwaTpjMTZkMjRjMS0yNjJmLTQ3ZTgtYmY1NC1hZGE5YmQ4ZjdhY2E=";
            // StackV
            String encode = "U3RhY2tWOjQ4OTdlOGMzLWI4MzctNDIxMS1hOGYyLWFmM2Q2ZTM2M2RmMg==";

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

            logger.trace_end(method);
            return (String) result.get("access_token");
        } catch (SocketTimeoutException | java.net.ConnectException ex) {
            // Keycloak connection timeout
            try {
                recur++;
                logger.warning(method, "Keycloak refresh timeout #" + recur);
                Thread.sleep(2000);
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
