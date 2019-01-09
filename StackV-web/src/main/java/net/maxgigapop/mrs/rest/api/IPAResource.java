/*
* Copyright (c) 2013-2018 University of Maryland
* Created by: Antonio Heard 2018
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
package net.maxgigapop.mrs.rest.api;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import javax.ejb.EJB;
import javax.ws.rs.core.Context;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import net.maxgigapop.mrs.system.HandleSystemCall;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
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
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;
import net.maxgigapop.mrs.common.IPATool;
import net.maxgigapop.mrs.common.StackLogger;
import org.jboss.resteasy.spi.HttpRequest;

/**
 * REST Web Service
 *
 * @author arheard
 */
@Path("md2")
public class IPAResource {

    private static final StackLogger logger = new StackLogger(IPAResource.class.getName(), "IPAResource");
    static String host = "http://127.0.0.1:8080/StackV-web/restapi";
    static JSONParser parser = new JSONParser();
    static OkHttpClient client = new OkHttpClient();

    static String kc_url, ipaBaseServerUrl, ipaUsername, ipaPasswd, ipaCookie;

    @EJB
    HandleSystemCall systemCallHandler;

    /**
     * Creates a new instance of WebResource
     */
    public IPAResource() {
    }

    static void loadConfig() {
        try {
            URL url = new URL(String.format("%s/config/", host));
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();
            String responseStr = response.body().string();

            Object obj = parser.parse(responseStr);
            JSONObject props = (JSONObject) obj;

            kc_url = (String) props.get("system.keycloak");
            logger.status("loadConfig", "global variable loaded - kc_url:" + props.get("system.keycloak"));
            ipaBaseServerUrl = (String) props.get("ipa.server");
            logger.status("loadConfig", "global variable loaded - ipaBaseServerUrl:" + props.get("ipa.server"));
            ipaUsername = (String) props.get("ipa.username");
            logger.status("loadConfig", "global variable loaded - ipaUsername:" + props.get("ipa.username"));
            ipaPasswd = (String) props.get("ipa.password");
            logger.status("loadConfig", "global variable loaded - ipaPasswd:" + props.get("ipa.password"));

        } catch (IOException | ParseException ex) {
            logger.throwing("loadConfig", ex);
        }
    }

    @PUT
    @Path("/reload/")
    public static void reloadConfig() {
        WebResource.loadConfig();
        IPAResource.loadConfig();
        return;
    }

    static {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
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

            }
        };

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
            // set the  allTrusting verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (KeyManagementException | NoSuchAlgorithmException ex) {

        }
    }

    // >FreeIPA-based ACL
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
//                conn.setSSLSocketFactory();
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

                    // get the ipa_session cookie from the returned header fields and assign it to ipaCookie
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
                logger.catching("ipaLogin", ex);
            } catch (IOException ex) {
                logger.catching("ipaLogin", ex);
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

                //JSONObject postDataJson = (JSONObject) parser.parse(postData);
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
                logger.catching("ipaRequest", ex);
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
            logger.error("ipaEndpoint", "IPA Request Failed. Exception: " + ex);
            return null;
        }
    }

    @GET
    @Path("driver/default/{topuri}")
    @Produces("application/json")
    public String getDriverDefault(@PathParam("topuri") String topologyURI) {
        String postData;
        JSONObject paramsJSON = new JSONObject();
        paramsJSON.put("topologyuri", topologyURI);
        postData = buildIpaRequest("md2_get_driver_default", null, paramsJSON);
        return ipaEndpoint(postData);
    }

    @GET
    @Path("driver/available")
    @Produces("application/json")
    public String getAvailableDrivers() {
        String postData;
        postData = buildIpaRequest("md2_get_available_drivers", null, null);
        return ipaEndpoint(postData);
    }

    /*
      Takes domain URI  url encoded string
      Takes path as url encoded string  
     */
    @GET
    @Path("directory/{domainuri}/{path}")
    @Produces("application/json")
    public String getDataByDirectory(@PathParam("domainuri") String domain, @PathParam("path") String path) {
        String postData;
        JSONObject paramsJSON = new JSONObject();
        paramsJSON.put("directory", path);
        paramsJSON.put("domaintopouri", domain);
        postData = buildIpaRequest("md2_get_data_by_directory", null, paramsJSON);
        return ipaEndpoint(postData);
    }

    /*
      Takes type as optional query parameter  
     */
    @GET
    @Path("alm/pools")
    @Produces("application/json")
    public String getAlmPoolList(@QueryParam("type") String type) {
        String postData;
        JSONObject paramsJSON = new JSONObject();
        paramsJSON.put("pooltype", type);
        postData = buildIpaRequest("almpool_list", null, paramsJSON);
        return ipaEndpoint(postData);
    }

    @POST
    @Path("inject")
    @Consumes("application/json")
    @Produces("application/json")
    public String injectEntry(String entryJson) {
        JSONObject paramsJSON = new JSONObject();
        paramsJSON.put("entryjson", entryJson);
        String postData = buildIpaRequest("md2_add_entry", null, paramsJSON);
        return ipaEndpoint(postData);
    }

    private String buildIpaRequest(String method, JSONArray arguments, JSONObject params) {
        JSONObject requestJSON = new JSONObject();

        requestJSON.put("method", method);

        JSONArray paramsArray = new JSONArray();
        if (arguments == null) {
            arguments = new JSONArray();
        }

        if (params == null) {
            params = new JSONObject();
        }

        paramsArray.add(arguments);
        paramsArray.add(params);

        requestJSON.put("params", paramsArray);
        requestJSON.put("id", 0);
        return requestJSON.toJSONString();
    }

    /**
     * Removing general JSON-RPC response information
     * to return enclosed data
     */
    private String removeRPCResponseData(String responseString) throws ParseException {
        JSONObject responseObj;
        responseObj = (JSONObject) parser.parse(responseString);

        JSONObject md2DataJSON = (JSONObject) responseObj.get("result");
        if (md2DataJSON != null) {
            while (md2DataJSON.get("result") != null) {
                md2DataJSON = (JSONObject) md2DataJSON.get("result");
            }
            return md2DataJSON.toJSONString();
        } else {
            return responseObj.toJSONString();
        }
    }

    // SERVICE FUNCTIONS //   
    @DELETE
    @Path("/servicepolicies/{serviceUUID}")
    @Produces("application/json")
    public String ipaDeleteAllPoliciesForService(@PathParam("serviceUUID") String uuid, String data) throws UnsupportedEncodingException {

        ipaLogin(); // ensure the ipa server cookie has been refreshed in case it was expired.
        JSONObject result = new JSONObject();

        // formatting all the groups and rules names
        String loginUg = "ug-login-" + uuid;
        String loginHg = "hg-login-" + uuid;
        String loginHbac = "hbac-login-" + uuid;
        String sudoUg = "ug-sudo-" + uuid;
        String sudoHg = "hg-sudo-" + uuid;
        String sudoHbac = "hbac-sudo-" + uuid;

        result.put("ServiceUUID", uuid);

        JSONObject delLoginUsergroup = formatIpaDeleteJSON("group_del", loginUg);
        JSONObject delLoginHostgroup = formatIpaDeleteJSON("hostgroup_del", loginHg);
        JSONObject delLoginHbacrule = formatIpaDeleteJSON("hbacrule_del", loginHbac);

        JSONObject delSudoUsergroup = formatIpaDeleteJSON("group_del", sudoUg);
        JSONObject delSudoHostgroup = formatIpaDeleteJSON("hostgroup_del", sudoHg);
        JSONObject delSudoHbacrule = formatIpaDeleteJSON("hbacrule_del", sudoHbac);

        /**
         * NOTE: if the groups or rules have not been created, there will an error
         * in the resulting JSON but it is not fatal. The error will indicate
         * the group or rule does not exist which is okay since some accesses
         * will not be given (mainly sudo accesses).
         */
        result.put("loginUgDel", ipaRequest(delLoginUsergroup.toJSONString()));
        result.put("loginHgDel", ipaRequest(delLoginHostgroup.toJSONString()));
        result.put("loginHbacDel", ipaRequest(delLoginHbacrule.toJSONString()));

        result.put("sudoUgDel", ipaRequest(delSudoUsergroup.toJSONString()));
        result.put("sudoHgDel", ipaRequest(delSudoHostgroup.toJSONString()));
        result.put("sudoHbacDel", ipaRequest(delSudoHbacrule.toJSONString()));

        // return the JSONObject as a string
        return result.toJSONString();
    }

    /**
     * Create the delete JSON for deleting a user group, host group, or HBAC rule.
     * All three have the same format.
     *
     * var ipaRequestData = {
     * "method": method,
     * "params":[
     * rule/group name,
     * {}
     * ],
     * "id":0
     * };
     * @param method
     * @param name
     * @return
     */
    private JSONObject formatIpaDeleteJSON(String method, String name) {
        JSONObject deleteJSON = new JSONObject();

        deleteJSON.put("method", method);

        JSONArray paramsArray = new JSONArray();
        JSONArray nameParam = new JSONArray();
        nameParam.add(name);
        paramsArray.add(nameParam);
        paramsArray.add(new JSONObject());

        deleteJSON.put("params", paramsArray);
        deleteJSON.put("id", 0);
        return deleteJSON;
    }
}
