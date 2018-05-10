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

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.net.SocketTimeoutException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author rikenavadur
 */
public class TokenHandler {

    private final StackLogger logger = new StackLogger("net.maxgigapop.mrs.rest.api.WebResource", "TokenHandler");
    private final String kc_url = System.getProperty("kc_url");
    
    private String kc_realm = "StackV";
    private String kc_encode = "U3RhY2tWOmFlNTNmYmVhLTg4MTItNGMxMy05MThmLTAwNjVhMTU1MGI3Yw==";
    private String auth = "Basic " + kc_encode;    
    private String durl = kc_url + "/realms/" + kc_realm + "/protocol/openid-connect/token";

    private final OkHttpClient client = new OkHttpClient();
    private static final MediaType URL = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
    private String requestData;
    
    JSONParser parser = new JSONParser();
    String accessToken;
    long accessCreationTime;
    String refreshToken;
    int recur = 0;

    public TokenHandler(String refresh) {
        if (refresh == null || refresh.isEmpty()) {
            logger.error("init", "No refresh token present!");
        }
        refreshToken = refresh;
        
        requestData = "grant_type=refresh_token&refresh_token=" + refreshToken;
        refreshTokenSub(0);
    }    
    public TokenHandler(String refresh, String realm, String encode) {
        if (refresh == null || refresh.isEmpty()) {
            logger.error("init", "No refresh token present!");
        }
        refreshToken = refresh;
        kc_realm = realm;
        kc_encode = encode;        
        auth = "Basic " + kc_encode;
        durl = kc_url + "/realms/" + kc_realm + "/protocol/openid-connect/token";
        
        requestData = "grant_type=refresh_token&refresh_token=" + refreshToken;
        refreshTokenSub(0);
    }

    public void refreshToken() {
        long elapsed = (System.nanoTime() - accessCreationTime) / 1000000;
        if (elapsed > 50000) {
            refreshTokenSub(0);
        }
    }

    public String auth() {
        refreshToken();
        return "bearer " + accessToken;
    }

    private void refreshTokenSub(int recur) {
        String method = "refreshToken";
        if (recur == 20) {
            logger.error(method, "Keycloak refresh connection failure!");
            return;
        }

        try {            
            RequestBody body = RequestBody.create(URL, requestData);
            Request request = new Request.Builder().url(durl).header("Authorization", auth).post(body).build();

            try (ResponseBody response = client.newCall(request).execute().body()) {
                JSONObject ret = (JSONObject) parser.parse(response.string());

                accessToken = (String) ret.get("access_token");
                accessCreationTime = System.nanoTime();
                
                refreshToken = (String) ret.get("refresh_token");
                requestData = "grant_type=refresh_token&refresh_token=" + refreshToken;
            }

            if (recur != 0) {
                logger.status(method, "Refresh achieved after " + recur + " retry");
            }
        } catch (SocketTimeoutException | java.net.ConnectException ex) {
            // Keycloak connection timeout
            try {
                recur++;
                logger.warning(method, "Keycloak refresh timeout #" + recur);
                Thread.sleep(3000);
                refreshTokenSub(recur);
            } catch (InterruptedException ex1) {
                logger.catching(method, ex1);
            }
        } catch (ParseException | IOException ex) {
            logger.catching(method, ex);
        }
    }
}
