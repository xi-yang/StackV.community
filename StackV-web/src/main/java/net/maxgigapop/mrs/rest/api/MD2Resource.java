/*
* Copyright (c) 2013-2019 University of Maryland
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
package net.maxgigapop.mrs.rest.api;

import java.io.IOException;
import java.net.URL;
import javax.ejb.EJB;
import javax.ws.rs.Path;
import net.maxgigapop.mrs.system.HandleSystemCall;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import net.maxgigapop.mrs.common.StackLogger;
import org.jboss.resteasy.spi.HttpRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import net.maxgigapop.mrs.common.MD2Connect;

@Path("md2")
public class MD2Resource {
    private static final StackLogger logger = new StackLogger(WebResource.class.getName(), "MD2Resource");
    private static MD2Connect conn;
    private static JSONParser parser = new JSONParser();    
    private static String host = "http://127.0.0.1:8080/StackV-web/restapi";    
    private static final OkHttpClient client = new OkHttpClient();
    private static String serverName;
    private static String ipaBaseServerUrl, ipaUsername, ipaPass;

    @javax.ws.rs.core.Context
    private HttpRequest httpRequest;

    @EJB
    HandleSystemCall systemCallHandler;

    /**
     * Creates a new instance of WebResource
     */
    public MD2Resource() {              
    }

    static void loadConfig() {
         try {
            URL url = new URL(String.format("%s/config/", host));
            Request request = new Request.Builder().url(url).build();
            Response response = client.newCall(request).execute();
            String responseStr = response.body().string();

            Object obj = parser.parse(responseStr);
            JSONObject props = (JSONObject) obj;

            ipaBaseServerUrl = (String) props.get("ipa.server");
            logger.status("loadConfig", "global variable loaded - ipaBaseServerUrl:" + ipaBaseServerUrl);
            ipaUsername = (String) props.get("ipa.username");
            logger.status("loadConfig", "global variable loaded - ipaUsername:" + ipaUsername);
            ipaPass = (String) props.get("ipa.password");
            logger.status("loadConfig", "global variable loaded - ipaPasswd:" + ipaPass);
            serverName = (String) props.get("system.name");
            logger.status("loadConfig", "global variable loaded - serverName:" + serverName);

            conn = new MD2Connect(ipaBaseServerUrl, ipaUsername, ipaPass);
        } catch (IOException | ParseException ex) {
            throw logger.throwing("loadConfig", ex);
        }
    }
    static {
        loadConfig();
        
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
    @PUT
    @Path("/reload")
    public static void reloadConfig() {
        MD2Resource.loadConfig();
    }

    @PUT
    @Path("/register")
    public void registerOrchestrator(final String inputString) {
        System.out.println(serverName);
        System.out.println("cn=" + serverName + ",cn=orchestrators,cn=stackv");
        
        HashMap<String,String[]> map = new HashMap<>();                
        map.put("cn", new String[]{"cn=" + serverName + ",cn=orchestrators,cn=stackv"});
        map.put("objectclass", new String[]{"top", "dckConfig"});               
        
        conn.add(map);
    }
    
    @DELETE
    @Path("/register")
    public void deregisterOrchestrator(final String inputString) {        
        conn.remove("cn=" + serverName + ",cn=orchestrators,cn=stackv");
    }
}