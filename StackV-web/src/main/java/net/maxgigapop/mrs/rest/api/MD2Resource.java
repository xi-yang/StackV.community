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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.EJB;
import javax.naming.AuthenticationException;
import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import net.maxgigapop.mrs.common.MD2Connect;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.system.HandleSystemCall;

@Path("md2")
public class MD2Resource {
    private static final StackLogger logger = new StackLogger(WebResource.class.getName(), "MD2Resource");
    private static MD2Connect conn;
    private static JSONParser parser = new JSONParser();
    private static String host = "http://127.0.0.1:8080/StackV-web/restapi";
    private static final OkHttpClient client = new OkHttpClient();
    private static String serverName;
    private static String ipaBaseServerUrl, ipaUsername, ipaPass;

    private static boolean online = true;

    @EJB
    HandleSystemCall systemCallHandler;

    static void loadConfig() {
        try {
            logger.trace_start("loadConfig");
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
            online = conn.validate();
        } catch (IOException | ParseException ex) {
            throw logger.throwing("loadConfig", ex);
        }
    }

    static {
        loadConfig();

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

    @PUT
    @Path("/reload")
    public static void reloadConfig() {
        loadConfig();
    }

    //
    // Registration
    //
    @GET
    @Path("/register")
    public String checkOrchestrator() throws AuthenticationException, InvalidNameException {
        if (!online) {
            return "Resource offline. Verify server URL and credentials";
        }

        return (conn.get("cn=" + serverName + ",cn=orchestrators,cn=stackv") == null ? "false" : "true");
    }

    @POST
    @Path("/register")
    public String registerOrchestrator() throws AuthenticationException, InvalidNameException {
        if (!online) {
            return "Resource offline. Verify server URL and credentials";
        }

        String method = "registerOrchestrator";
        logger.trace_start(method);
        // Orchestrator entry
        HashMap<String, String[]> map = new HashMap<>();
        map.put("cn", new String[] { "cn=" + serverName + ",cn=orchestrators,cn=stackv" });
        map.put("objectclass", new String[] { "top", "dckContainer" });
        conn.add(map);
        // Services subentry
        map.put("cn", new String[] { "cn=services,cn=" + serverName + ",cn=orchestrators,cn=stackv" });
        conn.add(map);
        // Domains subentry
        map.put("cn", new String[] { "cn=domains,cn=" + serverName + ",cn=orchestrators,cn=stackv" });
        conn.add(map);

        logger.end(method);
        return "cn=" + serverName + ",cn=orchestrators,cn=stackv";
    }

    @DELETE
    @Path("/register")
    public String deregisterOrchestrator() throws AuthenticationException, InvalidNameException {
        if (!online) {
            return "Resource offline. Verify server URL and credentials";
        }

        String method = "deregisterOrchestrator";
        logger.trace_start(method);
        conn.remove("cn=" + serverName + ",cn=orchestrators,cn=stackv");

        logger.end(method);
        return "cn=" + serverName + ",cn=orchestrators,cn=stackv";
    }

    // Drivers
    @POST
    @Path("/register/drivers/{driver}")
    public String registerDriver(@PathParam("driver") String driver)
            throws AuthenticationException, InvalidNameException {
        if (!online) {
            return "Resource offline. Verify server URL and credentials";
        }

        String method = "registerDriver";
        logger.trace_start(method);
        HashMap<String, String[]> map = new HashMap<>();
        map.put("cn", new String[] { "cn=" + driver + ",cn=domains,cn=dck,cn=stackv" });
        map.put("objectclass", new String[] { "top", "dckContainer" });
        conn.add(map);

        // Automation subentry
        map.put("cn", new String[] { "cn=automation,cn=" + driver + ",cn=domains,cn=dck,cn=stackv" });
        conn.add(map);
        map.put("cn", new String[] { "cn=devices,cn=automation,cn=" + driver + ",cn=domains,cn=dck,cn=stackv" });
        conn.add(map);
        map.put("cn", new String[] { "cn=networks,cn=automation,cn=" + driver + ",cn=domains,cn=dck,cn=stackv" });
        conn.add(map);
        map.put("cn", new String[] { "cn=servers,cn=automation,cn=" + driver + ",cn=domains,cn=dck,cn=stackv" });
        conn.add(map);

        // Configuration subentry
        map.put("cn", new String[] { "cn=configuration,cn=" + driver + ",cn=domains,cn=dck,cn=stackv" });
        conn.add(map);
        map.put("cn", new String[] { "cn=cloud,cn=configuration,cn=" + driver + ",cn=domains,cn=dck,cn=stackv" });
        conn.add(map);
        map.put("cn",
                new String[] { "cn=network,cn=cloud,cn=configuration,cn=" + driver + ",cn=domains,cn=dck,cn=stackv" });
        conn.add(map);
        map.put("cn",
                new String[] { "cn=server,cn=cloud,cn=configuration,cn=" + driver + ",cn=domains,cn=dck,cn=stackv" });
        conn.add(map);
        map.put("cn", new String[] { "cn=credentials,cn=configuration,cn=" + driver + ",cn=domains,cn=dck,cn=stackv" });
        conn.add(map);
        map.put("cn", new String[] { "cn=default,cn=configuration,cn=" + driver + ",cn=domains,cn=dck,cn=stackv" });
        conn.add(map);

        logger.end(method);
        return "cn=" + driver + ",cn=domains,cn=dck,cn=stackv";
    }

    @DELETE
    @Path("/register/drivers/{driver}")
    public String deregisterDriver(@PathParam("driver") String driver)
            throws AuthenticationException, InvalidNameException {
        if (!online) {
            return "Resource offline. Verify server URL and credentials";
        }

        String method = "deregisterDriver";
        logger.trace_start(method);
        conn.remove("cn=" + driver + ",cn=domains,cn=dck,cn=stackv");

        logger.end(method);
        return "cn=" + driver + ",cn=domains,cn=dck,cn=stackv";
    }

    // Services
    @POST
    @Path("/register/services/{service}")
    public String registerService(@PathParam("service") String service)
            throws AuthenticationException, InvalidNameException {
        if (!online) {
            return "Resource offline. Verify server URL and credentials";
        }

        String method = "registerService";
        logger.trace_start(method);
        HashMap<String, String[]> map = new HashMap<>();
        map.put("cn",
                new String[] { "cn=" + service + ",cn=services,cn=" + serverName + ",cn=orchestrators,cn=stackv" });
        map.put("objectclass", new String[] { "top", "dckConfig" });

        conn.add(map);
        logger.end(method);
        return "cn=" + service + ",cn=services,cn=" + serverName + ",cn=orchestrators,cn=stackv";
    }

    @DELETE
    @Path("/register/services/{service}")
    public String deregisterService(@PathParam("service") String service)
            throws AuthenticationException, InvalidNameException {
        if (!online) {
            return "Resource offline. Verify server URL and credentials";
        }

        String method = "deregisterService";
        logger.trace_start(method);
        conn.remove("cn=" + service + ",cn=services,cn=" + serverName + ",cn=orchestrators,cn=stackv");

        logger.end(method);
        return "cn=" + service + ",cn=services,cn=" + serverName + ",cn=orchestrators,cn=stackv";
    }

    //
    // Queries
    //
    @GET
    @Path("/query/drivers")
    public String driverQuery() throws AuthenticationException, InvalidNameException {
        return conn.search("cn=domains,cn=dck,cn=stackv");
    }

    @GET
    @Path("/query/services/{domain}")
    public String serviceQuery(@PathParam("domain") String domain)
            throws AuthenticationException, InvalidNameException {
        return conn.search("cn=services,cn=" + serverName + ",cn=orchestrators,cn=stackv");
    }

    @GET
    @Path("/query/search/{dn}")
    public String searchQuery(@PathParam("dn") String filter) throws AuthenticationException, InvalidNameException {
        return conn.search(filter);
    }

    @GET
    @Path("/query/attr/{dn}")
    public String attrQuery(@PathParam("dn") String filter) throws NamingException {
        Map<String, ArrayList<Object>> retMap = new HashMap<>();
        Attributes attrs = conn.get(filter);
        NamingEnumeration<? extends Attribute> en = attrs.getAll();
        while (en.hasMore()) {
            Attribute attr = en.next();
            NamingEnumeration<?> val = attr.getAll();
            ArrayList<Object> arr = new ArrayList<>();
            while (val.hasMore()) {
                arr.add(val.next());
            }

            retMap.put(attr.getID(), arr);
        }
        return JSONObject.toJSONString(retMap);
    }
}
