/*
* Copyright (c) 2013-2019 University of Maryland
* Created by: Alberto Jimenez 2015
* Modified by: Tao-Hung Yang 2016
* Modified by: Xi Yang 2016
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.jboss.resteasy.spi.HttpRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.system.HandleSystemCall;

/**
 * REST Web Service
 *
 * @author rikenavadur
 */
@Path("auth")
public class KeycloakResource {
    private static final StackLogger logger = new StackLogger(WebResource.class.getName(), "KeycloakResource");
    private static String host = "http://127.0.0.1:8080/StackV-web/restapi";
    private static JSONParser parser = new JSONParser();

    private static String kc_url;

    private static final ExecutorService executorService = java.util.concurrent.Executors.newCachedThreadPool();
    private static final OkHttpClient client = new OkHttpClient();

    private final JNDIFactory factory = new JNDIFactory();

    @Context
    private HttpRequest httpRequest;

    @EJB
    HandleSystemCall systemCallHandler;

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
        } catch (IOException | ParseException ex) {
            logger.throwing("loadConfig", ex);
        }
    }

    @PUT
    @Path("/reload/")
    public static void reloadConfigs() {
        KeycloakResource.loadConfig();
    }

    static {
        KeycloakResource.loadConfig();

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

    // >Access Control
    @GET
    @Path("/access/{uuid}")
    @Produces("application/json")
    public String getUserAccess(@PathParam(value = "uuid") String instance) throws IOException, ParseException {
        String method = "getUserAccess";
        JSONObject retJSON = new JSONObject();
        JSONArray retArr = new JSONArray();

        KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest
                .getAttribute(KeycloakSecurityContext.class.getName());
        AccessToken accessToken = securityContext.getToken();
        String username = accessToken.getPreferredUsername();

        try (Connection front_conn = factory.getConnection("frontend");
                PreparedStatement prep = front_conn
                        .prepareStatement("SELECT `subject` FROM `acl` WHERE `object` = ?");) {
            prep.setString(1, instance);
            try (ResultSet rs = prep.executeQuery();) {
                final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
                URL url = new URL(kc_url + "/admin/realms/StackV/users");
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", auth);
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
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
                JSONArray userArr = (JSONArray) obj;
                for (Object user : userArr) {
                    JSONObject userJSON = (JSONObject) user;
                    rs.beforeFirst();
                    while (rs.next()) {
                        if (rs.getString("subject").equals(userJSON.get("username"))) {
                            userJSON.put("permitted", true);
                        }
                    }
                    if (!userJSON.get("username").equals(username)) {
                        retArr.add(userJSON);
                    }
                }
                retJSON.put("data", retArr);
                return retJSON.toJSONString();
            }
        } catch (IOException | SQLException | ParseException ex) {
            logger.catching(method, ex);
            return retJSON.toJSONString();
        }
    }

    /**
     * Gives a user access to specified instance.
     * 
     * @param uuid
     * @param username
     * @throws SQLException
     */
    @PUT
    @Path("/access/{uuid}/{username}")
    @Consumes(value = { "application/json" })
    public void addAccess(@PathParam("uuid") String uuid, @PathParam("username") String username) throws SQLException {
        try (Connection front_conn = factory.getConnection("frontend");
                PreparedStatement prep = front_conn
                        .prepareStatement("INSERT INTO acl (`subject`, `object`) VALUES (?, ?)");) {
            // excluding the topuri if the driver type is raw
            prep.setString(1, username);
            prep.setString(2, uuid);
            prep.executeUpdate();
        } catch (SQLException ex) {
            logger.catching("addAccess", ex);
            throw ex;
        }
    }

    /**
     * Removes a user's access to specified instance.
     * 
     * @param uuid
     * @param username
     * @throws SQLException
     */
    @DELETE
    @Path("/access/{uuid}/{username}")
    @Consumes(value = { "application/json" })
    public void removeAccess(@PathParam("uuid") String uuid, @PathParam("username") String username)
            throws SQLException {
        try (Connection front_conn = factory.getConnection("frontend");
                PreparedStatement prep = front_conn
                        .prepareStatement("DELETE FROM acl WHERE `subject` = ? AND `object` = ?");) {
            // excluding the topuri if the driver type is raw
            prep.setString(1, username);
            prep.setString(2, uuid);
            prep.executeUpdate();
        } catch (SQLException ex) {
            logger.catching("removeAccess", ex);
            throw ex;
        }
    }

    @GET
    @Path("/access/ipa/{uuid}")
    @Produces("application/json")
    public String getUserResourceAccess(@PathParam(value = "uuid") String instance) throws IOException, ParseException {
        String method = "getUserResourceAccess";
        JSONObject retJSON = new JSONObject();
        JSONArray retArr = new JSONArray();

        final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
        URL url = new URL(kc_url + "/admin/realms/StackV/users");
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", auth);
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
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
        JSONArray userArr = (JSONArray) obj;
        for (Object user : userArr) {
            JSONObject userJSON = (JSONObject) user;
            retJSON = new JSONObject();
            retJSON.put("username", userJSON.get("username"));

            retArr.add(userJSON);
        }
        retJSON.put("data", retArr);
        logger.trace_end(method);
        return retJSON.toJSONString();
    }

    // >Data
    /**
     * @api {get} /app/keycloak/users Get Users
     * @apiVersion 1.0.0
     * @apiDescription Get a list of existing users.
     * @apiGroup Keycloak
     * @apiUse AuthHeader
     *
     * @apiExample {curl} Example Call: curl
     *             http://localhost:8080/StackV-web/restapi/app/keycloak/users -H
     *             "Authorization: bearer $KC_ACCESS_TOKEN"
     *
     * @apiSuccess {JSONArray} users users JSON
     * @apiSuccess {JSONArray} users.user user JSON
     * @apiSuccess {String} users.user.username username
     * @apiSuccess {String} users.user.name full name
     * @apiSuccess {String} users.user.email email
     * @apiSuccess {String} users.user.time timestamp of user creation
     * @apiSuccess {String} users.user.subject user ID
     * @apiSuccessExample {json} Example Response:
     *                    [["admin","",null,"1475506393070","1d183570-2798-4d69-80c3-490f926596ff"],["username","","email","1475506797561","1323ff3d-49f3-46ad-8313-53fd4c711ec6"]]
     */
    @GET
    @Path("/data/users")
    @Produces("application/json")
    public String getUsers() throws IOException, ParseException {
        try {
            JSONObject retJSON = new JSONObject();
            JSONArray retArr = new JSONArray();

            String method = "getUsers";
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL(kc_url + "/admin/realms/StackV/users");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", auth);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
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
            JSONArray userArr = (JSONArray) obj;
            for (Object user : userArr) {
                JSONObject userJSON = (JSONObject) user;
                retArr.add(userJSON);
            }
            retJSON.put("data", retArr);
            logger.trace_end(method);
            return retJSON.toJSONString();
        } catch (IOException | ParseException ex) {
            logger.catching("getUsers", ex);
            throw ex;
        }
    }

    // DEPRECATED
    // >Keycloak
    /* Andrew's Draft for new post method for adding additional roles to groups */
    @POST
    @Path("/keycloak/groups/{group}")
    @Produces("application/json")
    @RolesAllowed("F_Keycloak-W")
    public void addGroupRole(@PathParam("group") String subject, final String inputString)
            throws IOException, ParseException {
        try {
            String method = "addGroupRole";
            logger.start(method);
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL(kc_url + "/admin/realms/StackV/roles/" + subject + "/composites");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", auth);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            // Construct array
            try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()))) {
                Object obj = parser.parse(inputString);
                final JSONArray roleArr = (JSONArray) obj;

                out.write(roleArr.toString());
            }
            logger.trace("addGroupRole", conn.getResponseCode() + " - " + conn.getResponseMessage(), "result");
        } catch (IOException | ParseException ex) {
            logger.catching("addGroupRole", ex);
            throw ex;
        }
    }

    /* Andrew's draft for new delete method for deleting a role from a group */
    @DELETE
    @Path("keycloak/groups/{group}")
    @Produces("application/json")
    @RolesAllowed("F_Keycloak-W")
    public void removeGroupRole(@PathParam("group") String subject, final String inputString)
            throws IOException, ParseException {
        try {
            String method = "removeGroupRole";
            logger.start(method);
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL(kc_url + "/admin/realms/StackV/roles/" + subject + "/composites");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", auth);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            // Construct array
            try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()))) {
                String actual = inputString;
                Object obj = parser.parse(actual);
                final JSONArray roleArr = (JSONArray) obj;

                out.write(roleArr.toString());
            }

            logger.trace("removeGroupRole", conn.getResponseCode() + " - " + conn.getResponseMessage(), "result");
        } catch (IOException | ParseException ex) {
            logger.catching("removeGroupRole", ex);
            throw ex;
        }
    }

    /* Andrew's Draft for searching for the full information of a single role */
    @GET
    @Path("keycloak/roles/{role}")
    @Produces("application/json")
    @RolesAllowed("F_Keycloak-R")
    public ArrayList<ArrayList<String>> getRoleData(@PathParam("role") String subject)
            throws IOException, ParseException {
        String name = subject;
        try {
            String method = "getRoleData";
            logger.trace_start(method);
            ArrayList<ArrayList<String>> retList = new ArrayList<>();
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL(kc_url + "/admin/realms/StackV/clients/5c0fab65-4577-4747-ad42-59e34061390b/roles");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", auth);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            logger.trace("getRoleData", conn.getResponseCode() + " - " + conn.getResponseMessage(), "result");
            StringBuilder responseStr;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                responseStr = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    responseStr.append(inputLine);
                }
            }

            Object obj = parser.parse(responseStr.toString());
            HashMap<String, ArrayList<String>> search = new HashMap<>();
            JSONArray groupArr = (JSONArray) obj;
            for (Object group : groupArr) {
                ArrayList<String> groupList = new ArrayList<>();
                JSONObject groupJSON = (JSONObject) group;
                groupList.add((String) groupJSON.get("id"));
                groupList.add((String) groupJSON.get("name"));
                groupList.add(groupJSON.get("scopeParamRequired").toString());
                groupList.add(groupJSON.get("composite").toString());
                groupList.add(groupJSON.get("clientRole").toString());
                groupList.add((String) groupJSON.get("containerId"));

                search.put((String) groupJSON.get("name"), groupList);

            }
            retList.add(search.get(name));
            logger.trace_end(method);
            return retList;
        } catch (IOException | ParseException ex) {
            logger.catching("getRoleData", ex);
            throw ex;
        }

    }

    @GET
    @Path("/keycloak/roles")
    @Produces("application/json")
    @RolesAllowed("F_Keycloak-R")
    public ArrayList<ArrayList<String>> getRoles() throws IOException, ParseException {
        try {
            String method = "getRoles";
            logger.trace_start(method);
            ArrayList<ArrayList<String>> retList = new ArrayList<>();
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL(kc_url + "/admin/realms/StackV/roles");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", auth);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            logger.trace("getGroups", conn.getResponseCode() + " - " + conn.getResponseMessage(), "result");
            StringBuilder responseStr;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                responseStr = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    responseStr.append(inputLine);
                }
            }

            Object obj = parser.parse(responseStr.toString());
            JSONArray groupArr = (JSONArray) obj;
            for (Object group : groupArr) {
                ArrayList<String> groupList = new ArrayList<>();
                JSONObject groupJSON = (JSONObject) group;
                groupList.add((String) groupJSON.get("id"));
                groupList.add((String) groupJSON.get("name"));

                retList.add(groupList);
            }
            logger.trace_end(method);
            return retList;
        } catch (IOException | ParseException ex) {
            logger.catching("getGroups", ex);
            throw ex;
        }
    }

    @POST
    @Path("/keycloak/users/{user}/roles")
    @Produces("application/json")
    @RolesAllowed("F_Keycloak-W")
    public void addUserRole(@PathParam("user") String subject, final String inputString)
            throws IOException, ParseException {
        try {
            String method = "addUserRole";
            logger.start(method);
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL(kc_url + "/admin/realms/StackV/users/" + subject + "/role-mappings/realm");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", auth);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            // Construct array
            try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()))) {
                Object obj = parser.parse(inputString);
                final JSONObject inputJSON = (JSONObject) obj;
                JSONArray roleArr = new JSONArray();
                roleArr.add(inputJSON);

                out.write(roleArr.toString());
            }

            logger.trace("addUserGroup", conn.getResponseCode() + " - " + conn.getResponseMessage(), "result");
        } catch (IOException | ParseException ex) {
            logger.catching("addUserGroup", ex);
            throw ex;
        }
    }

    @DELETE
    @Path("/keycloak/users/{user}/roles")
    @Produces("application/json")
    @RolesAllowed("F_Keycloak-W")
    public void removeUserRole(@PathParam("user") String subject, final String inputString)
            throws IOException, ParseException {
        try {
            String method = "removeUserRole";
            logger.start(method);
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL(kc_url + "/admin/realms/StackV/users/" + subject + "/role-mappings/realm");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", auth);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            // Construct array
            try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()))) {
                Object obj = parser.parse(inputString);
                final JSONObject inputJSON = (JSONObject) obj;
                JSONArray roleArr = new JSONArray();
                roleArr.add(inputJSON);

                out.write(roleArr.toString());
            }

            logger.trace("removeUserGroup", conn.getResponseCode() + " - " + conn.getResponseMessage(), "result");
        } catch (IOException | ParseException ex) {
            logger.catching("removeUserGroup", ex);
            throw ex;
        }
    }

    @GET
    @Path("/keycloak/users/{user}/roles")
    @Produces("application/json")
    @RolesAllowed("F_Keycloak-R")
    public String getUserRoles(@PathParam("user") String subject) throws IOException, ParseException {
        try {
            String method = "getUserRoles";
            logger.trace_start(method);
            JSONArray retJSON = new JSONArray();
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");

            // Get assigned roles.
            URL url = new URL(kc_url + "/admin/realms/StackV/users/" + subject + "/role-mappings/realm");
            Request request = new Request.Builder().url(url).header("Authorization", auth).build();
            Response response = client.newCall(request).execute();
            String responseStr = response.body().string();

            Object obj = parser.parse(responseStr);
            JSONArray groupArr = (JSONArray) obj;
            ArrayList<String> roleList = new ArrayList<>();
            for (Object obj2 : groupArr) {
                JSONObject role = (JSONObject) obj2;
                roleList.add((String) role.get("name"));

                JSONObject roleJSON = new JSONObject();
                roleJSON.put("id", (String) role.get("id"));
                roleJSON.put("name", (String) role.get("name"));
                retJSON.add(roleJSON);
            }

            // Get delegated roles.
            for (String comp : roleList) {
                url = new URL(kc_url + "/admin/realms/StackV/roles/" + comp + "/composites");
                request = new Request.Builder().url(url).header("Authorization", auth).build();
                response = client.newCall(request).execute();
                responseStr = response.body().string();

                obj = parser.parse(responseStr);
                JSONArray roleArr = (JSONArray) obj;
                for (Object obj2 : roleArr) {
                    JSONObject roleJSON = new JSONObject();
                    JSONObject role = (JSONObject) obj2;
                    roleJSON.put("id", (String) role.get("id"));
                    roleJSON.put("name", (String) role.get("name"));
                    roleJSON.put("from", comp);

                    retJSON.add(roleJSON);
                }
            }

            logger.trace_end(method);
            return retJSON.toJSONString();

        } catch (IOException | ParseException ex) {
            logger.catching("getUserRoles", ex);
            throw ex;
        }
    }
}
