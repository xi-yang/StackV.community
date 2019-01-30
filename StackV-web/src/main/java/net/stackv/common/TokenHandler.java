/*
 * Copyright (c) 2013-2018 University of Maryland
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
package net.stackv.common;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;

import net.maxgigapop.mrs.common.StackLogger;

/**
 *
 * @author rikenavadur
 */
public class TokenHandler {
    private Map<String, Object> cred = new HashMap<String, Object>() {
        private static final long serialVersionUID = 1L;
        {
            put("secret", "ae53fbea-8812-4c13-918f-0065a1550b7c");
            // put("secret", "b1c063dd-1a2a-464f-8a9f-7fd2fac74a23");
        }
    };;
    private Configuration config = new Configuration("https://k152.maxgigapop.net:8543/auth", "StackV", "StackV", cred,
            null);
    AuthzClient keycloakClient = AuthzClient.create(config);

    private final StackLogger logger = new StackLogger("net.stackv.rest.WebResource", "TokenHandler");
    private final String kc_url = System.getProperty("kc_url");
    private String kc_encode = System.getProperty("kc_encode");

    private String kc_realm = "StackV";
    private String auth = "Basic " + kc_encode;
    private String durl = kc_url + "/realms/" + kc_realm + "/protocol/openid-connect/token";

    private final OkHttpClient httpClient;
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
        if (kc_encode == null) {
            kc_encode = "U3RhY2tWOmFlNTNmYmVhLTg4MTItNGMxMy05MThmLTAwNjVhMTU1MGI3Yw==";
            auth = "Basic " + kc_encode;
        }
        refreshToken = refresh;
        requestData = "grant_type=refresh_token&refresh_token=" + refreshToken;

        httpClient = getClient();
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

        httpClient = getClient();
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

            try (ResponseBody response = httpClient.newCall(request).execute().body()) {
                JSONObject ret = (JSONObject) parser.parse(response.string());

                accessToken = (String) ret.get("access_token");
                accessCreationTime = System.nanoTime();

                refreshToken = (String) ret.get("refresh_token");
                requestData = "grant_type=refresh_token&refresh_token=" + refreshToken;
            }

            if (recur != 0) {
                logger.status(method, "Refresh achieved after " + recur + " retry");
            }
        } catch (ParseException | IOException ex) {
            // Keycloak connection timeout
            logger.catching(method, ex);
            try {
                recur++;
                logger.warning(method, "Keycloak refresh timeout #" + recur);
                Thread.sleep(3000);
                refreshTokenSub(recur);
            } catch (InterruptedException ex1) {
                logger.catching(method, ex1);
            }
        }
    }

    private OkHttpClient getClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            } };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient okHttpClient = new OkHttpClient();
            okHttpClient.setSslSocketFactory(sslSocketFactory);
            okHttpClient.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
