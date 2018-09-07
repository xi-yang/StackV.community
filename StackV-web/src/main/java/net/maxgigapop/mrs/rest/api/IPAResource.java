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
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ws.rs.core.Context;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import net.maxgigapop.mrs.system.HandleSystemCall;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import com.hp.hpl.jena.ontology.OntModel;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.RequestBody;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.QueryParam;
import net.maxgigapop.mrs.common.AuditService;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import net.maxgigapop.mrs.common.IpaAlm;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.service.ServiceHandler;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.common.TokenHandler;
import net.maxgigapop.mrs.service.ServiceEngine;
import net.maxgigapop.mrs.service.VerificationHandler;
import org.apache.commons.dbutils.DbUtils;
import org.apache.logging.log4j.Level;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.apache.logging.log4j.core.config.Configurator;
import org.jboss.resteasy.spi.UnhandledException;
import templateengine.TemplateEngine;

/**
 * REST Web Service
 *
 * @author arheard
 */
@Path("md2")
public class IPAResource {
    private final StackLogger logger = new StackLogger(IPAResource.class.getName(), "IPAResource");

    String host = "http://127.0.0.1:8080/StackV-web/restapi";
    String kc_url = System.getProperty("kc_url");
    JSONParser parser = new JSONParser();

    private static final ExecutorService executorService = java.util.concurrent.Executors.newCachedThreadPool();
    private final OkHttpClient client = new OkHttpClient();

    private final String keycloakStackVClientID = "5c0fab65-4577-4747-ad42-59e34061390b";

    String ipaBaseServerUrl = System.getProperty("ipa_url");
    String ipaUsername = System.getProperty("ipa_username");
    String ipaPasswd = System.getProperty("ipa_passwd");
    static String ipaCookie;


    @Context
    private HttpRequest httpRequest;

    @EJB
    HandleSystemCall systemCallHandler;

    /**
     * Creates a new instance of WebResource
     */
    public IPAResource() {
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
    @POST
    @Path("login")
    @Produces("application/json")
    //@RolesAllowed("F_ACL-R")
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
                DataOutputStream wr = new DataOutputStream((conn.getOutputStream()));
                wr.writeBytes(formattedLoginData);
                wr.flush();
                conn.connect();

                // if the request is successful
                if (200 <= conn.getResponseCode() && conn.getResponseCode() <= 299) {
                    result.put("Result", "Login Successful");
                    result.put("ResponseCode", conn.getResponseCode());
                    result.put("ResponseMessage", conn.getResponseMessage());

                    // get the ipa_session cookie from the returned header fields and assign it to ipaCookie
                    ipaCookie = conn.getHeaderFields().get("Set-Cookie").get(0);
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
                    result.put("Error", errorStream);
                }

            } catch (MalformedURLException ex) {
                Logger.getLogger(IPAResource.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(IPAResource.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }

            // return the JSON object as a string
            return result.toJSONString();
        } else {
            throw logger.error_throwing("ipaLogin()", "IPA username or password not set");
        }
    }    
    
    @POST
    @Consumes("application/json")
    @Produces("application/json")
    //@RolesAllowed("F_ACL-R")
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
                Logger.getLogger(IPAResource.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }

            // return the JSONObject as a string
            return result.toJSONString();
        } else {
            throw logger.error_throwing("ipaRequest()", "IPA server url not set");
        }
    }    
    
    @POST
    @Path("/request")
    @Consumes("application/json")
    @Produces("application/json")
    public String ipaEndpoint(String postData) {
        try { 
            ipaLogin();
            String response = ipaRequest(postData);
            response = removeRPCResponseData(response);
            return response;
        } catch (Exception ex) {
            Logger.getLogger(IPAResource.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
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
         postData = buildIpaRequest("md2_get_driver_default", null, paramsJSON );
         return ipaEndpoint(postData);
    }    
    
    @GET
    @Path("driver/available")
    @Produces("application/json")
    public String getAvailableDrivers() {
         String postData;
         postData = buildIpaRequest("md2_get_available_drivers", null, null );
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
         postData = buildIpaRequest("md2_get_data_by_directory", null, paramsJSON );
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
         postData = buildIpaRequest("almpool_list", null, paramsJSON );
         return ipaEndpoint(postData);
    }    

    @POST
    @Path("inject")
    @Consumes("application/json")
    @Produces("application/json")
    public String injectEntry(String entryJson) {
         JSONObject paramsJSON = new JSONObject();
         paramsJSON.put("entryjson", entryJson);
         String postData = buildIpaRequest("md2_add_entry", null, paramsJSON );
         return ipaEndpoint(postData);        
    }
    
    private String buildIpaRequest(String method, JSONArray arguments, JSONObject params ) {
        JSONObject requestJSON = new JSONObject();

        requestJSON.put("method", method);

        JSONArray paramsArray = new JSONArray();
        if (arguments == null)
           arguments = new JSONArray();

        if (params == null)
           params = new JSONObject();

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
}