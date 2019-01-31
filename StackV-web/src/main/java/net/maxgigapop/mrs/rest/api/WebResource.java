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
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.EJBException;
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

import com.hp.hpl.jena.ontology.OntModel;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.apache.commons.dbutils.DbUtils;
import org.jboss.resteasy.spi.HttpRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.system.HandleSystemCall;
import net.maxgigapop.mrs.common.KeycloakHandler;
import net.maxgigapop.mrs.common.TokenHandler;
import net.maxgigapop.mrs.service.ServiceHandler;
import net.maxgigapop.mrs.service.VerificationHandler;
import templateengine.TemplateEngine;

/**
 * REST Web Service
 *
 * @author rikenavadur
 */
@Path("app")
public class WebResource {

    // SERVICES DATA
    public static final Map<String, List<String>> Services = createServiceMap();

    public static enum SuperState {
        CREATE, CANCEL, MODIFY, REINSTATE, DELETE
    }

    private static Map<String, List<String>> createServiceMap() {
        HashMap<String, List<String>> result = new HashMap<>();

        result.put("vcn", Arrays.asList("Virtual Cloud Network", "Network Creation Pilot Testbed."));

        result.put("dnc", Arrays.asList("Dynamic Network Connection", "Creation of new network connections."));

        result.put("ahc", Arrays.asList("Advanced Hybrid Cloud", "Advanced Hybrid Cloud Service."));

        result.put("ecc", Arrays.asList("EdgeCloud Connection", "MAX EdgeCloud Pilot Service."));

        return Collections.unmodifiableMap(result);
    }

    private static final StackLogger logger = new StackLogger(WebResource.class.getName(), "WebResource");
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

    /**
     * Creates a new instance of WebResource
     */
    public WebResource() {
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
        } catch (IOException | ParseException ex) {
            logger.throwing("loadConfig", ex);
        }
    }

    @PUT
    @Path("/reload/")
    public static void reloadConfigs() {
        WebResource.loadConfig();
    }

    static {
        WebResource.loadConfig();

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

        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            front_conn = factory.getConnection("frontend");
            prep = front_conn.prepareStatement("SELECT `subject` FROM `acl` WHERE `object` = ?");
            prep.setString(1, instance);
            rs = prep.executeQuery();

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
        } catch (IOException | SQLException | ParseException ex) {
            logger.catching(method, ex);
            return retJSON.toJSONString();
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    /**
     * @apiDefine AuthHeader
     * @apiHeader {String} authorization="Authorization: bearer $KC_ACCESS_TOKEN"
     *            Keycloak authorization token header.
     */
    // >Access
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
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            front_conn = factory.getConnection("frontend");

            // excluding the topuri if the driver type is raw
            prep = front_conn.prepareStatement("INSERT INTO acl (`subject`, `object`) VALUES (?, ?)");
            prep.setString(1, username);
            prep.setString(2, uuid);
            prep.executeUpdate();
        } catch (SQLException ex) {
            logger.catching("addAccess", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
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
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            front_conn = factory.getConnection("frontend");

            // excluding the topuri if the driver type is raw
            prep = front_conn.prepareStatement("DELETE FROM acl WHERE `subject` = ? AND `object` = ?");
            prep.setString(1, username);
            prep.setString(2, uuid);
            prep.executeUpdate();
        } catch (SQLException ex) {
            logger.catching("removeAccess", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
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

    // >Clipbook
    /**
     * Gets all clips associated with user.
     * 
     * @param username
     * @param clipObject
     * @throws SQLException
     */
    @GET
    @Path("/clipbook/{user}")
    @Produces("application/json")
    public JSONArray getClips(@PathParam("user") String username) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            front_conn = factory.getConnection("frontend");
            JSONArray retJSON = new JSONArray();

            prep = front_conn.prepareStatement("SELECT name, clip FROM clipbook WHERE username = ?");
            prep.setString(1, username);
            rs = prep.executeQuery();

            while (rs.next()) {
                JSONObject clipJSON = new JSONObject();
                clipJSON.put("name", rs.getString("name"));
                clipJSON.put("clip", rs.getString("clip"));
                retJSON.add(clipJSON);
            }
            return retJSON;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    /**
     * Gets all clips associated with user.
     * 
     * @param username
     * @param clipObject
     * @throws SQLException
     */
    @GET
    @Path("/clipbook/{user}/{name}")
    @Produces("application/json")
    public String getClipByName(@PathParam("user") String username, @PathParam("name") String name)
            throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            front_conn = factory.getConnection("frontend");
            prep = front_conn.prepareStatement("SELECT name, clip FROM clipbook WHERE username = ?");
            prep.setString(1, username);
            rs = prep.executeQuery();

            while (rs.next()) {
                return rs.getString("clip");
            }
            return null;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    /**
     * Adds a clip.
     * 
     * @param username
     * @param clipObject
     * @throws SQLException
     */
    @POST
    @Path("/clipbook/{user}")
    @Consumes(value = { "application/json" })
    public void addClip(@PathParam("user") String username, final String dataInput) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            JSONObject inputJSON = new JSONObject();
            try {
                Object obj = parser.parse(dataInput);
                inputJSON = (JSONObject) obj;
            } catch (ParseException ex) {
                logger.catching("addClip", ex);
            }

            String name = (String) inputJSON.get("name");
            String clip = (String) inputJSON.get("clip");
            front_conn = factory.getConnection("frontend");

            // excluding the topuri if the driver type is raw
            prep = front_conn
                    .prepareStatement("INSERT INTO frontend.clipbook (`username`, `name`, `clip`) VALUES (?, ?, ?)");
            prep.setString(1, username);
            prep.setString(2, name);
            prep.setString(3, clip);
            prep.executeUpdate();
        } catch (SQLException ex) {
            logger.catching("addClip", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    /**
     * Adds a clip.
     * 
     * @param username
     * @param clipObject
     * @throws SQLException
     */
    @DELETE
    @Path("/clipbook/{user}/{name}")
    @Consumes(value = { "application/json" })
    public void deleteClip(@PathParam("user") String username, @PathParam("name") String name) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            front_conn = factory.getConnection("frontend");

            // excluding the topuri if the driver type is raw
            prep = front_conn.prepareStatement("DELETE FROM frontend.clipbook WHERE username = ? AND name = ?");
            prep.setString(1, username);
            prep.setString(2, name);
            prep.executeUpdate();
        } catch (SQLException ex) {
            logger.catching("deleteClip", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
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

    // >Drivers
    @GET
    @Path("/drivers")
    @Produces("application/json")
    @RolesAllowed("F_Drivers-R")
    public String getDrivers() throws SQLException, IOException, ParseException {
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        final TokenHandler token = new TokenHandler(refresh);
        JSONArray retArr = new JSONArray();

        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            front_conn = factory.getConnection("frontend");

            prep = front_conn.prepareStatement("SELECT * FROM driver");
            ResultSet ret = prep.executeQuery();
            String resStr;
            try {
                URL url = new URL(String.format("%s/driver", host));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                resStr = executeHttpMethod(url, connection, "GET", null, token.auth());
            } catch (IOException ex) {
                logger.catching("installDriver", ex);
                throw ex;
            }

            JSONArray resJSON = (JSONArray) parser.parse(resStr);
            ArrayList<String> mapped = new ArrayList<>();
            // Iterate through saved/known drivers.
            while (ret.next()) {
                JSONObject driver = new JSONObject();
                String status = "Unplugged";
                String urn = ret.getString("urn");
                driver.put("urn", urn);
                driver.put("type", ret.getString("type"));
                driver.put("xml", ret.getString("xml"));

                // Iterate through backend results looking for match.
                for (int i = 0; i < resJSON.size(); i++) {
                    JSONObject resDriver = (JSONObject) resJSON.get(i);
                    if (resDriver != null && resDriver.get("topologyUri").equals(urn)) {
                        status = "Plugged";
                        mapped.add(urn);

                        driver.put("errors", resDriver.get("contErrors"));
                        driver.put("disabled", resDriver.get("disabled"));

                        break;
                    }
                }
                driver.put("status", status);
                retArr.add(driver);
            }

            // Synchronize any missing backend drivers
            for (int i = 0; i < resJSON.size(); i++) {
                JSONObject resDriver = (JSONObject) resJSON.get(i);
                if (!mapped.contains(resDriver.get("topologyUri"))) {
                    // Translate properties into xml
                    String xml = "<driverInstance><properties>";
                    String propStr;
                    try {
                        URL url = new URL(String.format("%s/driver/%s", host, (String) resDriver.get("topologyUri")));
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        propStr = executeHttpMethod(url, connection, "GET", null, token.auth());
                    } catch (IOException ex) {
                        logger.catching("installDriver", ex);
                        throw ex;
                    }
                    JSONObject propJSON = (JSONObject) parser.parse(propStr);
                    for (Object key : propJSON.keySet()) {
                        String keyStr = (String) key;
                        String keyValue = (String) propJSON.get(keyStr);
                        xml += "<entry><key>" + keyStr + "</key><value>" + keyValue + "</value></entry>";
                    }
                    xml += "</properties></driverInstance>";

                    String urn = (String) resDriver.get("topologyUri");
                    String type = (String) resDriver.get("driverEjbPath");
                    prep = front_conn.prepareStatement(
                            "INSERT INTO frontend.driver VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `type` = ?,`xml` = ?");
                    prep.setString(1, urn);
                    prep.setString(2, type);
                    prep.setString(3, xml);
                    prep.setString(4, type);
                    prep.setString(5, xml);
                    prep.executeUpdate();
                }
            }

            return retArr.toJSONString();
        } catch (SQLException ex) {
            logger.catching("getDrivers", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    /**
     * Adds a new driver
     * 
     * @param dataInput
     * @throws SQLException
     */
    @PUT
    @Path("/drivers/")
    @Consumes(value = { "application/json" })
    @RolesAllowed("F_Drivers-W")
    public void addDriver(final String dataInput) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            JSONObject inputJSON = new JSONObject();
            try {
                Object obj = parser.parse(dataInput);
                inputJSON = (JSONObject) obj;

            } catch (ParseException ex) {
                logger.catching("addDriver", ex);
            }

            String urn = (String) inputJSON.get("urn");
            String type = (String) inputJSON.get("type");
            String xml = (String) inputJSON.get("xml");

            front_conn = factory.getConnection("frontend");
            prep = front_conn.prepareStatement(
                    "INSERT INTO frontend.driver VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `type` = ?,`xml` = ?");
            prep.setString(1, urn);
            prep.setString(2, type);
            prep.setString(3, xml);
            prep.setString(4, type);
            prep.setString(5, xml);
            prep.executeUpdate();
        } catch (SQLException ex) {
            logger.catching("addDriver", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    /**
     * Deletes a driver
     * 
     * @param dataInput
     * @throws SQLException
     */
    @DELETE
    @Path("/drivers/")
    @Consumes(value = { "application/json" })
    @RolesAllowed("F_Drivers-W")
    public void deleteDriver(final String dataInput) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            JSONObject inputJSON = new JSONObject();
            try {
                Object obj = parser.parse(dataInput);
                inputJSON = (JSONObject) obj;

            } catch (ParseException ex) {
                logger.catching("addDriver", ex);
            }

            String urn = (String) inputJSON.get("urn");

            front_conn = factory.getConnection("frontend");
            prep = front_conn.prepareStatement("DELETE FROM frontend.driver WHERE `urn` = ?");
            prep.setString(1, urn);
            prep.executeUpdate();
        } catch (SQLException ex) {
            logger.catching("deleteDriver", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    @PUT
    @Path("/driver/{user}/edit/{topuri}")
    @Consumes(value = { "application/json" })
    @RolesAllowed("F_Drivers-W")
    public String editDriverProfile(@PathParam(value = "user") String username,
            @PathParam(value = "topuri") String oldTopUri, final String dataInput) throws SQLException, ParseException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;

        try {
            JSONObject inputJSON = new JSONObject();
            try {
                Object obj = parser.parse(dataInput);
                inputJSON = (JSONObject) obj;

            } catch (ParseException ex) {
                logger.catching("editDriverProfile", ex);
            }

            String newTopUri = (String) inputJSON.get("topuri");
            String newData = (String) inputJSON.get("data");

            front_conn = factory.getConnection("frontend");

            prep = front_conn.prepareStatement(
                    "UPDATE driver_wizard SET TopUri = ?, data = ? WHERE username = ? AND TopUri = ?");
            prep.setString(1, newTopUri);
            prep.setString(2, newData);
            prep.setString(3, username);
            prep.setString(4, oldTopUri);
            prep.executeUpdate();
        } catch (SQLException ex) {
            logger.catching("editDriverProfile", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }

        return "Saved edits successfully.";
    }

    @DELETE
    @Path(value = "driver/{username}/delete/{topuri}")
    @RolesAllowed("F_Drivers-W")
    public String deleteDriverProfile(@PathParam(value = "username") String username,
            @PathParam(value = "topuri") String topuri) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            front_conn = factory.getConnection("frontend");

            prep = front_conn.prepareStatement(
                    "DELETE FROM driver_wizard WHERE username = \'" + username + "\' AND TopUri = \'" + topuri + "\'");
            // prep.setString(1, username);
            // prep.setString(2, topuri);
            prep.executeUpdate();

            return "Deleted";
        } catch (SQLException ex) {
            logger.catching("deleteDriverProfile", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    @GET
    @Path("/driver/{user}/getdetails/{topuri}")
    @Produces("application/json")
    @RolesAllowed("F_Drivers-R")
    public JSONObject getDriverDetails(@PathParam(value = "user") String username,
            @PathParam(value = "topuri") String topuri) throws SQLException, ParseException {
        Connection front_conn = factory.getConnection("frontend");

        PreparedStatement prep = front_conn.prepareStatement(
                "SELECT * FROM driver_wizard WHERE username = \'" + username + "\' AND TopUri = \'" + topuri + "\'");
        // prep.setString(1, username);
        // prep.setString(2, topuri);
        ResultSet rs = prep.executeQuery();

        rs.next();

        Object obj = parser.parse(rs.getString("data"));
        JSONObject JSONtemp = (JSONObject) obj;
        JSONArray JSONtempArray = (JSONArray) JSONtemp.get("jsonData");
        JSONObject JSONdata = (JSONObject) JSONtempArray.get(0);

        commonsClose(front_conn, prep, rs, logger);

        return JSONdata;
    }

    @GET
    @Path("/driver/{user}/get")
    @Produces("application/json")
    @RolesAllowed("F_Drivers-R")
    public ArrayList<String> getDriver(@PathParam("user") String username) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            ArrayList<String> list = new ArrayList<>();

            front_conn = factory.getConnection("frontend");

            prep = front_conn.prepareStatement("SELECT * FROM driver_wizard WHERE username = ?");
            prep.setString(1, username);
            ResultSet ret = prep.executeQuery();

            while (ret.next()) {
                list.add(ret.getString("drivername"));
                list.add(ret.getString("drivertype"));
                list.add(ret.getString("TopUri"));
                list.add(ret.getString("description"));
                list.add(ret.getString("data"));
            }

            return list;
        } catch (SQLException ex) {
            logger.catching("getDriver", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

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

    // >Manifests
    /**
     * @api {get} /app/manifest/:svcUUID Get Manifest
     * @apiVersion 1.0.0
     * @apiDescription Get manifest for specified service instance.
     * @apiGroup Manifests
     * @apiUse AuthHeader
     * @apiParam {String} svcUUID instance UUID
     *
     ** @apiExample {curl} Example Call:
     *
     * @apiSuccess Object return
     * @apiSuccessExample {json} Example Response:
     */
    @GET
    @Path("/manifest/{svcUUID}")
    @Produces("application/json")
    public String getManifest(@PathParam("svcUUID") String svcUUID) throws SQLException {
        logger.refuuid(svcUUID);
        String method = "getManifest";
        logger.trace_start(method);

        final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");

        String serviceType = null;
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            front_conn = factory.getConnection("frontend");

            prep = front_conn.prepareStatement("select S.name from service_instance I, service S "
                    + "where I.referenceUUID=? AND I.service_id=S.service_id");
            prep.setString(1, svcUUID);
            rs = prep.executeQuery();
            while (rs.next()) {
                serviceType = rs.getString("name");
            }
        } catch (SQLException ex) {
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
        if (serviceType != null) {
            if (serviceType.equals("Virtual Cloud Network")) {
                try {
                    URL url = new URL(String.format("%s/service/property/%s/host", host, svcUUID));
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    String result = executeHttpMethod(url, conn, "GET", null, auth);
                    switch (result) {
                    case "ops":
                        serviceType = "Virtual Cloud Network - OPS";
                        break;
                    case "aws":
                        serviceType = "Virtual Cloud Network - AWS";
                        break;
                    default:
                        throw new EJBException("cannot tell type of VCN service without 'host' property");
                    }
                } catch (IOException | EJBException ex) {
                    throw new EJBException("cannot tell type of VCN service without 'host' property", ex);
                }
            }
            String manifest = "";
            switch (serviceType) {
            case "Dynamic Network Connection":
                manifest = this.resolveManifest(svcUUID, ManifestTemplate.jsonTemplateDNC, auth);
                break;
            case "Advanced Hybrid Cloud":
                manifest = this.resolveManifest(svcUUID, ManifestTemplate.jsonTemplateAHC, auth);
                break;
            case "Virtual Cloud Network - OPS":
                manifest = this.resolveManifest(svcUUID, ManifestTemplate.jsonTemplateOPS, auth);
                break;
            case "Virtual Cloud Network - AWS":
                manifest = this.resolveManifest(svcUUID, ManifestTemplate.jsonTemplateAWS, auth);
                break;
            default:
                throw new EJBException("cannot get manifest for service type=" + serviceType);
            }
            org.json.JSONObject obj = new org.json.JSONObject(manifest);
            if (!obj.has("jsonTemplate")) {
                throw new EJBException("getManifest cannot get manifest for service uuid=" + svcUUID);
            }
            logger.trace_end(method);
            return obj.getString("jsonTemplate");
        }
        logger.trace_end(method);
        return null;
    }

    @GET
    @Path("/manifest/{svcUUID}")
    @Produces("application/xml")
    public String getManifestXml(@PathParam("svcUUID") String svcUUID) throws SQLException {
        logger.refuuid(svcUUID);
        String manifestJStr = getManifest(svcUUID);
        org.json.JSONObject obj = new org.json.JSONObject(manifestJStr);
        String manifest = org.json.XML.toString(obj);
        return manifest;
    }

    // >Panels
    @GET
    @Path("/panel/wizard")
    @Produces("application/json")
    public ArrayList<ArrayList<String>> loadWizard() throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            ArrayList<ArrayList<String>> retList = new ArrayList<>();
            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest
                    .getAttribute(KeycloakSecurityContext.class.getName());
            AccessToken accessToken = securityContext.getToken();
            String username = accessToken.getPreferredUsername();

            front_conn = factory.getConnection("frontend");

            if (username.equals("admin")) {
                prep = front_conn.prepareStatement("SELECT DISTINCT * FROM service_wizard");
            } else {
                prep = front_conn.prepareStatement("SELECT DISTINCT W.* FROM service_wizard W "
                        + "LEFT JOIN service_wizard_licenses L " + "ON (W.service_wizard_id = L.service_wizard_id) "
                        + "WHERE W.owner = ? OR L.username = ?");
                prep.setString(1, username);
                prep.setString(2, username);
            }
            rs = prep.executeQuery();
            while (rs.next()) {
                ArrayList<String> wizardList = new ArrayList<>();

                wizardList.add(rs.getString("name"));
                wizardList.add(rs.getString("description"));
                wizardList.add(rs.getString("service_wizard_id"));
                wizardList.add(rs.getString("owner"));
                wizardList.add(rs.getString("editable"));
                wizardList.add(rs.getString("created"));
                wizardList.add(rs.getString("last_edited"));

                retList.add(wizardList);
            }

            return retList;
        } catch (SQLException ex) {
            logger.catching("loadWizard", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    @GET
    @Path("/panel/editor")
    @Produces("application/json")
    public ArrayList<ArrayList<String>> loadEditor() {
        ArrayList<ArrayList<String>> retList = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : Services.entrySet()) {
            String entryRole = "F_Services-" + entry.getKey().toUpperCase();
            if (KeycloakHandler.verifyUserRole(httpRequest, (entryRole))) {
                List<String> list = entry.getValue();
                ArrayList<String> wizardList = new ArrayList<>();
                wizardList.add(list.get(0));
                wizardList.add(list.get(1));
                wizardList.add(entry.getKey());

                retList.add(wizardList);
            }
        }

        return retList;
    }

    @GET
    @Path("/panel/{refUuid}/acl")
    @Produces("application/json")
    public ArrayList<String> loadObjectACL(@PathParam("refUuid") String refUuid) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            ArrayList<String> retList = new ArrayList<>();

            front_conn = factory.getConnection("frontend");

            prep = front_conn.prepareStatement("SELECT A.subject FROM acl A WHERE A.object = ?");
            prep.setString(1, refUuid);
            rs = prep.executeQuery();

            while (rs.next()) {
                retList.add(rs.getString("subject"));
            }
            return retList;
        } catch (SQLException ex) {
            logger.catching("loadObjectACL", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    @GET
    @Path("/panel/acl")
    @Produces("application/json")
    public ArrayList<String> loadSubjectACL() throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest
                    .getAttribute(KeycloakSecurityContext.class.getName());
            AccessToken accessToken = securityContext.getToken();
            String username = accessToken.getPreferredUsername();

            ArrayList<String> retList = new ArrayList<>();

            front_conn = factory.getConnection("frontend");

            prep = front_conn.prepareStatement("SELECT A.object FROM acl A WHERE A.subject = ?");
            prep.setString(1, username);
            rs = prep.executeQuery();

            while (rs.next()) {
                retList.add(rs.getString("subject"));
            }
            return retList;
        } catch (SQLException ex) {
            logger.catching("loadObjectACL", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    @GET
    @Path("/details/{uuid}/instance")
    @Produces("application/json")
    public ArrayList<String> loadInstanceDetails(@PathParam("uuid") String uuid) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        logger.refuuid(uuid);
        try {
            ArrayList<String> retList = new ArrayList<>();

            front_conn = factory.getConnection("frontend");

            prep = front_conn.prepareStatement(
                    "SELECT * FROM service_instance WHERE referenceUUID = ? ORDER BY service_instance_id DESC");
            prep.setString(1, uuid);

            rs = prep.executeQuery();
            while (rs.next()) {
                retList.add(Services.get(rs.getString("type")).get(0));
                retList.add(rs.getString("alias_name"));
                retList.add(rs.getString("creation_time"));
                retList.add(rs.getString("username"));
                retList.add(rs.getString("super_state"));
                retList.add(rs.getString("last_state"));
                retList.add(rs.getString("intent"));
            }

            return retList;
        } catch (SQLException ex) {
            logger.catching("loadInstanceDetails", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    @GET
    @Path("/details/{uuid}/verification")
    @Produces("application/json")
    public ArrayList<String> loadInstanceVerification(@PathParam("uuid") String uuid) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        logger.refuuid(uuid);
        try {
            ArrayList<String> retList = new ArrayList<>();

            front_conn = factory.getConnection("frontend");

            prep = front_conn.prepareStatement("SELECT * FROM service_verification V, service_instance I "
                    + "WHERE I.referenceUUID = ? AND V.service_instance_id = I.service_instance_id");
            prep.setString(1, uuid);

            rs = prep.executeQuery();
            while (rs.next()) {
                retList.add(rs.getString("state"));
                retList.add(rs.getString("verification_state"));
                retList.add(rs.getString("verification_run"));
                retList.add(rs.getString("creation_time"));
                retList.add(rs.getString("addition"));
                retList.add(rs.getString("reduction"));
                retList.add(rs.getString("service_instance_id"));
                retList.add(rs.getString("elapsed_time"));
            }

            return retList;
        } catch (SQLException ex) {
            logger.catching("loadInstanceVerification", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    /**
     * @api {GET} /app/details/:siUUID/verification/drone Check Verification Drone
     * @apiVersion 1.0.0
     * @apiDescription Check if instance has an operational verification drone
     * @apiGroup Service
     * @apiUse AuthHeader
     * @apiParam {String} siUUID instance UUID
     *
     * @apiExample {curl} Example Call: curl -X DELETE
     *             http://localhost:8080/StackV-web/restapi/app/service/49f3d197-de3e-464c-aaa8-d3fe5f14af0b
     *             -H "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @GET
    @Path(value = "/details/{siUUID}/verification/drone")
    public String hasVerifyDrone(@PathParam(value = "siUUID") final String refUUID)
            throws SQLException, IOException, InterruptedException {
        String method = "hasVerifyDrone";

        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            front_conn = factory.getConnection("frontend");

            prep = front_conn
                    .prepareStatement("SELECT timestamp FROM service_verification " + "WHERE instanceUUID = ?");
            prep.setString(1, refUUID);
            rs = prep.executeQuery();
            while (rs.next()) {
                BigInteger ONE_BILLION = new BigInteger("1000000000");
                Timestamp time = rs.getTimestamp(1);
                if (time == null) {
                    return "0";
                }
                Timestamp now = new Timestamp(System.currentTimeMillis());

                final BigInteger firstTime = BigInteger.valueOf(time.getTime() / 1000 * 1000).multiply(ONE_BILLION)
                        .add(BigInteger.valueOf(time.getNanos()));
                final BigInteger secondTime = BigInteger.valueOf(now.getTime() / 1000 * 1000).multiply(ONE_BILLION)
                        .add(BigInteger.valueOf(now.getNanos()));
                int diff = (firstTime.subtract(secondTime)).divide(new BigInteger("1000000000000")).intValue();

                if (diff < -1) {
                    return "0";
                } else {
                    return "1";
                }
            }
        } catch (SQLException ex) {
            logger.catching(method, ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
        return "-1";
    }

    @GET
    @Path("/details/{uuid}/acl")
    @Produces("application/json")
    public ArrayList<String> loadInstanceACL(@PathParam("uuid") String uuid) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        logger.refuuid(uuid);
        try {
            ArrayList<String> retList = new ArrayList<>();

            front_conn = factory.getConnection("frontend");

            prep = front_conn.prepareStatement("SELECT * FROM `acl`");

            rs = prep.executeQuery();
            while (rs.next()) {
            }

            return retList;
        } catch (SQLException ex) {
            logger.catching("loadInstanceACL", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    @GET
    @Path("/service/lastverify/{siUUID}")
    @Produces("application/json")
    public HashMap<String, String> getVerificationResults(@PathParam("siUUID") String serviceUUID) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        logger.refuuid(serviceUUID);
        try {
            HashMap<String, String> retMap = new HashMap<>();
            front_conn = factory.getConnection("frontend");
            prep = front_conn.prepareStatement(
                    "SELECT V.* FROM service_instance I, service_verification V WHERE I.referenceUUID = ? AND V.service_instance_id = I.service_instance_id");
            prep.setString(1, serviceUUID);
            rs = prep.executeQuery();
            while (rs.next()) {
                retMap.put("delta_uuid", rs.getString("delta_uuid"));
                retMap.put("creation_time", rs.getString("creation_time"));
                retMap.put("verified_reduction", rs.getString("verified_reduction"));
                retMap.put("verified_addition", rs.getString("verified_addition"));
                retMap.put("unverified_reduction", rs.getString("unverified_reduction"));
                retMap.put("unverified_addition", rs.getString("unverified_addition"));
                retMap.put("reduction", rs.getString("reduction"));
                retMap.put("addition", rs.getString("addition"));
            }

            return retMap;

        } catch (SQLException ex) {
            logger.catching("getVerificationResults", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    @GET
    @Path("/service/availibleitems/{siUUID}")
    @Produces("application/json")
    public String getVerificationResultsUnion(@PathParam("siUUID") String serviceUUID) throws Exception {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        logger.refuuid(serviceUUID);
        try {
            front_conn = factory.getConnection("frontend");

            prep = front_conn.prepareStatement(
                    "SELECT V.* FROM service_instance I, service_verification V WHERE I.referenceUUID = ? AND V.service_instance_id = I.service_instance_id");
            prep.setString(1, serviceUUID);
            rs = prep.executeQuery();
            String verified_addition = "";
            String unverified_reduction = "";
            OntModel vAddition;
            OntModel uReduction;

            while (rs.next()) {
                verified_addition = rs.getString("verified_addition");
                unverified_reduction = rs.getString("unverified_reduction");
            }

            if (verified_addition != null && unverified_reduction != null) {
                vAddition = ModelUtil.unmarshalOntModelJson(verified_addition);
                uReduction = ModelUtil.unmarshalOntModelJson(unverified_reduction);

                ArrayList<OntModel> modelList = new ArrayList<>();
                modelList.add(vAddition);
                modelList.add(uReduction);
                OntModel newModel = ModelUtil.createUnionOntModel(modelList);
                return ModelUtil.marshalOntModelJson(newModel);
            }

            if (verified_addition != null) {
                return verified_addition;
            } else if (unverified_reduction != null) {
                return unverified_reduction;
            } else {
                return null;

            }

        } catch (SQLException ex) {
            logger.catching("getVerificationResultsUnion", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    @GET
    @Path("/delta/{siUUID}")
    @Produces("application/json")
    public String getDeltaBacked(@PathParam("siUUID") String serviceUUID) throws IOException, SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
        try {
            front_conn = factory.getConnection("frontend");

            prep = front_conn.prepareStatement(
                    "SELECT COUNT(*) FROM service_delta D, service_instance I WHERE D.service_instance_id = I.service_instance_id AND I.referenceUUID = ?");
            prep.setString(1, serviceUUID);
            rs = prep.executeQuery();
            rs.next();

            if (rs.getInt(1) > 0) {
                URL url = new URL(String.format("%s/service/delta/%s", host, serviceUUID));
                HttpURLConnection status = (HttpURLConnection) url.openConnection();
                String result = executeHttpMethod(url, status, "GET", null, auth);

                return result;
            } else {
                return "{}";
            }
        } catch (IOException ex) {
            return "{}";
        } catch (SQLException ex) {
            logger.catching("getDeltaBacked", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    @GET
    @Path("/access/{category}/{uuid}")
    public String verifyPanel(@PathParam("category") String category, @PathParam("uuid") String uuid)
            throws SQLException {
        return Boolean.toString(verifyAccess(category, uuid));
    }

    // >Profiles
    /**
     * @api {get} /app/profile/:wizardID Get Profile
     * @apiVersion 1.0.0
     * @apiDescription Get specified profile.
     * @apiGroup Profile
     * @apiUse AuthHeader
     * @apiParam {String} wizardID wizard ID
     *
     * @apiExample {curl} Example Call: curl
     *             http://localhost:8080/StackV-web/restapi/app/profile/11 -H
     *             "Authorization: bearer $KC_ACCESS_TOKEN"
     *
     * @apiSuccess {JSONObject} wizard_json Profile JSON
     * @apiSuccessExample {json} Example Response: {"username": "admin","type":
     *                    "netcreate","alias": "VCN.OPS.1VM_Ext.233","data":
     *                    {"virtual_clouds": []}}
     */
    @GET
    @Path("/profile/{wizardID}")
    @Produces("application/json")
    public String getProfile(@PathParam("wizardID") int wizardID) throws SQLException {
        try {
            if (verifyAccess("profiles", wizardID)) {
                try (Connection front_conn = factory.getConnection("frontend");) {
                    JSONObject profJSON = new JSONObject();
                    try (PreparedStatement prep = front_conn
                            .prepareStatement("SELECT * FROM service_wizard WHERE service_wizard_id = ?");) {
                        prep.setInt(1, wizardID);
                        try (ResultSet rs = prep.executeQuery();) {
                            while (rs.next()) {
                                profJSON.put("name", rs.getString("name"));
                                profJSON.put("wizard_json", rs.getString("wizard_json"));
                                profJSON.put("owner", rs.getString("owner"));
                                profJSON.put("editable", rs.getString("editable"));
                                profJSON.put("authorized", rs.getString("authorized"));
                                profJSON.put("description", rs.getString("description"));
                            }
                        }
                    }

                    try (PreparedStatement prep = front_conn.prepareStatement(
                            "SELECT DISTINCT * FROM service_wizard_licenses WHERE service_wizard_id = ?");) {
                        prep.setInt(1, wizardID);
                        try (ResultSet rs = prep.executeQuery();) {
                            JSONArray licenseJSON = new JSONArray();
                            while (rs.next()) {
                                JSONObject obj = new JSONObject();
                                obj.put("remaining", rs.getInt("remaining"));
                                obj.put("type", rs.getString("type"));
                                obj.put("username", rs.getString("username"));
                                licenseJSON.add(obj);
                            }
                            profJSON.put("licenses", licenseJSON);

                            return profJSON.toJSONString();
                        }
                    }
                }
            }
            return "";
        } catch (SQLException ex) {
            logger.catching("getProfile", ex);
            throw ex;
        }
    }

    /**
     * @api {put} /app/profile/:wizardID/edit Modify Profile
     * @apiVersion 1.0.0
     * @apiDescription Modify the specified profile.
     * @apiGroup Profile
     * @apiUse AuthHeader
     * @apiParam {String} wizardID wizard ID
     * @apiParam {JSONObject} inputString Profile JSON
     *
     * @apiExample {curl} Example Call: curl -X PUT -d @newprofile.json -H
     *             "Content-Type: application/json"
     *             http://localhost:8080/StackV-web/restapi/app/profile/11/edit -H
     *             "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @PUT
    @Path("/profile/{wizardID}/edit")
    @RolesAllowed("F_Profiles-W")
    public void editProfile(@PathParam("wizardID") int wizardID, final String inputString)
            throws SQLException, ParseException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        String method = "editProfile";
        try {
            if (verifyAccess("profiles", wizardID)) {
                Object obj = parser.parse(inputString);
                JSONObject inputJSON = (JSONObject) obj;

                String editable = "0";
                if ((Boolean) inputJSON.get("editable")) {
                    editable = "1";
                }

                logger.trace_start(method);
                // Connect to the DB
                front_conn = factory.getConnection("frontend");
                prep = front_conn.prepareStatement(
                        "UPDATE service_wizard SET wizard_json = ?, editable = ?, last_edited = ? WHERE service_wizard_id = ? ");
                prep.setString(1, (String) inputJSON.get("data"));
                prep.setString(2, editable);
                prep.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                prep.setInt(4, wizardID);
                prep.executeUpdate();
            }
            logger.trace_end(method);
        } catch (SQLException ex) {
            logger.catching("editProfile", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    /**
     * @api {put} /app/profile/:wizardID/meta Modify Profile
     * @apiVersion 1.0.0
     * @apiDescription Modify the specified profile.
     * @apiGroup Profile
     * @apiUse AuthHeader
     * @apiParam {String} wizardID wizard ID
     * @apiParam {JSONObject} inputString Profile JSON
     *
     * @apiExample {curl} Example Call: curl -X PUT -d @newprofile.json -H
     *             "Content-Type: application/json"
     *             http://localhost:8080/StackV-web/restapi/app/profile/11/edit -H
     *             "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @PUT
    @Path("/profile/{wizardID}/meta")
    @RolesAllowed("F_Profiles-W")
    public void editProfileMetadata(@PathParam("wizardID") int wizardID, final String inputString)
            throws SQLException, ParseException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        String method = "editProfileMetadata";
        try {
            if (verifyAccess("profiles", wizardID)) {
                Object obj = parser.parse(inputString);
                JSONObject inputJSON = (JSONObject) obj;
                logger.trace_start(method);
                // Connect to the DB
                front_conn = factory.getConnection("frontend");
                prep = front_conn.prepareStatement(
                        "UPDATE service_wizard SET name = ?, description = ? WHERE service_wizard_id = ? ");
                prep.setString(1, (String) inputJSON.get("name"));
                prep.setString(2, (String) inputJSON.get("description"));
                prep.setInt(3, wizardID);
                prep.executeUpdate();
            }
            logger.trace_end(method);
        } catch (SQLException ex) {
            logger.catching("editProfile", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    /**
     * @api {post} /app/profile/:wizardID/licenses Add Profile license
     * @apiVersion 1.0.0
     * @apiDescription
     * @apiGroup Profile
     * @apiUse AuthHeader
     * @apiParam {String} wizardID wizard ID
     * @apiParam {JSONObject} inputString Profile JSON
     *
     * @apiExample {curl} Example Call: curl -X PUT -d @newprofile.json -H
     *             "Content-Type: application/json"
     *             http://localhost:8080/StackV-web/restapi/app/profile/11/edit -H
     *             "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @POST
    @Path("/profile/{wizardID}/licenses")
    @Consumes(value = { "application/json" })
    @RolesAllowed("F_Profiles-W")
    public void addProfileLicenses(@PathParam("wizardID") int wizardID, final String inputString)
            throws SQLException, ParseException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        String method = "addProfileLicenses";
        try {
            if (verifyAccess("profiles", wizardID)) {
                Object obj = parser.parse(inputString);
                JSONObject inputJSON = (JSONObject) obj;

                logger.trace_start(method);
                // Connect to the DB
                front_conn = factory.getConnection("frontend");
                prep = front_conn.prepareStatement(
                        "INSERT INTO service_wizard_licenses (service_wizard_id, type, remaining, username) VALUES (?,?,?,?)");
                prep.setInt(1, wizardID);
                prep.setString(2, (String) inputJSON.get("type"));
                prep.setInt(3, Integer.parseInt((String) inputJSON.get("remaining")));
                prep.setString(4, (String) inputJSON.get("username"));
                prep.executeUpdate();
            }
            logger.trace_end(method);
        } catch (SQLException ex) {
            logger.catching(method, ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    /**
     * @api {put} /app/profile/:wizardID/licenses Modify Profile
     * @apiVersion 1.0.0
     * @apiDescription Modify the specified profile.
     * @apiGroup Profile
     * @apiUse AuthHeader
     * @apiParam {String} wizardID wizard ID
     * @apiParam {JSONObject} inputString Profile JSON
     *
     * @apiExample {curl} Example Call: curl -X PUT -d @newprofile.json -H
     *             "Content-Type: application/json"
     *             http://localhost:8080/StackV-web/restapi/app/profile/11/edit -H
     *             "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @PUT
    @Path("/profile/{wizardID}/licenses")
    @Consumes(value = { "application/json" })
    @RolesAllowed("F_Profiles-W")
    public void editProfileLicenses(@PathParam("wizardID") int wizardID, final String inputString)
            throws SQLException, ParseException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        String method = "editProfileLicenses";
        try {
            if (verifyAccess("profiles", wizardID)) {
                Object obj = parser.parse(inputString);
                JSONObject inputJSON = (JSONObject) obj;

                logger.trace_start(method);
                // Connect to the DB
                front_conn = factory.getConnection("frontend");
                if (Integer.parseInt((String) inputJSON.get("remaining")) <= 0) {
                    prep = front_conn.prepareStatement(
                            "DELETE FROM service_wizard_licenses WHERE service_wizard_id = ? AND username = ?");
                    prep.setInt(1, wizardID);
                    prep.setString(2, (String) inputJSON.get("username"));
                    prep.executeUpdate();
                } else {
                    prep = front_conn.prepareStatement(
                            "UPDATE service_wizard_licenses SET remaining = ?, type = ? WHERE service_wizard_id = ? AND username = ?");
                    prep.setInt(1, Integer.parseInt((String) inputJSON.get("remaining")));
                    prep.setString(2, (String) inputJSON.get("type"));
                    prep.setInt(3, wizardID);
                    prep.setString(4, (String) inputJSON.get("username"));
                    prep.executeUpdate();
                }
            }
            logger.trace_end(method);
        } catch (SQLException ex) {
            logger.catching(method, ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    /**
     * @api {get} /app/profile/:wizardID/uses Get license uses
     * @apiVersion 1.0.0
     * @apiDescription
     * @apiGroup Profile
     * @apiUse AuthHeader
     * @apiParam {String} wizardID wizard ID
     * @apiParam {String} username username
     *
     */
    @GET
    @Path("/profile/{wizardID}/uses/{username}")
    @Consumes(value = { "application/json" })
    @RolesAllowed("F_Profiles-R")
    public String getProfileLicenseUsage(@PathParam("wizardID") int wizardID, @PathParam("username") String username)
            throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        String method = "getProfileLicenseUsage";
        try {
            if (verifyAccess("profiles", wizardID)) {
                // Connect to the DB
                front_conn = factory.getConnection("frontend");

                prep = front_conn.prepareStatement(
                        "SELECT COUNT(*) FROM service_instance WHERE" + " service_wizard_id = ? AND username = ?");
                prep.setInt(1, wizardID);
                prep.setString(2, username);
                prep.executeQuery();

                rs = prep.executeQuery();
                rs.next();
                return rs.getString(1);
            }
        } catch (SQLException ex) {
            logger.catching(method, ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
        return null;
    }

    /**
     * @api {put} /app/profile/new Add New Profile
     * @apiVersion 1.0.0
     * @apiDescription Save a new wizard profile.
     * @apiGroup Profile
     * @apiUse AuthHeader
     * @apiParam {JSONObject} inputString Profile JSON
     *
     * @apiExample {curl} Example Call: curl -X PUT -d @newprofile.json -H
     *             "Content-Type: application/json"
     *             http://localhost:8080/StackV-web/restapi/app/profile/11/edit -H
     *             "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @PUT
    @Path("/profile/new")
    @RolesAllowed("F_Profiles-W")
    public String newProfile(final String inputString) throws SQLException, ParseException {
        try {
            String method = "newProfile";
            logger.start(method);
            try (Connection front_conn = factory.getConnection("frontend");) {
                Object obj = parser.parse(inputString);
                JSONObject inputJSON = (JSONObject) obj;

                String name = (String) inputJSON.get("name");
                String description = (String) inputJSON.get("description");
                String username = (String) inputJSON.get("username");
                JSONArray licenseArr = (JSONArray) inputJSON.get("licenses");

                JSONObject inputData = (JSONObject) inputJSON.get("data");
                inputData.remove("uuid");
                if (inputData.containsKey("options") && ((JSONArray) inputData.get("options")).isEmpty()) {
                    inputData.remove("options");
                }
                String inputDataString = inputData.toJSONString();

                int authorized = (KeycloakHandler.verifyUserRole(httpRequest, "A_Admin")) ? 1 : 0;
                try (PreparedStatement prep = front_conn.prepareStatement(
                        "INSERT INTO `frontend`.`service_wizard` (owner, name, wizard_json, description, editable, authorized) VALUES (?, ?, ?, ?, ?, ?)");) {
                    prep.setString(1, username);
                    prep.setString(2, name);
                    prep.setString(3, inputDataString);
                    prep.setString(4, description);
                    prep.setInt(5, 0);
                    prep.setInt(6, authorized);
                    prep.executeUpdate();
                }

                int wizardID;
                try (PreparedStatement prep = front_conn.prepareStatement(
                        "SELECT service_wizard_id FROM service_wizard WHERE owner = ? AND name = ?");) {
                    prep.setString(1, username);
                    prep.setString(2, name);
                    try (ResultSet rs = prep.executeQuery();) {
                        rs.next();
                        wizardID = rs.getInt("service_wizard_id");
                    }
                }
                if (licenseArr != null) {
                    for (Object obj2 : licenseArr) {
                        JSONObject licenseObj = (JSONObject) obj2;
                        try (PreparedStatement prep = front_conn.prepareStatement(
                                "INSERT INTO `frontend`.`service_wizard_licenses` (service_wizard_id, username, remaining) VALUES (?, ?, ?)");) {
                            prep.setInt(1, wizardID);
                            prep.setString(2, (String) licenseObj.get("username"));
                            prep.setInt(3, (Integer) licenseObj.get("remaining"));
                            prep.executeUpdate();
                        }
                    }
                }

                logger.end(method);
                return null;
            }
        } catch (SQLException | ParseException ex) {
            logger.catching("newProfile", ex);
            throw ex;
        }
    }

    /**
     * @api {delete} /app/profile/:wizardID Delete Profile
     * @apiVersion 1.0.0
     * @apiDescription Delete specified profile.
     * @apiGroup Profile
     * @apiUse AuthHeader
     * @apiParam {String} wizardID wizard ID
     *
     * @apiExample {curl} Example Call: curl -X DELETE
     *             http://localhost:8080/StackV-web/restapi/app/profile/11 -H
     *             "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @DELETE
    @Path("/profile/{wizardId}")
    @RolesAllowed("F_Profiles-W")
    public void deleteProfile(@PathParam("wizardId") int wizardID) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            String method = "deleteProfile";
            logger.start(method);

            if (verifyAccess("profiles", wizardID)) {
                front_conn = factory.getConnection("frontend");

                prep = front_conn.prepareStatement("DELETE FROM service_wizard WHERE service_wizard_id = ?");
                prep.setInt(1, wizardID);
                prep.executeUpdate();
            }
            logger.end(method);
        } catch (SQLException ex) {
            logger.catching("deleteProfile", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    /**
     * @api {post} /app/profile/ Execute profile
     * @apiVersion 1.0.0
     * @apiDescription
     * @apiGroup Profile
     * @apiUse AuthHeader
     *
     */
    @POST
    @Path(value = "/profile")
    @Consumes(value = { "application/json", "application/xml" })
    @RolesAllowed("F_Profiles-X")
    public javax.ws.rs.core.Response executeProfile(final String inputString)
            throws SQLException, IOException, ParseException, InterruptedException {
        final String method = "executeProfile";
        logger.start(method);
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            logger.start(method, "Thread:" + Thread.currentThread());
            final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
            final TokenHandler token = new TokenHandler(refresh);
            Object obj = parser.parse(inputString);
            final JSONObject inputJSON = (JSONObject) obj;
            String serviceType = (String) inputJSON.get("service");

            // Authorize service.
            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest
                    .getAttribute(KeycloakSecurityContext.class.getName());
            final AccessToken accessToken = securityContext.getToken();
            Set<String> roleSet = accessToken.getRealmAccess().getRoles();
            String username = accessToken.getPreferredUsername();

            String profileID = (String) inputJSON.get("profileID");
            front_conn = factory.getConnection("frontend");
            int profileAuthorized = 0;
            prep = front_conn.prepareStatement("SELECT authorized FROM service_wizard WHERE service_wizard_id = ?");
            prep.setString(1, profileID);
            rs = prep.executeQuery();
            while (rs.next()) {
                profileAuthorized = rs.getInt(1);
            }

            if (roleSet.contains("F_Services-" + serviceType.toUpperCase()) || profileAuthorized == 1) {
                // Instance Creation
                final String refUUID;
                try {
                    URL url = new URL(String.format("%s/service/instance", host));
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    refUUID = executeHttpMethod(url, connection, "GET", null, token.auth());
                } catch (IOException ex) {
                    logger.catching("doCreateService", ex);
                    throw ex;
                }

                inputJSON.remove("username");
                inputJSON.put("username", username);
                inputJSON.put("uuid", refUUID);
                ((JSONObject) inputJSON.get("data")).put("uuid", refUUID);

                String sync = (String) inputJSON.get("synchronous");
                String proceed = (String) inputJSON.get("proceed");
                if (sync != null && sync.equals("true")) {
                    if (proceed != null && proceed.equals("true")) {
                        doCreateService(inputJSON, token, refUUID, true);
                    } else {
                        doCreateService(inputJSON, token, refUUID, false);
                    }
                } else if (proceed != null && proceed.equals("true")) {
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                doCreateService(inputJSON, token, refUUID, true);
                            } catch (SQLException | EJBException | IOException | InterruptedException
                                    | ParseException ex) {
                                logger.catching(method, ex);
                            }
                        }
                    });
                } else {
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                doCreateService(inputJSON, token, refUUID, false);
                            } catch (SQLException | EJBException | IOException | InterruptedException
                                    | ParseException ex) {
                                logger.catching(method, ex);
                            }
                        }
                    });
                }
            } else {
                logger.status(method, "User " + username + " not authorized for service " + serviceType);
                return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.UNAUTHORIZED).build();
            }

            logger.end(method);
        } catch (ParseException ex) {
            logger.catching(method, ex);
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
        return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.OK).build();
    }

    // >Services
    @GET
    @Path("/service/{siUUID}/status")
    public String checkStatus(@PathParam("siUUID") String refUUID) throws SQLException, IOException {
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        final TokenHandler token = new TokenHandler(refresh);

        ServiceHandler instance = new ServiceHandler(refUUID, token);

        return instance.superState.name() + " - " + instance.status() + "\n";
    }

    @GET
    @Path("/service/{siUUID}/substatus")
    public String subStatus(@PathParam("siUUID") String refUUID) throws SQLException, IOException {
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        final TokenHandler token = new TokenHandler(refresh);

        ServiceHandler instance = new ServiceHandler(refUUID, token);

        return instance.status();
    }

    @POST
    @Path(value = "/service")
    @Consumes(value = { "application/json", "application/xml" })
    public String createService(final String inputString)
            throws IOException, EJBException, SQLException, InterruptedException {
        return createService(null, inputString);
    }

    @POST
    @Path(value = "/service/{siUUID}")
    @Consumes(value = { "application/json", "application/xml" })
    public String createService(@PathParam(value = "siUUID") final String siUUID, final String inputString)
            throws IOException, EJBException, SQLException, InterruptedException {
        final String method = "createService";
        try {
            logger.start(method, "Thread:" + Thread.currentThread());
            final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
            final TokenHandler token = new TokenHandler(refresh);
            logger.message(method, inputString);
            Object obj = parser.parse(inputString);
            final JSONObject inputJSON = (JSONObject) obj;
            String serviceType = (String) inputJSON.get("service");

            // Authorize service.
            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest
                    .getAttribute(KeycloakSecurityContext.class.getName());
            final AccessToken accessToken = securityContext.getToken();
            Set<String> roleSet = accessToken.getRealmAccess().getRoles();
            String username = accessToken.getPreferredUsername();

            // Instance Creation
            final String refUUID;
            if (siUUID == null) {
                try {
                    URL url = new URL(String.format("%s/service/instance", host));
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    refUUID = executeHttpMethod(url, connection, "GET", null, token.auth());
                } catch (IOException ex) {
                    logger.catching("doCreateService", ex);
                    throw ex;
                }
            } else {
                refUUID = siUUID;
            }

            if (roleSet.contains("F_Services-" + serviceType.toUpperCase())) {
                inputJSON.remove("username");
                inputJSON.put("username", username);
                inputJSON.put("uuid", refUUID);
                ((JSONObject) inputJSON.get("data")).put("uuid", refUUID);

                String sync = (String) inputJSON.get("synchronous");
                String proceed = (String) inputJSON.get("proceed");
                if (sync != null && sync.equals("true")) {
                    if (proceed != null && proceed.equals("true")) {
                        doCreateService(inputJSON, token, refUUID, true);
                    } else {
                        doCreateService(inputJSON, token, refUUID, false);
                    }
                } else if (proceed != null && proceed.equals("true")) {
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                doCreateService(inputJSON, token, refUUID, true);
                            } catch (SQLException | EJBException | IOException | InterruptedException
                                    | ParseException ex) {
                                logger.catching(method, ex);
                            }
                        }
                    });
                } else {
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                doCreateService(inputJSON, token, refUUID, false);
                            } catch (SQLException | EJBException | IOException | InterruptedException
                                    | ParseException ex) {
                                logger.catching(method, ex);
                            }
                        }
                    });
                }
            } else {
                logger.warning(method, "User not allowed access to " + serviceType);
                return null;
            }
            logger.end(method);
            return refUUID;
        } catch (ParseException ex) {
            logger.catching(method, ex);
            return null;
        }
    }

    @GET
    @Path(value = "/service")
    public String initService() throws IOException {
        String method = "initService";
        logger.trace_start(method);
        try {
            final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
            final TokenHandler token = new TokenHandler(refresh);

            URL url = new URL(String.format("%s/service/instance", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String refUUID = executeHttpMethod(url, connection, "GET", null, token.auth());

            logger.trace_end(method);
            return refUUID;
        } catch (IOException ex) {
            logger.catching(method, ex);
            throw ex;
        }
    }

    @GET
    @Path(value = "/service/uuid")
    public String generateUUID() {
        return UUID.randomUUID().toString();
    }

    @PUT
    @Path(value = "/service/{siUUID}/superstate/{state}")
    public String adminChangeSuperState(@PathParam(value = "siUUID") final String refUUID,
            @PathParam(value = "state") final String state) throws IOException, SQLException {
        final String method = "adminChangeSuperState";
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            logger.start(method);

            String stateStr = state.toUpperCase();

            front_conn = factory.getConnection("frontend");
            prep = front_conn.prepareStatement(
                    "UPDATE `frontend`.`service_instance` SET `super_state` = ? WHERE `service_instance`.`referenceUUID` = ?");
            prep.setString(1, stateStr);
            prep.setString(2, refUUID);
            prep.executeUpdate();

            logger.end(method);
            return null;
        } catch (SQLException ex) {
            logger.catching(method, ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }

    @PUT
    @Path(value = "/service/{siUUID}/{action}")
    public String operate(@PathParam(value = "siUUID") final String refUuid,
            @PathParam(value = "action") final String action) throws IOException {
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        final TokenHandler token = new TokenHandler(refresh);
        final String method = "operate";
        logger.trace_start(method, "Thread:" + Thread.currentThread());

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    doOperate(refUuid, action, token);
                } catch (SQLException | IOException | InterruptedException ex) {
                    logger.catching(method, ex);
                }
            }
        });
        logger.trace_end(method);
        return null;
    }

    @PUT
    @Path(value = "/service/{siUUID}/{action}/sync")
    public void operateSync(@PathParam(value = "siUUID") final String refUuid,
            @PathParam(value = "action") final String action) throws SQLException, InterruptedException, IOException {
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        final TokenHandler token = new TokenHandler(refresh);
        String method = "operateSync";
        logger.trace_start(method);
        doOperate(refUuid, action, token);
        logger.trace_end(method);
    }

    @GET
    @Path(value = "/service/{siUUID}/call_verify")
    public String callVerify(@PathParam(value = "siUUID") final String refUUID)
            throws SQLException, InterruptedException, IOException {
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        final TokenHandler token = new TokenHandler(refresh);
        String method = "callVerify";
        logger.trace_start(method);

        VerificationHandler verify = new VerificationHandler(refUUID, token, 1, 10, true);
        return verify.startVerification();
    }

    @DELETE
    @Path(value = "/service/{siUUID}/{action}")
    public void delete(@PathParam(value = "siUUID") final String refUuid)
            throws SQLException, IOException, InterruptedException {
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        final TokenHandler token = new TokenHandler(refresh);
        String method = "operate";
        logger.trace_start(method, "Thread:" + Thread.currentThread());
        doOperate(refUuid, "delete", token);
        logger.trace_end(method);
    }

    // Async Methods -----------------------------------------------------------
    private void doCreateService(JSONObject inputJSON, TokenHandler token, String refUUID, boolean autoProceed)
            throws EJBException, SQLException, IOException, InterruptedException, ParseException {
        TemplateEngine template = new TemplateEngine();

        String retString = template.apply(inputJSON);
        retString = retString.replace("&lt;", "<").replace("&gt;", ">");

        if (((JSONObject) inputJSON.get("data")).containsKey("parent")) {
            String parent = (String) ((JSONObject) inputJSON.get("data")).get("parent");
            if (parent.contains("amazon")) {
                inputJSON.put("host", "aws");
            } else {
                inputJSON.put("host", "ops");
            }
        }

        inputJSON.put("intent", inputJSON.toJSONString());
        inputJSON.put("data", retString);

        new ServiceHandler(inputJSON, token, refUUID, autoProceed);
    }

    private String doOperate(@PathParam("siUUID") String refUUID, @PathParam("action") String action,
            TokenHandler token) throws SQLException, IOException, InterruptedException {
        ServiceHandler instance = new ServiceHandler(refUUID, token);
        instance.operate(action);

        return instance.superState.name() + " -- " + instance.status();
    }

    // Utility Methods ---------------------------------------------------------
    /**
     * Executes HTTP Request.
     *
     * @param url        destination url
     * @param conn       connection object
     * @param method     request method
     * @param body       request body
     * @param authHeader authorization header
     * @return response string.
     * @throws IOException
     */
    public static String executeHttpMethod(URL url, HttpURLConnection conn, String method, String body,
            String authHeader) throws IOException {
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-type", "application/xml");
        conn.setRequestProperty("Accept", "application/json");

        if (authHeader != null) {
            conn.setRequestProperty("Authorization", authHeader);
            // logger.log(Level.INFO, "{0} header added", authHeader);
        }

        if (body != null && !body.isEmpty()) {
            conn.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.writeBytes(body);
                wr.flush();
            }
        }

        conn.getResponseCode();
        StringBuilder responseStr;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String inputLine;
            responseStr = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                responseStr.append(inputLine);
            }
        }

        String retStr = responseStr.substring(0, Math.min(responseStr.length(), 20));
        if (retStr.length() == 20) {
            retStr += "...";
        }

        return responseStr.toString();
    }

    private String resolveManifest(String refUuid, String jsonTemplate, String auth) {
        try {
            URL url = new URL(String.format("%s/service/manifest/%s", host, refUuid));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String data = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<serviceManifest>\n<serviceUUID/>\n<jsonTemplate>\n%s</jsonTemplate>\n</serviceManifest>",
                    jsonTemplate);
            String result = executeHttpMethod(url, conn, "POST", data, auth);
            return result;
        } catch (IOException ex) {
            throw new EJBException("resolveManifest cannot fetch manifest for service uuid=" + refUuid, ex);
        }
    }

    public static void commonsClose(Connection front_conn, PreparedStatement prep, ResultSet rs, StackLogger logger) {
        try {
            DbUtils.close(rs);
            DbUtils.close(prep);
            DbUtils.close(front_conn);
        } catch (SQLException ex) {
            logger.catching("commonsClose", ex);
        }
    }

    private boolean verifyAccess(String category, int id) throws SQLException {
        String method = "verifyAccess";
        try (Connection front_conn = factory.getConnection("frontend");) {
            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest
                    .getAttribute(KeycloakSecurityContext.class.getName());
            final AccessToken accessToken = securityContext.getToken();
            String username = accessToken.getPreferredUsername();

            boolean result = false;
            switch (category) {
            case "profiles":
                HashSet<String> nameSet = new HashSet<>();
                try (PreparedStatement prep = front_conn
                        .prepareStatement("SELECT DISTINCT owner FROM service_wizard WHERE service_wizard_id = ?");) {
                    prep.setInt(1, id);
                    try (ResultSet rs = prep.executeQuery();) {
                        while (rs.next()) {
                            nameSet.add(rs.getString("owner"));
                        }
                    }
                }
                try (PreparedStatement prep = front_conn.prepareStatement(
                        "SELECT DISTINCT username FROM service_wizard_licenses WHERE service_wizard_id = ?");) {
                    prep.setInt(1, id);
                    try (ResultSet rs = prep.executeQuery();) {
                        while (rs.next()) {
                            nameSet.add(rs.getString("username"));
                        }

                        result = nameSet.contains(username);
                        break;
                    }
                }
            }

            if (result) {
                return true;
            } else {
                logger.warning(method,
                        "User " + username + " refused access to resource [" + category + "], ID: " + id);
                return false;
            }
        } catch (SQLException ex) {
            logger.catching(method, ex);
            throw ex;
        }
    }

    private boolean verifyAccess(String category, String uuid) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        String method = "verifyAccess";
        try {
            front_conn = factory.getConnection("frontend");

            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest
                    .getAttribute(KeycloakSecurityContext.class.getName());
            final AccessToken accessToken = securityContext.getToken();
            String username = accessToken.getPreferredUsername();

            boolean result = false;
            switch (category) {
            case "instances":
                prep = front_conn.prepareStatement("SELECT subject FROM acl WHERE object = ?");
                prep.setString(1, uuid);
                rs = prep.executeQuery();

                while (rs.next()) {
                    if (rs.getString("subject").equals(username)) {
                        result = true;
                        break;
                    }
                }
                break;

            }

            if (result) {
                return true;
            } else {
                logger.warning(method,
                        "User " + username + " refused access to resource [" + category + "], UUID: " + uuid);
                return false;
            }
        } catch (SQLException ex) {
            logger.catching(method, ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs, logger);
        }
    }
}
