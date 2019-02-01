/*
* Copyright (c) 2013-2018 University of Maryland
* Created by: Antonio Heard 2018
* Edited by: SaiArvind Ganganapalle 2018
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
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * IPA tool to interact with FreeIPA. Derived from Antonio Heard's
 * IPAResource.java (located in StackV-web net.maxgigapop.mrs.rest.api). Antonio
 * strips the RPC headers, but I do not as Alm.java uses those headers to verify
 * the success/failure of the request
 *
 * @author saiarvind
 */
public class IPATool {

    private final StackLogger logger = new StackLogger(IPATool.class.getName(), "IPATool");
    private final String host = "http://127.0.0.1:8080/StackV-web/restapi";

    JSONParser parser = new JSONParser();
    OkHttpClient client = new OkHttpClient();
    String ipaBaseServerUrl, ipaBaseDomain, ipaUsername, ipaPasswd, ipaCookie;

    public IPATool() {
        loadConfig();
    }

    public void loadConfig() {
        try {
            URL url = new URL(String.format("%s/config/", host));
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();
            String responseStr = response.body().string();

            Object obj = parser.parse(responseStr);
            JSONObject props = (JSONObject) obj;

            ipaBaseServerUrl = (String) props.get("ipa.server");
            logger.status("loadConfig", "global variable loaded - ipaBaseServerUrl:" + props.get("ipa.server"));
            ipaBaseDomain = (String) props.get("ipa.domain");
            logger.status("loadConfig", "global variable loaded - ipaBaseServerUrl:" + props.get("ipa.domain"));
            ipaUsername = (String) props.get("ipa.username");
            logger.status("loadConfig", "global variable loaded - ipaUsername:" + props.get("ipa.username"));
            ipaPasswd = (String) props.get("ipa.password");
            logger.status("loadConfig", "global variable loaded - ipaPasswd:" + props.get("ipa.password"));

        } catch (IOException | ParseException ex) {
            logger.throwing("loadConfig", ex);
        }
    }

    static {
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }

        } };

        SSLContext sc;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            // set the allTrusting verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (KeyManagementException | NoSuchAlgorithmException ex) {

        }
    }

    public String ipaLogin() throws UnsupportedEncodingException {
        if (ipaUsername != null && ipaPasswd != null) {

            JSONObject result = new JSONObject();

            String formattedLoginData = "user=" + ipaUsername + "&password=" + ipaPasswd;

            try {

                URL ipaurl = new URL(ipaBaseServerUrl + "/ipa/session/login_password");
                HttpsURLConnection conn = (HttpsURLConnection) ipaurl.openConnection();
                conn.setRequestProperty("referrer", ipaBaseServerUrl + "/ipa");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Accept", "text/plain");
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                // conn.setSSLSocketFactory();
                DataOutputStream wr = new DataOutputStream((conn.getOutputStream()));
                wr.writeBytes(formattedLoginData);
                wr.flush();
                conn.connect();

                // if the request is successful
                if (200 <= conn.getResponseCode() && conn.getResponseCode() <= 299) {
                    result.put("Result", "Login Successful");
                    result.put("ResponseCode", conn.getResponseCode());
                    result.put("ResponseMessage", conn.getResponseMessage());
                    result.put("LoginSuccess", true);

                    // get the ipa_session cookie from the returned header fields and assign it to
                    // ipaCookie
                    ipaCookie = conn.getHeaderFields().get("Set-Cookie").get(0);
                    logger.trace("ipaLogin", "Successfully logged into IPA Server");
                } else { // if the request fails
                    String errorStream = "";
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            errorStream += inputLine;
                        }
                    }
                    result.put("Result", "Login Unsuccessful");
                    result.put("ResponseCode", conn.getResponseCode());
                    result.put("ResponseMessage", conn.getResponseMessage());
                    result.put("LoginSuccess", false);
                    result.put("Error", errorStream);
                    logger.warning("ipaLogin", "Could not login to IPA Server");
                }

            } catch (MalformedURLException ex) {
                Logger.getLogger(IPATool.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(IPATool.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }

            // return the JSON object as a string
            return result.toJSONString();
        } else {
            throw logger.error_throwing("ipaLogin()", "IPA username or password not set");
        }
    }

    public String ipaRequest(String postData) {
        if (ipaBaseServerUrl != null) {

            JSONObject result = new JSONObject();
            try {
                URL ipaurl = new URL(ipaBaseServerUrl + "/ipa/session/json");
                HttpsURLConnection conn = (HttpsURLConnection) ipaurl.openConnection();
                conn.setRequestProperty("referer", ipaBaseServerUrl + "/ipa");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Cookie", ipaCookie);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                // JSONObject postDataJson = (JSONObject) parser.parse(postData);
                DataOutputStream wr = new DataOutputStream((conn.getOutputStream()));
                wr.writeBytes(postData);
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
                    ipaCookie = conn.getHeaderFields().get("Set-Cookie").get(0);
                    result = (JSONObject) parser.parse(responseStr.toString());
                } else { // if the request fails
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                        String inputLine;
                        responseStr = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            responseStr.append(inputLine);
                        }
                    }
                    result.put("Error", responseStr.toString());
                }
            } catch (IOException | ParseException ex) {
                Logger.getLogger(IPATool.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }

            // return the JSONObject as a string
            return result.toJSONString();
        } else {
            throw logger.error_throwing("ipaRequest()", "IPA server url not set");
        }
    }

    public String ipaEndpoint(String postData) {
        try {
            ipaLogin();
            return ipaRequest(postData);
        } catch (Exception ex) {
            Logger.getLogger(IPATool.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            logger.error("ipaEndpoint", "IPA Request Failed. Exception: " + ex);
            return null;
        }
    }

}
