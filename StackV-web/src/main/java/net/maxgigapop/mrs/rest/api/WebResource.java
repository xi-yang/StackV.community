/*
 * Copyright (c) 2013-2016 University of Maryland
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
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
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import net.maxgigapop.mrs.system.HandleSystemCall;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import web.beans.serviceBeans;
import com.hp.hpl.jena.ontology.OntModel;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.security.RolesAllowed;
import javax.net.ssl.HttpsURLConnection;
import javax.ws.rs.QueryParam;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.StackLogger;
import org.apache.logging.log4j.Level;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * REST Web Service
 *
 * @author rikenavadur
 */
@Path("app")
public class WebResource {

    private final StackLogger logger = new StackLogger(WebResource.class.getName(), "WebResource");
    private static final Marker SERVICE_MARKER = MarkerManager.getMarker("SQL");

    private final String front_db_user = "front_view";
    private final String front_db_pass = "frontuser";
    String host = "http://127.0.0.1:8080/StackV-web/restapi";
    String kc_url = System.getProperty("kc_url");
    private final serviceBeans servBean = new serviceBeans();
    JSONParser parser = new JSONParser();
    private final ExecutorService executorService = java.util.concurrent.Executors.newCachedThreadPool();

    private final String keycloakStackVClientID = "5c0fab65-4577-4747-ad42-59e34061390b";

    @Context
    private HttpRequest httpRequest;

    @EJB
    HandleSystemCall systemCallHandler;

    /**
     * Creates a new instance of WebResource
     */
    public WebResource() {
    }

    /**
     * @apiDefine AuthHeader
     * @apiHeader {String} authorization="Authorization: bearer $KC_ACCESS_TOKEN" Keycloak authorization token header.
     */
    // >ACL 
    /**
     * @api {post} /app/acl/:refUUID Add ACL Entry
     * @apiVersion 1.0.0
     * @apiDescription Add subject pairing to object specified by UUID.
     * @apiGroup ACL
     * @apiUse AuthHeader
     * @apiParam {String} subject subject ID
     * @apiParam {String} refUUID object reference UUID
     *
     * @apiExample {curl} Example Call:
     * curl -X POST -H "Content-Type: application/json" -d "test1"
     * http://localhost:8080/StackV-web/restapi/app/acl/b7688fef-1911-487e-b5d9-3e9e936599a8
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @POST
    @Path(value = "/acl/{refUUID}")
    @Consumes(value = {"application/json", "application/xml"})
    @RolesAllowed("ACL")
    public void addACLEntry(@PathParam("refUUID") String refUUID, final String subject) {
        try {
            String method = "addACLEntry";
            logger.start(method);
            // Authorize service.
            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class
                    .getName());
            final AccessToken accessToken = securityContext.getToken();

            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("INSERT INTO `frontend`.`acl` (`subject`, `is_group`, `object`) "
                    + "VALUES (?, '0', ?)");
            prep.setString(1, subject);
            prep.setString(2, refUUID);
            prep.executeUpdate();

            logger.end(method);
        } catch (SQLException ex) {
            logger.catching("addACLEntry", ex);
        }
    }

    /**
     * @api {delete} /app/acl/:refUUID Delete ACL Entry
     * @apiDescription Delete subject associated with object specified by UUID.
     * @apiVersion 1.0.0
     * @apiGroup ACL
     * @apiUse AuthHeader
     * @apiParam {String} subject subject ID
     * @apiParam {String} refUUID object reference UUID
     *
     * @apiExample {curl} Example Call:
     * curl -X DELETE -H "Content-Type: application/json" -d "test1"
     * http://localhost:8080/StackV-web/restapi/app/acl/b7688fef-1911-487e-b5d9-3e9e936599a8
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @DELETE
    @Path(value = "/acl/{refUUID}")
    @Consumes(value = {"application/json", "application/xml"})
    @RolesAllowed("ACL")
    public void removeACLEntry(@PathParam("refUUID") String refUUID, final String subject) {
        try {
            String method = "removeACLEntry";
            logger.start(method);
            // Authorize service.
            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class
                    .getName());
            final AccessToken accessToken = securityContext.getToken();

            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("DELETE FROM `frontend`.`acl` WHERE subject = ? AND object = ?");
            prep.setString(1, subject);
            prep.setString(2, refUUID);
            prep.executeUpdate();

            logger.end(method);
        } catch (SQLException ex) {
            logger.catching("removeACLEntry", ex);
        }
    }

    /**
     * @api {GET} /app/acl/:refUUID Get ACL Entries
     * @apiVersion 1.0.0
     * @apiDescription Get all entries associated with object specified by UUID.
     * @apiGroup ACL
     * @apiUse AuthHeader
     * @apiParam {String} refUUID object reference UUID
     *
     * @apiExample {curl} Example Call:
     * curl
     * http://localhost:8080/StackV-web/restapi/app/acl/b7688fef-1911-487e-b5d9-3e9e936599a8
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     *
     * @apiSuccess {JSONArray} users users JSON
     * @apiSuccess {JSONArray} users.user user JSON
     * @apiSuccess {String} users.user.username username
     * @apiSuccess {String} users.user.name full name
     * @apiSuccess {String} users.user.email email
     * @apiSuccessExample {json} Example Response:
     * [["admin","",null]]
     */
    @GET
    @Path("/acl/{refUuid}")
    @Produces("application/json")
    @RolesAllowed("ACL")
    public ArrayList<ArrayList<String>> getACLwithInfo(@PathParam("refUuid") String refUUID) {
        try {
            String method = "getACLwithInfo";
            logger.trace_start(method);
            ArrayList<ArrayList<String>> retList = new ArrayList<>();
            ArrayList<String> sqlList = new ArrayList<>();
            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            try {
                front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                        front_connectionProps);

                PreparedStatement prep = front_conn.prepareStatement("SELECT A.subject FROM acl A WHERE A.object = ?");
                prep.setString(1, refUUID);
                ResultSet rs1 = prep.executeQuery();

                while (rs1.next()) {
                    sqlList.add(rs1.getString("subject"));
                }
            } catch (SQLException ex) {
                logger.catching("getACLwithInfo", ex);
            }

            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL(kc_url + "/admin/realms/StackV/users");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", auth);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            logger.trace("getACLwithInfo", conn.getResponseCode() + " - " + conn.getResponseMessage(), "users");
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
                String username = (String) userJSON.get("username");

                if (sqlList.contains(username)) {
                    ArrayList<String> userList = new ArrayList<>();
                    userList.add(username);

                    if (userJSON.containsKey("firstName") && userJSON.containsKey("lastName")) {
                        userList.add((String) userJSON.get("firstName") + " " + (String) userJSON.get("lastName"));
                    } else {
                        userList.add("");
                    }

                    userList.add((String) userJSON.get("email"));
                    retList.add(userList);
                }
            }

            logger.trace_end(method);
            return retList;
        } catch (IOException | ParseException ex) {
            logger.catching("getACLwithInfo", ex);
            return null;
        }
    }

    // >Drivers   
    /**
     * @api {put} /app/driver/install Install Driver
     * @apiVersion 1.0.0
     * @apiDescription Install driver from JSON
     * @apiGroup Driver
     * @apiUse AuthHeader
     * @apiParam {JSONObject} dataInput driver json
     * @apiParamExample {JSONObject} Example JSON:
     * {
     * TODO - Add Example JSON
     * }
     *
     * @apiExample {curl} Example Call:
     * TODO - Add Example Call
     *
     * @apiSuccess Object return TODO - Add Return
     * @apiSuccessExample {json} Example Response:
     * TODO - Add Example Response
     */
    @PUT
    @Path("/driver/install")
    @Consumes("application/json")
    @Produces("text/plain")
    @RolesAllowed("Drivers")
    public String installDriver(final String dataInput) throws SQLException, ParseException {
        String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        if (refresh != null) {
            auth = servBean.refreshToken(refresh);
        }

        Object obj = parser.parse(dataInput);
        JSONObject JSONtemp = (JSONObject) obj;
        JSONArray JSONtempArray = (JSONArray) JSONtemp.get("jsonData");
        JSONObject JSONdata = (JSONObject) JSONtempArray.get(0);

        String xmldata = JSONtoxml(JSONdata, (String) JSONdata.get("drivertype"));

        try {
            URL url = new URL(String.format("%s/driver", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String result = servBean.executeHttpMethod(url, connection, "POST", xmldata, auth);
            if (!result.equalsIgnoreCase("plug successfully")) //plugin error
            {
                return "PLUGIN FAILED: Driver Resource did not return successfull";
            }
        } catch (IOException ex) {
            logger.catching("installDriver", ex);
            return "PLUGIN FAILED: Exception" + ex;
        }

        return "PLUGIN SUCCEEDED";
    }

    /**
     * @api {put} /app/driver/:user/install/:topuri Install Driver Profile
     * @apiVersion 1.0.0
     * @apiDescription Install driver from StackV profile
     * @apiGroup Driver
     * @apiUse AuthHeader
     * @apiParam {String} user username
     * @apiParam {String} topuri profile topology uri
     *
     * @apiExample {curl} Example Call:
     * TODO - Add Example Call
     *
     * @apiSuccess Object return TODO - Add Return
     * @apiSuccessExample {json} Example Response:
     * TODO - Add Example Response
     */
    @PUT
    @Path("/driver/{user}/install/{topuri}")
    @Produces("text/plain")
    @RolesAllowed("Drivers")
    public String installDriverProfile(@PathParam("user") String username, @PathParam(value = "topuri") String topuri) throws SQLException, ParseException {
        String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        if (refresh != null) {
            auth = servBean.refreshToken(refresh);
        }

        Properties prop = new Properties();
        prop.put("user", front_db_user);
        prop.put("password", front_db_pass);
        Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                prop);

        PreparedStatement prep = front_conn.prepareStatement("SELECT * FROM driver_wizard WHERE username = ? AND TopUri = ?");
        prep.setString(1, username);
        prep.setString(2, topuri);
        ResultSet ret = prep.executeQuery();

        ret.next();
        Object obj = parser.parse(ret.getString("data"));
        JSONObject JSONtemp = (JSONObject) obj;
        JSONArray JSONtempArray = (JSONArray) JSONtemp.get("jsonData");
        JSONObject JSONdata = (JSONObject) JSONtempArray.get(0);

        String xmldata = JSONtoxml(JSONdata, ret.getString("drivertype"));

        try {
            URL url = new URL(String.format("%s/driver", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String result = servBean.executeHttpMethod(url, connection, "POST", xmldata, auth);
            if (!result.equalsIgnoreCase("plug successfully")) //plugin error
            {
                return "PLUGIN FAILED: Driver Resource Failed";
            }
        } catch (IOException ex) {
            logger.catching("installDriverProfile", ex);
            return "PLUGIN FAILED: Exception" + ex;
        }

        return "PLUGIN SUCCEEDED";
    }

    /**
     * @api {put} /app/driver/:user/add Add Driver Profile
     * @apiVersion 1.0.0
     * @apiDescription Add new StackV profile
     * @apiGroup Driver
     * @apiUse AuthHeader
     * @apiParam {String} user username
     * @apiParam {JSONObject} dataInput profile JSON
     * @apiParamExample {JSONObject} Example JSON:
     * {
     * TODO - Add Example JSON
     * }
     *
     * @apiExample {curl} Example Call:
     * TODO - Add Example Call
     *
     * @apiSuccess Object return TODO - Add Return
     * @apiSuccessExample {JSONObject} Example Response:
     * TODO - Add Example Response
     */
    @PUT
    @Path("driver/{user}/add")
    @Consumes(value = {"application/json"})
    @RolesAllowed("Drivers")
    public void addDriver(@PathParam("user") String username, final String dataInput) {
        try {
            JSONObject inputJSON = new JSONObject();
            try {
                Object obj = parser.parse(dataInput);
                inputJSON = (JSONObject) obj;

            } catch (ParseException ex) {
                logger.catching("addDriver", ex);
            }

            String user = (String) inputJSON.get("username");
            String driver = (String) inputJSON.get("drivername");
            String desc = (String) inputJSON.get("driverDescription");
            String data = (String) inputJSON.get("data");
            String uri = (String) inputJSON.get("topuri");
            String drivertype = (String) inputJSON.get("drivertype");

            Properties prop = new Properties();
            prop.put("user", front_db_user);
            prop.put("password", front_db_pass);
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend", prop);

            PreparedStatement prep = conn.prepareStatement("INSERT INTO frontend.driver_wizard VALUES (?, ?, ?, ?, ?, ?)");
            prep.setString(1, user);
            prep.setString(2, driver);
            prep.setString(3, desc);
            prep.setString(4, data);
            prep.setString(5, uri);
            prep.setString(6, drivertype);
            prep.executeUpdate();
        } catch (SQLException ex) {
            logger.catching("addDriver", ex);
        }
    }

    /**
     * @api {delete} /app/driver/:username/delete/:topuri Delete Driver Profile
     * @apiVersion 1.0.0
     * @apiDescription Delete saved driver profile.
     * @apiGroup Driver
     * @apiUse AuthHeader
     * @apiParam {String} username user
     * @apiParam {String} topuri profile topology uri
     *
     * @apiExample {curl} Example Call:
     * TODO - Add Example Call
     *
     * @apiSuccess Object return TODO - Add Return
     * @apiSuccessExample {json} Example Response:
     * TODO - Add Example Response
     */
    @DELETE
    @Path(value = "/driver/{username}/delete/{topuri}")
    @RolesAllowed("Drivers")
    public String deleteDriverProfile(@PathParam(value = "username") String username, @PathParam(value = "topuri") String topuri) {
        try {
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("DELETE FROM frontend.driver_wizard WHERE username = ? AND TopUri = ?");
            prep.setString(1, username);
            prep.setString(2, topuri);
            prep.executeUpdate();

            return "Deleted";
        } catch (SQLException ex) {
            logger.catching("deleteDriverProfile", ex);
            return "Failed";
        }
    }

    /**
     * @api {get} /app/driver/:username/getdetails/:topuri Get Driver Profile
     * @apiVersion 1.0.0
     * @apiDescription Get saved driver profile.
     * @apiGroup Driver
     * @apiUse AuthHeader
     * @apiParam {String} user username
     * @apiParam {String} topuri profile topology uri
     *
     * @apiExample {curl} Example Call:
     * TODO - Add Example Call
     *
     * @apiSuccess Object return TODO - Add Return
     * @apiSuccessExample {json} Example Response:
     * TODO - Add Example Response
     */
    @GET
    @Path("/driver/{user}/getdetails/{topuri}")
    @Produces("application/json")
    @RolesAllowed("Drivers")
    public JSONObject getDriverDetails(@PathParam(value = "user") String username, @PathParam(value = "topuri") String topuri) throws SQLException, ParseException {

        Properties prop = new Properties();
        prop.put("user", front_db_user);
        prop.put("password", front_db_pass);
        Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                prop);

        PreparedStatement prep = front_conn.prepareStatement("SELECT * FROM driver_wizard WHERE username = ? AND TopUri = ?");
        prep.setString(1, username);
        prep.setString(2, topuri);
        ResultSet ret = prep.executeQuery();

        ret.next();

        Object obj = parser.parse(ret.getString("data"));
        JSONObject JSONtemp = (JSONObject) obj;
        JSONArray JSONtempArray = (JSONArray) JSONtemp.get("jsonData");
        JSONObject JSONdata = (JSONObject) JSONtempArray.get(0);

        return JSONdata;
    }

    /**
     * @api {get} /app/driver/:user/get Get Profile Information
     * @apiVersion 1.0.0
     * @apiDescription Get saved driver profile information.
     * @apiGroup Driver
     * @apiUse AuthHeader
     * @apiParam {String} user username
     * @apiParam {String} topuri profile topology uri
     *
     * @apiExample {curl} Example Call:
     * TODO - Add Example Call
     *
     * @apiSuccess Object return TODO - Add Return
     * @apiSuccessExample {json} Example Response:
     * TODO - Add Example Response
     */
    @GET
    @Path("/driver/{user}/get")
    @Produces("application/json")
    @RolesAllowed("Drivers")
    public ArrayList<String> getDriver(@PathParam("user") String username) {
        try {
            ArrayList<String> list = new ArrayList<>();

            Properties prop = new Properties();
            prop.put("user", front_db_user);
            prop.put("password", front_db_pass);
            Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    prop);

            PreparedStatement prep = front_conn.prepareStatement("SELECT * FROM driver_wizard WHERE username = ?");
            prep.setString(1, username);
            ResultSet ret = prep.executeQuery();

            while (ret.next()) {
                list.add(ret.getString("drivername"));
                list.add(ret.getString("description"));
                list.add(ret.getString("data"));
                list.add(ret.getString("TopUri"));
            }

            return list;
        } catch (SQLException ex) {
            logger.catching("getDriver", ex);
            return null;
        }
    }

    // >Keycloak
    /**
     * @api {get} /app/keycloak/users Get Users
     * @apiVersion 1.0.0
     * @apiDescription Get a list of existing users.
     * @apiGroup Keycloak
     * @apiUse AuthHeader
     *
     * @apiExample {curl} Example Call:
     * curl http://localhost:8080/StackV-web/restapi/app/keycloak/users
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     *
     * @apiSuccess {JSONArray} users users JSON
     * @apiSuccess {JSONArray} users.user user JSON
     * @apiSuccess {String} users.user.username username
     * @apiSuccess {String} users.user.name full name
     * @apiSuccess {String} users.user.email email
     * @apiSuccess {String} users.user.time timestamp of user creation
     * @apiSuccess {String} users.user.subject user ID
     * @apiSuccessExample {json} Example Response:
     * [["admin","",null,"1475506393070","1d183570-2798-4d69-80c3-490f926596ff"],["username","","email","1475506797561","1323ff3d-49f3-46ad-8313-53fd4c711ec6"]]
     */
    @GET
    @Path("/keycloak/users")
    @Produces("application/json")
    @RolesAllowed("Keycloak")
    public ArrayList<ArrayList<String>> getUsers() {
        try {
            String method = "getUsers";
            logger.trace_start(method);
            ArrayList<ArrayList<String>> retList = new ArrayList<>();
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL(kc_url + "/admin/realms/StackV/users");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", auth);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            logger.trace("getUsers", conn.getResponseCode() + " - " + conn.getResponseMessage(), "result");
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
                String subject = (String) userJSON.get("id");
                String username = (String) userJSON.get("username");

                ArrayList<String> userList = new ArrayList<>();
                userList.add(username);

                if (userJSON.containsKey("firstName") && userJSON.containsKey("lastName")) {
                    userList.add((String) userJSON.get("firstName") + " " + (String) userJSON.get("lastName"));
                } else {
                    userList.add("");
                }

                userList.add((String) userJSON.get("email"));
                userList.add(Long.toString((Long) userJSON.get("createdTimestamp")));
                userList.add(subject);
                retList.add(userList);
            }

            logger.trace_end(method);
            return retList;
        } catch (IOException | ParseException ex) {
            logger.catching("getUsers", ex);
            return null;
        }
    }

    /**
     * @api {get} /app/keycloak/groups Get Groups
     * @apiVersion 1.0.0
     * @apiDescription Get a list of existing groups.
     * @apiGroup Keycloak
     * @apiUse AuthHeader
     *
     * @apiExample {curl} Example Call:
     * curl http://localhost:8080/StackV-web/restapi/app/keycloak/groups
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     *
     * @apiSuccess {JSONArray} groups groups JSON
     * @apiSuccess {JSONArray} groups.group group JSON
     * @apiSuccess {String} groups.group.id group ID
     * @apiSuccess {String} groups.group.name group name
     * @apiSuccessExample {json} Example Response:
     * [["c8b87f1a-6f2f-4ae0-824d-1dda0ca7aaab","TeamA"],["968ee80f-92a0-42c4-8f19-fe502d41480a","offline_access"]]
     */
    @GET
    @Path("/keycloak/groups")
    @Produces("application/json")
    @RolesAllowed("Keycloak")
    public ArrayList<ArrayList<String>> getGroups() {
        try {
            String method = "getGroups";
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
            return null;
        }
    }

    /**
     * @api {get} /app/keycloak/roles Get Roles
     * @apiVersion 1.0.0
     * @apiDescription Get a list of existing roles.
     * @apiGroup Keycloak
     * @apiUse AuthHeader
     *
     * @apiExample {curl} Example Call:
     * curl http://localhost:8080/StackV-web/restapi/app/keycloak/roles
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     *
     * @apiSuccess {JSONArray} roles roles JSON
     * @apiSuccess {JSONArray} roles.role role JSON
     * @apiSuccess {String} roles.role.id role ID
     * @apiSuccess {String} roles.role.name role name
     * @apiSuccessExample {json} Example Response:
     * [["e619f97d-9811-4612-82f7-fa01fbbf0515","Drivers"],["a08da95a-9c90-4dca-96db-0903cc8f82fa","Labels"],["f12ad2d8-2f7b-4e12-9cfe-d264d13e96fc","Keycloak"]]
     */
    @GET
    @Path("/keycloak/roles")
    @Produces("application/json")
    @RolesAllowed("Keycloak")
    public ArrayList<ArrayList<String>> getRoles() {
        try {
            String method = "getRoles";
            logger.trace_start(method);
            ArrayList<ArrayList<String>> retList = new ArrayList<>();
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL(kc_url + "/admin/realms/StackV/clients/" + keycloakStackVClientID + "/roles");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", auth);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            logger.trace("getRoles", conn.getResponseCode() + " - " + conn.getResponseMessage(), "result");
            StringBuilder responseStr;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                responseStr = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    responseStr.append(inputLine);
                }
            }

            Object obj = parser.parse(responseStr.toString());
            JSONArray roleArr = (JSONArray) obj;
            for (Object role : roleArr) {
                ArrayList<String> roleList = new ArrayList<>();
                JSONObject roleJSON = (JSONObject) role;
                roleList.add((String) roleJSON.get("id"));
                roleList.add((String) roleJSON.get("name"));

                retList.add(roleList);
            }

            logger.trace_end(method);
            return retList;
        } catch (IOException | ParseException ex) {
            logger.catching("getRoles", ex);
            return null;
        }
    }

    /**
     * @api {get} /app/keycloak/users/:user/groups Get User Groups
     * @apiVersion 1.0.0
     * @apiDescription Get a list of groups specified user belongs to.
     * @apiGroup Keycloak
     * @apiUse AuthHeader
     * @apiParam {String} user user ID
     *
     * @apiExample {curl} Example Call:
     * curl http://localhost:8080/StackV-web/restapi/app/keycloak/groups
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     *
     * @apiSuccess {JSONArray} groups groups JSON
     * @apiSuccess {JSONArray} groups.group group JSON
     * @apiSuccess {String} groups.group.id group ID
     * @apiSuccess {String} groups.group.name group name
     * @apiSuccessExample {json} Example Response:
     * [["c8b87f1a-6f2f-4ae0-824d-1dda0ca7aaab","TeamA"],["6f299a2f-185b-4784-a135-b861179af17d","admin"]]
     */
    @GET
    @Path("/keycloak/users/{user}/groups")
    @Produces("application/json")
    @RolesAllowed("Keycloak")
    public ArrayList<ArrayList<String>> getUserGroups(@PathParam("user") String subject) {
        try {
            String method = "getUserGroups";
            logger.trace_start(method);
            ArrayList<ArrayList<String>> retList = new ArrayList<>();
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL(kc_url + "/admin/realms/StackV/users/" + subject + "/role-mappings/realm");
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
            JSONArray roleArr = (JSONArray) obj;
            for (Object obj2 : roleArr) {
                ArrayList<String> roleList = new ArrayList<>();
                JSONObject role = (JSONObject) obj2;
                roleList.add((String) role.get("id"));
                roleList.add((String) role.get("name"));

                retList.add(roleList);
            }

            logger.trace_end(method);
            return retList;
        } catch (IOException | ParseException ex) {
            logger.catching("getUserGroups", ex);
            return null;
        }
    }

    /**
     * @api {post} /app/keycloak/users/:user/groups Add User to Group
     * @apiVersion 1.0.0
     * @apiDescription Assign group membership to a user
     * @apiGroup Keycloak
     * @apiUse AuthHeader
     * @apiParam {String} user user ID
     * @apiParam {JSONObject} inputString input JSON
     * @apiParam {String} inputString.id group ID
     * @apiParam {String} inputString.name group name
     *
     * @apiExample {curl} Example Call:
     * curl -X POST -d '{"id":"c8b87f1a-6f2f-4ae0-824d-1dda0ca7aaab","name":"TeamA"}'
     * http://localhost:8080/StackV-web/restapi/app/keycloak/users/1d183570-2798-4d69-80c3-490f926596ff/groups
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @POST
    @Path("/keycloak/users/{user}/groups")
    @Produces("application/json")
    @RolesAllowed("Keycloak")
    public void addUserGroup(@PathParam("user") String subject, final String inputString) {
        try {
            String method = "addUserGroup";
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
        }
    }

    /**
     * @api {delete} /app/keycloak/users/:user/groups Remove User from Group
     * @apiVersion 1.0.0
     * @apiDescription Retract group membership from a user
     * @apiGroup Keycloak
     * @apiUse AuthHeader
     * @apiParam {String} user user ID
     * @apiParam {JSONObject} inputString input JSON
     * @apiParam {String} inputString.id group ID
     * @apiParam {String} inputString.name group name
     *
     * @apiExample {curl} Example Call:
     * curl -X DELETE -d '{"id":"c8b87f1a-6f2f-4ae0-824d-1dda0ca7aaab","name":"TeamA"}'
     * http://localhost:8080/StackV-web/restapi/app/keycloak/users/1d183570-2798-4d69-80c3-490f926596ff/groups
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @DELETE
    @Path("/keycloak/users/{user}/groups")
    @Produces("application/json")
    @RolesAllowed("Keycloak")
    public void removeUserGroup(@PathParam("user") String subject, final String inputString) {
        try {
            String method = "removeUserGroup";
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
        }
    }

    /**
     * @api {get} /app/keycloak/users/:user/roles Get User Roles
     * @apiVersion 1.0.0
     * @apiDescription Get a list of roles the user has assigned.
     * @apiGroup Keycloak
     * @apiUse AuthHeader
     * @apiParam {String} user user ID
     *
     * @apiExample {curl} Example Call:
     * curl http://localhost:8080/StackV-web/restapi/app/keycloak/users/1d183570-2798-4d69-80c3-490f926596ff/roles
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     *
     * @apiSuccess {JSONArray} roles roles JSON
     * @apiSuccess {JSONArray} roles.role role JSON
     * @apiSuccess {String} roles.role.id role ID
     * @apiSuccess {String} roles.role.name role name
     * @apiSuccess {String} roles.role.source role source, either "assigned" or the name of group who delegates the role.
     * @apiSuccessExample {json} Example Response:
     * [["056af27f-b754-4287-aebe-129f5de8ab47","Services","assigned"],["a08da95a-9c90-4dca-96db-0903cc8f82fa","Labels","admin"],["7d307d71-1b89-45f7-a0be-3b0f0d1b2045","Manifests","admin"]]
     */
    @GET
    @Path("/keycloak/users/{user}/roles")
    @Produces("application/json")
    @RolesAllowed("Keycloak")
    public ArrayList<ArrayList<String>> getUserRoles(@PathParam("user") String subject) {
        try {
            String method = "getUserRoles";
            logger.trace_start(method);
            ArrayList<ArrayList<String>> retList = new ArrayList<>();
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");

            // Get assigned roles.
            URL url = new URL(kc_url + "/admin/realms/StackV/users/" + subject + "/role-mappings/clients/" + keycloakStackVClientID);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", auth);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            logger.trace("getUserRoles", conn.getResponseCode() + " - " + conn.getResponseMessage(), "roles");
            StringBuilder responseStr;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                responseStr = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    responseStr.append(inputLine);
                }
            }

            Object obj = parser.parse(responseStr.toString());
            JSONArray roleArr = (JSONArray) obj;
            for (Object obj2 : roleArr) {
                ArrayList<String> roleList = new ArrayList<>();
                JSONObject role = (JSONObject) obj2;
                roleList.add((String) role.get("id"));
                roleList.add((String) role.get("name"));
                roleList.add("assigned");

                retList.add(roleList);
            }

            // Get groups.
            url = new URL(kc_url + "/admin/realms/StackV/users/" + subject + "/role-mappings/realm");
            conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", auth);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            logger.trace("getUserRoles", conn.getResponseCode() + " - " + conn.getResponseMessage(), "groups");
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String inputLine;
                responseStr = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    responseStr.append(inputLine);
                }
            }

            obj = parser.parse(responseStr.toString());
            JSONArray groupArr = (JSONArray) obj;
            ArrayList<String> groupList = new ArrayList<>();
            for (Object obj2 : groupArr) {
                JSONObject role = (JSONObject) obj2;
                groupList.add((String) role.get("name"));
            }

            // Get delegated roles.
            for (String group : groupList) {
                url = new URL(kc_url + "/admin/realms/StackV/roles/" + group + "/composites");
                conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", auth);
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                logger.trace("getUserRoles", conn.getResponseCode() + " - " + conn.getResponseMessage(), "composites");
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String inputLine;
                    responseStr = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        responseStr.append(inputLine);
                    }
                }

                obj = parser.parse(responseStr.toString());
                roleArr = (JSONArray) obj;
                for (Object obj2 : roleArr) {
                    ArrayList<String> roleList = new ArrayList<>();
                    JSONObject role = (JSONObject) obj2;
                    roleList.add((String) role.get("id"));
                    roleList.add((String) role.get("name"));
                    roleList.add(group);

                    retList.add(roleList);
                }
            }

            logger.trace_end(method);
            return retList;
        } catch (IOException | ParseException ex) {
            logger.catching("getUserRoles", ex);
            return null;
        }
    }

    /**
     * @api {post} /app/keycloak/users/:user/roles Add User Role
     * @apiVersion 1.0.0
     * @apiDescription Directly assign a role to specified user.
     * @apiGroup Keycloak
     * @apiUse AuthHeader
     * @apiParam {String} user user ID
     * @apiParam {JSONObject} inputString input JSON
     * @apiParam {String} inputString.id role ID
     * @apiParam {String} inputString.name role name
     *
     * @apiExample {curl} Example Call:
     * curl -X POST -d '{"id":"056af27f-b754-4287-aebe-129f5de8ab47","name":"Services"}'
     * http://localhost:8080/StackV-web/restapi/app/keycloak/users/1d183570-2798-4d69-80c3-490f926596ff/roles
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @POST
    @Path("/keycloak/users/{user}/roles")
    @Produces("application/json")
    @RolesAllowed("Keycloak")
    public void addUserRole(@PathParam("user") String subject, final String inputString) {
        try {
            String method = "addUserRole";
            logger.start(method);
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL(kc_url + "/admin/realms/StackV/users/" + subject + "/role-mappings/clients/" + keycloakStackVClientID + "");
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

            logger.trace("addUserRole", conn.getResponseCode() + " - " + conn.getResponseMessage(), "result");
        } catch (IOException | ParseException ex) {
            logger.catching("addUserRole", ex);
        }
    }

    /**
     * @api {delete} /app/keycloak/users/:user/roles Delete User Role
     * @apiVersion 1.0.0
     * @apiDescription Remove a directly assigned role from the specified user.
     * @apiGroup Keycloak
     * @apiUse AuthHeader
     * @apiParam {String} user user ID
     * @apiParam {JSONObject} inputString input JSON
     * @apiParam {String} inputString.id role ID
     * @apiParam {String} inputString.name role name
     *
     * @apiExample {curl} Example Call:
     * curl -X DELETE -d '{"id":"056af27f-b754-4287-aebe-129f5de8ab47","name":"Services"}'
     * http://localhost:8080/StackV-web/restapi/app/keycloak/users/1d183570-2798-4d69-80c3-490f926596ff/roles
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @DELETE
    @Path("/keycloak/users/{user}/roles")
    @Produces("application/json")
    @RolesAllowed("Keycloak")
    public void removeUserRole(@PathParam("user") String subject, final String inputString) {
        try {
            String method = "removeUserRole";
            logger.start(method);
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL(kc_url + "/admin/realms/StackV/users/" + subject + "/role-mappings/clients/" + keycloakStackVClientID);
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

            logger.trace("removeUserRole", conn.getResponseCode() + " - " + conn.getResponseMessage(), "result");
        } catch (IOException | ParseException ex) {
            logger.catching("removeUserRole", ex);
        }
    }

    // >Labels
    /**
     * @api {get} /app/label/:user
     * @apiVersion 1.0.0
     * @apiDescription Get a list of labels belonging to the specified user.
     * @apiGroup Labels
     * @apiUse AuthHeader
     * @apiParam {String} user user ID
     *
     ** @apiExample {curl} Example Call:
     * TODO - Add Example Call
     *
     * @apiSuccess Object return TODO - Add Return
     * @apiSuccessExample {json} Example Response:
     * TODO - Add Example Response
     */
    @GET
    @Path("/label/{user}")
    @Produces("application/json")
    @RolesAllowed("Labels")
    public ArrayList<ArrayList<String>> getLabels(@PathParam("user") String username) {
        try {
            String method = "getLabels";
            logger.trace_start(method);
            ArrayList<ArrayList<String>> retList = new ArrayList<>();

            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("SELECT * FROM label WHERE username = ?");
            prep.setString(1, username);
            ResultSet rs1 = prep.executeQuery();
            while (rs1.next()) {
                ArrayList<String> labelList = new ArrayList<>();

                labelList.add(rs1.getString("identifier"));
                labelList.add(rs1.getString("label"));
                labelList.add(rs1.getString("color"));

                retList.add(labelList);
            }

            logger.trace_end(method);
            return retList;
        } catch (SQLException ex) {
            logger.catching("getLabels", ex);
            return null;
        }
    }

    /**
     * @api {put} /app/label Add Label
     * @apiVersion 1.0.0
     * @apiDescription Add a new label.
     * @apiGroup Labels
     * @apiUse AuthHeader
     * @apiParam {String} user user ID
     * @apiParam {JSONObject} inputString TODO - Add Parameter Structure
     *
     ** @apiExample {curl} Example Call:
     * TODO - Add Example Call
     *
     * @apiSuccess Object return TODO - Add Return
     * @apiSuccessExample {json} Example Response:
     * TODO - Add Example Response
     */
    @PUT
    @Path(value = "/label")
    @Consumes(value = {"application/json", "application/xml"})
    @RolesAllowed("Labels")
    public String label(final String inputString) {
        try {
            String method = "label";
            logger.start(method);
            JSONObject inputJSON = new JSONObject();
            try {
                Object obj = parser.parse(inputString);
                inputJSON = (JSONObject) obj;

            } catch (ParseException ex) {
                logger.catching("label", ex);
            }

            String user = (String) inputJSON.get("user");
            String identifier = (String) inputJSON.get("identifier");
            String label = (String) inputJSON.get("label");
            String color = (String) inputJSON.get("color");

            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("INSERT INTO `frontend`.`label` (`identifier`, `username`, `label`, `color`) VALUES (?, ?, ?, ?)");
            prep.setString(1, identifier);
            prep.setString(2, user);
            prep.setString(3, label);
            prep.setString(4, color);
            prep.executeUpdate();

            logger.end(method);
            return "Added";
        } catch (SQLException ex) {
            logger.catching("label", ex);
            return "Failed";
        }
    }

    /**
     * @api {delete} /app/label/:username/delete/:identifier Delete Label
     * @apiVersion 1.0.0
     * @apiDescription Delete identified label owned by specified user.
     * @apiGroup Labels
     * @apiUse AuthHeader
     * @apiParam {String} username username
     * @apiParam {String} identifier label ID
     *
     ** @apiExample {curl} Example Call:
     * TODO - Add Example Call
     *
     * @apiSuccess Object return TODO - Add Return
     * @apiSuccessExample {json} Example Response:
     * TODO - Add Example Response
     */
    @DELETE
    @Path(value = "/label/{username}/delete/{identifier}")
    @RolesAllowed("Labels")
    public String deleteLabel(@PathParam(value = "username") String username, @PathParam(value = "identifier") String identifier) {
        try {
            String method = "deleteLabel";
            logger.start(method);
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("DELETE FROM `frontend` .`label` WHERE username = ? AND identifier = ?");
            prep.setString(1, username);
            prep.setString(2, identifier);
            prep.executeUpdate();

            logger.end(method);
            return "Deleted";
        } catch (SQLException ex) {
            logger.catching("deleteLabel", ex);
            return "Failed";
        }
    }

    /**
     * @api {delete} /app/label/:username/clearall Clear Labels
     * @apiVersion 1.0.0
     * @apiDescription Delete all labels owned by specified user.
     * @apiGroup Labels
     * @apiUse AuthHeader
     * @apiParam {String} username username
     *
     ** @apiExample {curl} Example Call:
     * TODO - Add Example Call
     *
     * @apiSuccess Object return TODO - Add Return
     * @apiSuccessExample {json} Example Response:
     * TODO - Add Example Response
     */
    @DELETE
    @Path(value = "/label/{username}/clearall")
    @RolesAllowed("Labels")
    public String clearLabels(@PathParam(value = "username") String username) {
        try {
            String method = "clearLabels";
            logger.start(method);
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("DELETE FROM `frontend`.`label` WHERE username = ? ");
            prep.setString(1, username);
            prep.executeUpdate();

            logger.end(method);
            return "Labels Cleared";
        } catch (SQLException ex) {
            logger.catching("clearLabels", ex);
            return "Failed";
        }
    }

    // >Logging
    /**
     * @api {get} /app/logging/ Get Logging
     * @apiVersion 1.0.0
     * @apiDescription Get system logging level.
     * @apiGroup Logging
     * @apiUse AuthHeader
     *
     ** @apiExample {curl} Example Call:
     * curl -k -v http://127.0.0.1:8080/StackV-web/restapi/app/logging/ -H "Authorization: bearer $KC_ACCESS_TOKEN"
     *
     * @apiSuccess String logging level
     * @apiSuccessExample {json} Example Response:
     * info
     */
    @GET
    @Path("/logging/")
    @Produces("application/json")
    @RolesAllowed("Logging")
    public String getLogLevel() {
        return logger.getLogger().getLevel().name();
    }

    /**
     * @api {put} /app/logging/:level Set Logging
     * @apiVersion 1.0.0
     * @apiDescription Set system logging level.
     * @apiGroup Logging
     * @apiUse AuthHeader
     * @apiParam {String} level logging level, one of the following: TRACE, DEBUG, INFO, WARN, ERROR
     *
     * @apiExample {curl} Example Call:
     * curl -X PUT -k -v http://127.0.0.1:8080/StackV-web/restapi/app/logging/trace -H "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @PUT
    @Path("/logging/{level}")
    @Produces("application/json")
    @RolesAllowed("Logging")
    public void setLogLevel(@PathParam("level") String level) {
        if (verifyUserRole("realm", "admin")) {
            switch (level) {
                case "TRACE":
                    Configurator.setLevel(WebResource.class.getName(), Level.TRACE);
                    Configurator.setLevel(SecurityInterceptor.class.getName(), Level.TRACE);
                    break;
                case "DEBUG":
                    Configurator.setLevel(WebResource.class.getName(), Level.DEBUG);
                    Configurator.setLevel(SecurityInterceptor.class.getName(), Level.DEBUG);
                    break;
                case "INFO":
                    Configurator.setLevel(WebResource.class.getName(), Level.INFO);
                    Configurator.setLevel(SecurityInterceptor.class.getName(), Level.INFO);
                    break;
                case "WARN":
                    Configurator.setLevel(WebResource.class.getName(), Level.WARN);
                    Configurator.setLevel(SecurityInterceptor.class.getName(), Level.WARN);
                    break;
                case "ERROR":
                    Configurator.setLevel(WebResource.class.getName(), Level.ERROR);
                    Configurator.setLevel(SecurityInterceptor.class.getName(), Level.ERROR);
                    break;
            }

            logger.status("setLogLevel", level);
        } else {
            logger.warning("setLogLevel", "User not authorized.");
        }
    }

    /**
     * @api {get} /app/logging/logs? Get Logs
     * @apiVersion 1.0.0
     * @apiDescription Get logs according to filters.
     * @apiGroup Logging
     * @apiUse AuthHeader
     * @apiParam
     *
     * @apiExample {curl} Example Call:
     * curl -k -v http://127.0.0.1:8080/StackV-web/restapi/app/logging/logs/e4d3bfd6-c269-4063-b02b-44aaef71d5b6 -H "Authorization: bearer $KC_ACCESS_TOKEN"
     *
     * @apiSuccess {JSONArray} logs logs JSON
     * @apiSuccess {JSONObject} logs.log log JSON
     * @apiSuccess {String} logs.log.marker log marker
     * @apiSuccess {String} logs.log.timestamp log timestamp
     * @apiSuccess {String} logs.log.level log level
     * @apiSuccess {String} logs.log.logger log source
     * @apiSuccess {String} logs.log.message log message
     * @apiSuccess {String} logs.log.exception log exception
     * @apiSuccessExample {json} Example Response:
     * [
     * {
     * "exception": "",
     * "level": "INFO",
     * "marker": "",
     * "logger": "net.maxgigapop.mrs.rest.api.WebResource",
     * "message": "Initialized.",
     * "timestamp": "2017-03-17 12:23:16.0"
     * },
     * ...]
     */
    @GET
    @Path("/logging/logs")
    @Produces("application/json")
    @RolesAllowed("Logging")
    public String getLogs(@QueryParam("refUUID") String refUUID, @QueryParam("level") String level) {
        String method = "getLogs";
        try {
            logger.trace_start(method);
            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep;
            // Filtering by UUID alone
            if (refUUID != null && level == null) {
                prep = front_conn.prepareStatement("SELECT * FROM log WHERE referenceUUID = ? ORDER BY timestamp DESC");
                prep.setString(1, refUUID);
            } // Filtering by level alone 
            else if (refUUID == null && level != null) {
                prep = front_conn.prepareStatement("SELECT * FROM log WHERE level = ? ORDER BY timestamp DESC");
                prep.setString(1, level);
            } // Filtering by both
            else if (refUUID != null && level != null) {
                prep = front_conn.prepareStatement("SELECT * FROM log WHERE referenceUUID = ? AND level = ? ORDER BY timestamp DESC");
                prep.setString(1, refUUID);
                prep.setString(2, level);
            } else {
                prep = front_conn.prepareStatement("SELECT * FROM log ORDER BY timestamp DESC");
            }

            ResultSet rs1 = prep.executeQuery();
            JSONArray logArr = new JSONArray();
            while (rs1.next()) {                
                JSONObject logJSON = new JSONObject();

                logJSON.put("marker", rs1.getString("marker"));
                logJSON.put("timestamp", rs1.getString("timestamp"));
                logJSON.put("level", rs1.getString("level"));
                logJSON.put("logger", rs1.getString("logger"));
                logJSON.put("message", rs1.getString("message"));
                logJSON.put("exception", rs1.getString("exception"));

                logArr.add(logJSON);
            }

            logger.trace_end(method);
            return logArr.toJSONString();
        } catch (SQLException ex) {
            logger.catching("getLogs", ex);
            return null;
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
     * TODO - Add Example Call
     *
     * @apiSuccess Object return TODO - Add Return
     * @apiSuccessExample {json} Example Response:
     * TODO - Add Example Response
     */
    @GET
    @Path("/manifest/{svcUUID}")
    @Produces("application/json")
    @RolesAllowed("Manifests")
    public String getManifest(@PathParam("svcUUID") String svcUUID) {
        logger.refuuid(svcUUID);
        String method = "getManifest";
        logger.trace_start(method);

        final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
        String serviceType = getServiceType(svcUUID);
        if (serviceType.equals("Virtual Cloud Network")) {
            try {
                URL url = new URL(String.format("%s/service/property/%s/host", host, svcUUID));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                String result = servBean.executeHttpMethod(url, conn, "GET", null, auth);
                if (result.equals("ops")) {
                    serviceType = "Virtual Cloud Network - OPS";
                } else if (result.equals("aws")) {
                    serviceType = "Virtual Cloud Network - AWS";
                } else {
                    throw new EJBException("cannot tell type of VCN service without 'host' property");
                }
            } catch (Exception ex) {
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

    @GET
    @Path("/manifest/{svcUUID}")
    @Produces("application/xml")
    @RolesAllowed("Manifests")
    public String getManifestXml(@PathParam("svcUUID") String svcUUID) {
        logger.refuuid(svcUUID);
        String manifestJStr = getManifest(svcUUID);
        org.json.JSONObject obj = new org.json.JSONObject(manifestJStr);
        String manifest = org.json.XML.toString(obj);
        return manifest;
    }

    // >Panels
    @GET
    @Path("/panel/{userId}/instances")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<ArrayList<String>> loadInstances(@PathParam("userId") String userId) {
        try {
            ArrayList<ArrayList<String>> retList = new ArrayList<>();
            ArrayList<String> banList = new ArrayList<>();
            String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");

            // Verify user
            String username = authUsername(userId);
            if (username == null) {
                logger.error("loadInstances", "Logged-in user does not match requested user information");
                return retList;
            }

            banList.add("Driver Management");

            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep;
            if (username.equals("admin")) {
                prep = front_conn.prepareStatement("SELECT DISTINCT S.name, I.referenceUUID, X.super_state, I.alias_name "
                        + "FROM service S, service_instance I, service_state X, acl A "
                        + "WHERE S.service_id = I.service_id AND I.service_state_id = X.service_state_id");
            } else {
                prep = front_conn.prepareStatement("SELECT DISTINCT S.name, I.referenceUUID, X.super_state, I.alias_name "
                        + "FROM service S, service_instance I, service_state X, acl A "
                        + "WHERE S.service_id = I.service_id AND I.service_state_id = X.service_state_id AND I.referenceUUID = A.object AND (A.subject = ? OR I.username = ?)");
                prep.setString(1, username);
                prep.setString(2, username);
            }
            ResultSet rs1 = prep.executeQuery();
            while (rs1.next()) {
                ArrayList<String> instanceList = new ArrayList<>();

                String instanceName = rs1.getString("name");
                String instanceUUID = rs1.getString("referenceUUID");
                String instanceSuperState = rs1.getString("super_state");
                String instanceAlias = rs1.getString("alias_name");
                if (!banList.contains(instanceName)) {
                    try {
                        URL url = new URL(String.format("%s/service/%s/status", host, instanceUUID));
                        HttpURLConnection status = (HttpURLConnection) url.openConnection();

                        String instanceState = instanceSuperState + " - " + servBean.executeHttpMethod(url, status, "GET", null, auth);

                        instanceList.add(instanceName);
                        instanceList.add(instanceUUID);
                        instanceList.add(instanceState);
                        instanceList.add(instanceAlias);

                        retList.add(instanceList);
                    } catch (IOException ex) {
                        logger.catching("loadInstances", ex);
                    }
                }
            }

            return retList;
        } catch (SQLException ex) {
            logger.catching("loadInstances", ex);
            return null;
        }
    }

    @GET
    @Path("/panel/{userId}/wizard")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<ArrayList<String>> loadWizard(@PathParam("userId") String userId) {
        try {
            ArrayList<ArrayList<String>> retList = new ArrayList<>();

            // Verify user
            String username = authUsername(userId);
            if (username == null) {
                logger.error("loadInstances", "Logged-in user does not match requested user information");
                return retList;
            }

            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);

            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep;
            if (username.equals("admin")) {
                prep = front_conn.prepareStatement("SELECT DISTINCT W.name, W.description, W.editable, W.service_wizard_id "
                        + "FROM service_wizard W");
            } else {
                prep = front_conn.prepareStatement("SELECT DISTINCT W.name, W.description, W.editable, W.service_wizard_id "
                        + "FROM service_wizard W WHERE W.username = ? OR W.username IS NULL");
                prep.setString(1, username);
            }
            ResultSet rs1 = prep.executeQuery();
            while (rs1.next()) {
                ArrayList<String> wizardList = new ArrayList<>();

                wizardList.add(rs1.getString("name"));
                wizardList.add(rs1.getString("description"));
                wizardList.add(rs1.getString("service_wizard_id"));

                retList.add(wizardList);
            }

            return retList;
        } catch (SQLException ex) {
            logger.catching("loadWizard", ex);
            return null;
        }
    }

    @GET
    @Path("/panel/{userId}/editor")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<ArrayList<String>> loadEditor(@PathParam("userId") String userId) {
        try {
            ArrayList<ArrayList<String>> retList = new ArrayList<>();

            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class.getName());
            AccessToken accessToken = securityContext.getToken();
            Set<String> roleSet = accessToken.getResourceAccess("StackV").getRoles();

            Connection front_conn;
            Properties front_connectionProps = new Properties();

            front_connectionProps.put(
                    "user", front_db_user);
            front_connectionProps.put(
                    "password", front_db_pass);

            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("SELECT DISTINCT S.name, S.filename, S.description FROM service S WHERE S.atomic = 0");
            ResultSet rs1 = prep.executeQuery();

            while (rs1.next()) {
                if (roleSet.contains(rs1.getString("filename"))) {
                    ArrayList<String> wizardList = new ArrayList<>();
                    wizardList.add(rs1.getString("name"));
                    wizardList.add(rs1.getString("description"));
                    wizardList.add(rs1.getString("filename"));

                    retList.add(wizardList);
                }
            }
            return retList;
        } catch (SQLException ex) {
            logger.catching("loadEditor", ex);
            return null;
        }
    }

    @GET
    @Path("/panel/{refUuid}/acl")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<String> loadObjectACL(@PathParam("refUuid") String refUuid) {
        try {
            ArrayList<String> retList = new ArrayList<>();
            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("SELECT A.subject FROM acl A WHERE A.object = ?");
            prep.setString(1, refUuid);
            ResultSet rs1 = prep.executeQuery();

            while (rs1.next()) {
                retList.add(rs1.getString("subject"));
            }
            return retList;
        } catch (SQLException ex) {
            logger.catching("loadObjectACL", ex);
            return null;
        }
    }

    @GET
    @Path("/panel/acl")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<String> loadSubjectACL() {
        try {
            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class.getName());
            AccessToken accessToken = securityContext.getToken();
            String username = accessToken.getPreferredUsername();

            ArrayList<String> retList = new ArrayList<>();
            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("SELECT A.object FROM acl A WHERE A.subject = ?");
            prep.setString(1, username);
            ResultSet rs1 = prep.executeQuery();

            while (rs1.next()) {
                retList.add(rs1.getString("subject"));
            }
            return retList;
        } catch (SQLException ex) {
            logger.catching("loadObjectACL", ex);
            return null;
        }
    }

    @GET
    @Path("/details/{uuid}/instance")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<String> loadInstanceDetails(@PathParam("uuid") String uuid) {
        logger.refuuid(uuid);
        try {
            ArrayList<String> retList = new ArrayList<>();

            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);

            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep;
            prep = front_conn.prepareStatement("SELECT S.name, I.creation_time, I.alias_name, X.super_state, V.verification_state FROM service S, service_instance I, service_state X, service_verification V "
                    + "WHERE I.referenceUUID = ? AND I.service_instance_id = V.service_instance_id AND S.service_id = I.service_id AND X.service_state_id = I.service_state_id");
            prep.setString(1, uuid);

            ResultSet rs1 = prep.executeQuery();
            while (rs1.next()) {
                retList.add(rs1.getString("verification_state"));
                retList.add(rs1.getString("name"));
                retList.add(rs1.getString("alias_name"));
                retList.add(rs1.getString("creation_time"));
                retList.add(rs1.getString("super_state"));
            }

            return retList;
        } catch (SQLException ex) {
            logger.catching("loadInstanceDetails", ex);
            return null;
        }
    }

    @GET
    @Path("/details/{uuid}/delta")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<ArrayList<String>> loadInstanceDelta(@PathParam("uuid") String uuid) {
        logger.refuuid(uuid);
        try {
            ArrayList<ArrayList<String>> retList = new ArrayList<>();

            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);

            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep;
            prep = front_conn.prepareStatement("SELECT D.service_delta_id, D.delta, D.type, S.super_state FROM service_delta D, service_instance I, service_state S, service_history H "
                    + "WHERE I.referenceUUID = ? AND I.service_instance_id = D.service_instance_id AND D.service_history_id = H.service_history_id AND D.service_instance_id = H.service_instance_id AND H.service_state_id = S.service_state_id");
            prep.setString(1, uuid);

            ResultSet rs1 = prep.executeQuery();
            while (rs1.next()) {
                ArrayList<String> deltaList = new ArrayList<>();
                deltaList.add(rs1.getString("type"));
                deltaList.add(rs1.getString("service_delta_id"));
                deltaList.add(rs1.getString("super_state"));
                deltaList.add(rs1.getString("delta"));
                retList.add(deltaList);
            }

            return retList;
        } catch (SQLException ex) {
            logger.catching("loadInstanceDelta", ex);
            return null;
        }
    }

    @GET
    @Path("/details/{uuid}/verification")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<String> loadInstanceVerification(@PathParam("uuid") String uuid) {
        logger.refuuid(uuid);
        try {
            ArrayList<String> retList = new ArrayList<>();

            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);

            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep;
            prep = front_conn.prepareStatement("SELECT V.service_instance_id, V.verification_run, V.creation_time, V.addition, V.reduction, V.verified_reduction, V.verified_addition, V.unverified_reduction, V.unverified_addition "
                    + "FROM service_verification V, service_instance I WHERE I.referenceUUID = ? AND V.service_instance_id = I.service_instance_id");
            prep.setString(1, uuid);

            ResultSet rs1 = prep.executeQuery();
            while (rs1.next()) {
                retList.add(rs1.getString("verification_run"));
                retList.add(rs1.getString("creation_time"));
                retList.add(rs1.getString("addition"));
                retList.add(rs1.getString("reduction"));
                retList.add(rs1.getString("service_instance_id"));
            }

            return retList;
        } catch (SQLException ex) {
            logger.catching("loadInstanceVerification", ex);
            return null;
        }
    }

    @GET
    @Path("/details/{uuid}/acl")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<String> loadInstanceACL(@PathParam("uuid") String uuid) {
        logger.refuuid(uuid);
        try {
            ArrayList<String> retList = new ArrayList<>();

            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);

            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep;
            prep = front_conn.prepareStatement("SELECT * FROM `acl`");

            ResultSet rs1 = prep.executeQuery();
            while (rs1.next()) {
            }

            return retList;
        } catch (SQLException ex) {
            logger.catching("loadInstanceACL", ex);
            return null;
        }
    }

    @GET
    @Path("/service/lastverify/{siUUID}")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public HashMap<String, String> getVerificationResults(@PathParam("siUUID") String serviceUUID) {
        logger.refuuid(serviceUUID);
        try {
            HashMap<String, String> retMap = new HashMap<>();
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("SELECT V.* FROM service_instance I, service_verification V WHERE I.referenceUUID = ? AND V.service_instance_id = I.service_instance_id");
            prep.setString(1, serviceUUID);
            ResultSet rs1 = prep.executeQuery();
            while (rs1.next()) {
                retMap.put("delta_uuid", rs1.getString("delta_uuid"));
                retMap.put("creation_time", rs1.getString("creation_time"));
                retMap.put("verified_reduction", rs1.getString("verified_reduction"));
                retMap.put("verified_addition", rs1.getString("verified_addition"));
                retMap.put("unverified_reduction", rs1.getString("unverified_reduction"));
                retMap.put("unverified_addition", rs1.getString("unverified_addition"));
                retMap.put("reduction", rs1.getString("reduction"));
                retMap.put("addition", rs1.getString("addition"));
            }

            return retMap;

        } catch (SQLException ex) {
            logger.catching("getVerificationResults", ex);
            return null;
        }
    }

    @GET
    @Path("/service/availibleitems/{siUUID}")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public String getVerificationResultsUnion(@PathParam("siUUID") String serviceUUID) throws Exception {
        logger.refuuid(serviceUUID);
        try {
            HashMap<String, String> retMap = new HashMap<>();
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("SELECT V.* FROM service_instance I, service_verification V WHERE I.referenceUUID = ? AND V.service_instance_id = I.service_instance_id");
            prep.setString(1, serviceUUID);
            ResultSet rs1 = prep.executeQuery();
            String verified_addition = "";
            String unverified_reduction = "";
            OntModel vAddition;
            OntModel uReduction;

            while (rs1.next()) {
                verified_addition = rs1.getString("verified_addition");
                unverified_reduction = rs1.getString("unverified_reduction");
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
            return null;
        }
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
     * @apiExample {curl} Example Call:
     * curl http://localhost:8080/StackV-web/restapi/app/profile/11
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     *
     * @apiSuccess {JSONObject} wizard_json Profile JSON
     * @apiSuccessExample {json} Example Response:
     * {"username": "admin","type": "netcreate","alias": "VCN.OPS.1VM_Ext.233","data": {"virtual_clouds": []}}
     */
    @GET
    @Path("/profile/{wizardID}")
    @Produces("application/json")
    @RolesAllowed("Profiles")
    public String getProfile(@PathParam("wizardID") int wizardID) {
        try {
            String method = "getProfile";
            logger.trace_start(method);
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("SELECT wizard_json FROM service_wizard WHERE service_wizard_id = ?");
            prep.setInt(1, wizardID);
            ResultSet rs1 = prep.executeQuery();
            while (rs1.next()) {
                return rs1.getString(1);
            }

            logger.trace_end(method);
            return "";
        } catch (SQLException ex) {
            logger.catching("getProfile", ex);
            return null;
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
     * @apiExample {curl} Example Call:
     * curl -X PUT -d @newprofile.json -H "Content-Type: application/json"
     * http://localhost:8080/StackV-web/restapi/app/profile/11/edit
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @PUT
    @Path("/profile/{wizardID}/edit")
    @RolesAllowed("Profiles")
    public void editProfile(@PathParam("wizardID") int wizardId, final String inputString) {
        try {
            String method = "editProfile";
            logger.start(method);
            // Connect to the DB
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend", front_connectionProps);

            // TODO: Sanitize the input!
            PreparedStatement prep = front_conn.prepareStatement("UPDATE service_wizard SET wizard_json = ? WHERE service_wizard_id = ? ");
            prep.setString(1, inputString);
            prep.setInt(2, wizardId);
            prep.executeUpdate();

            logger.end(method);
        } catch (SQLException ex) {
            logger.catching("editProfile", ex);
        }
    }

    /**
     * @api {put} /app/profile/new Add New Profile
     * @apiVersion 1.0.0
     * @apiDescription Save a new wizard profile.
     * @apiGroup Profile
     * @apiUse AuthHeader
     * @apiParam {JSONObject} inputString Profile JSON
     *
     * @apiExample {curl} Example Call:
     * curl -X PUT -d @newprofile.json -H "Content-Type: application/json"
     * http://localhost:8080/StackV-web/restapi/app/profile/11/edit
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @PUT
    @Path("/profile/new")
    @RolesAllowed("Profiles")
    public String newProfile(final String inputString) {
        try {
            String method = "newProfile";
            logger.start(method);
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend", front_connectionProps);
            Object obj = parser.parse(inputString);
            JSONObject inputJSON = (JSONObject) obj;
            String name = (String) inputJSON.get("name");
            String description = (String) inputJSON.get("description");
            String inputData = (String) inputJSON.get("data");

            Object obj2 = parser.parse(inputData);
            JSONObject dataJSON = (JSONObject) obj2;
            String username = authUsername((String) dataJSON.get("userID"));
            String type = (String) dataJSON.get("type");

            int serviceID = servBean.getServiceID(type);

            PreparedStatement prep = front_conn.prepareStatement("INSERT INTO `frontend`.`service_wizard` (service_id, username, name, wizard_json, description, editable) VALUES (?, ?, ?, ?, ?, ?)");
            prep.setInt(1, serviceID);
            prep.setString(2, username);
            prep.setString(3, name);
            prep.setString(4, inputData);
            prep.setString(5, description);
            prep.setInt(6, 0);
            prep.executeUpdate();

            logger.end(method);
            return null;
        } catch (SQLException | ParseException ex) {
            logger.catching("newProfile", ex);
            return ex.toString();
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
     * @apiExample {curl} Example Call:
     * curl -X DELETE http://localhost:8080/StackV-web/restapi/app/profile/11
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @DELETE
    @Path("/profile/{wizardId}")
    @RolesAllowed("Profiles")
    public void deleteProfile(@PathParam("wizardId") int wizardId) {
        try {
            String method = "deleteProfile";
            logger.start(method);
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("DELETE FROM service_wizard WHERE service_wizard_id = ?");
            prep.setInt(1, wizardId);
            prep.executeUpdate();

            logger.end(method);
        } catch (SQLException ex) {
            logger.catching("deleteProfile", ex);
        }
    }

    // >Services
    /**
     * @api {get} /app/service/:siUUID/status Check Status
     * @apiVersion 1.0.0
     * @apiDescription Retrieve full status of specified service instance.
     * @apiGroup Service
     * @apiUse AuthHeader
     * @apiParam {String} siUUID instance UUID
     *
     * @apiExample {curl} Example Call:
     * curl http://localhost:8080/StackV-web/restapi/app/service/49f3d197-de3e-464c-aaa8-d3fe5f14af0b/status
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     *
     * @apiSuccess {String} status Instance status composite, containing both superstate and substate
     * @apiSuccessExample {String} Example Response:
     * Cancel - FAILED
     */
    @GET
    @Path("/service/{siUUID}/status")
    @RolesAllowed("Services")
    public String checkStatus(@PathParam("siUUID") String svcInstanceUUID) {
        logger.refuuid(svcInstanceUUID);
        String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        if (refresh != null) {
            auth = servBean.refreshToken(refresh);
        }

        try {
            Thread.sleep(300);
            return superStatus(svcInstanceUUID) + " - " + status(svcInstanceUUID, auth) + "\n";
        } catch (IOException | InterruptedException ex) {
            logger.catching("checkStatus", ex);
            return null;
        }
    }

    /**
     * @api {get} /app/service/:siUUID/substatus Check Substatus
     * @apiVersion 1.0.0
     * @apiDescription Retrieve substatus of specified service instance.
     * @apiGroup Service
     * @apiUse AuthHeader
     * @apiParam {String} siUUID instance UUID
     *
     * @apiExample {curl} Example Call:
     * curl http://localhost:8080/StackV-web/restapi/app/service/49f3d197-de3e-464c-aaa8-d3fe5f14af0b/substatus
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     *
     * @apiSuccess {String} status Instance substatus
     * @apiSuccessExample {String} Example Response:
     * FAILED
     */
    @GET
    @Path("/service/{siUUID}/substatus")
    @RolesAllowed("Services")
    public String subStatus(@PathParam("siUUID") String svcInstanceUUID) {
        logger.refuuid(svcInstanceUUID);
        String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        if (refresh != null) {
            auth = servBean.refreshToken(refresh);
        }

        try {
            Thread.sleep(300);
            return status(svcInstanceUUID, auth);
        } catch (IOException | InterruptedException ex) {
            logger.catching("subStatus", ex);
        }
        return null;
    }

    /**
     * @api {post} /app/service Create Service
     * @apiVersion 1.0.0
     * @apiDescription Create new service instance.
     * @apiGroup Service
     * @apiUse AuthHeader
     * @apiParam {JSONObject} inputString service JSON
     *
     * @apiExample {curl} Example Call:
     * curl -X POST -d @newservice.json -H "Content-Type: application/json"
     * http://localhost:8080/StackV-web/restapi/app/service
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @POST
    @Path(value = "/service")
    @Consumes(value = {"application/json", "application/xml"})
    @RolesAllowed("Services")
    public void createService(@Suspended
            final AsyncResponse asyncResponse, final String inputString) {
        try {
            logger.start("createService");
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
            Object obj = parser.parse(inputString);
            final JSONObject inputJSON = (JSONObject) obj;
            String serviceType = (String) inputJSON.get("type");

            // Authorize service.
            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class
                    .getName());
            final AccessToken accessToken = securityContext.getToken();
            Set<String> roleSet = accessToken.getResourceAccess("StackV").getRoles();

            if (roleSet.contains(serviceType)) {
                String username = accessToken.getPreferredUsername();
                inputJSON.remove("username");
                inputJSON.put("username", username);

                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        asyncResponse.resume(doCreateService(inputJSON, auth, refresh));
                    }
                });
            } else {
                logger.warning("createService", "User not allowed access to " + serviceType);
            }

        } catch (ParseException ex) {
            logger.catching("createService", ex);
        }
    }

    /**
     * @api {put} /app/service/:siUUID/:action Operate Service
     * @apiVersion 1.0.0
     * @apiDescription Operate on the specified service instance.
     * @apiGroup Service
     * @apiUse AuthHeader
     * @apiParam {String} siUUID instance UUID
     * @apiParam {String} action operation to execute
     *
     * @apiExample {curl} Example Call:
     * curl -X PUT http://localhost:8080/StackV-web/restapi/app/service/49f3d197-de3e-464c-aaa8-d3fe5f14af0b/cancel
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @PUT
    @Path(value = "/service/{siUUID}/{action}")
    @RolesAllowed("Services")
    public void operate(@Suspended
            final AsyncResponse asyncResponse, @PathParam(value = "siUUID")
            final String refUuid, @PathParam(value = "action")
            final String action) {
        final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                asyncResponse.resume(doOperate(refUuid, action, auth, refresh));
            }
        });
    }

    // Async Methods -----------------------------------------------------------
    private String doCreateService(JSONObject inputJSON, String auth, String refresh) {
        try {
            long startTime = System.currentTimeMillis();
            logger.start("doCreateService");

            String serviceType = (String) inputJSON.get("type");
            String alias = (String) inputJSON.get("alias");
            String username = (String) inputJSON.get("username");

            JSONObject dataJSON = (JSONObject) inputJSON.get("data");

            // Find user ID.
            Connection front_conn;
            try {
                Class.forName("com.mysql.jdbc.Driver").newInstance();

            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
                logger.catching("doCreateService", ex);
            }

            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            // Instance Creation
            URL url = new URL(String.format("%s/service/instance", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Authorization", auth);
            String refUUID = servBean.executeHttpMethod(url, connection, "GET", null, auth);
            logger.refuuid(refUUID);

            // Create Parameter Map
            HashMap<String, String> paraMap = new HashMap<>();
            switch (serviceType) {
                case "dnc":
                    paraMap = parseDNC(dataJSON, refUUID);
                    break;
                case "netcreate":
                    paraMap = parseNet(dataJSON, refUUID);
                    break;
                case "fl2p":
                    paraMap = parseFlow(dataJSON, refUUID);
                    break;
                case "hybridcloud":
                    paraMap = parseHybridCloud(dataJSON, refUUID);
                    break;
                case "omm":
                    paraMap = parseOperatationalModifications(dataJSON, refUUID);
                    break;
                default:
            }

            // Initialize service parameters.
            PreparedStatement prep = front_conn.prepareStatement("SELECT service_id"
                    + " FROM service WHERE filename = ?");
            prep.setString(1, serviceType);
            ResultSet rs1 = prep.executeQuery();
            int serviceID = -1;
            while (rs1.next()) {
                serviceID = rs1.getInt(1);
            }
            Timestamp timeStamp = new Timestamp(System.currentTimeMillis());

            // Install Instance into DB.
            prep = front_conn.prepareStatement("INSERT INTO frontend.service_instance "
                    + "(`service_id`, `username`, `creation_time`, `referenceUUID`, `alias_name`, `service_state_id`) VALUES (?, ?, ?, ?, ?, ?)");
            prep.setInt(1, serviceID);
            prep.setString(2, username);
            prep.setTimestamp(3, timeStamp);
            prep.setString(4, refUUID);
            prep.setString(5, alias);
            prep.setInt(6, 1);
            prep.executeUpdate();

            int instanceID = servBean.getInstanceID(refUUID);

            prep = front_conn.prepareStatement("INSERT INTO `frontend`.`service_history` "
                    + "(`service_state_id`, `service_instance_id`) VALUES (1, ?)");
            prep.setInt(1, instanceID);
            prep.executeUpdate();

            prep = front_conn.prepareStatement("INSERT INTO `frontend`.`service_verification` "
                    + "(`service_instance_id`) VALUES (?)");
            prep.setInt(1, instanceID);
            prep.executeUpdate();

            prep = front_conn.prepareStatement("INSERT INTO `frontend`.`acl` (`subject`, `is_group`, `object`) "
                    + "VALUES (?, '0', ?)");
            prep.setString(1, username);
            prep.setString(2, refUUID);
            prep.executeUpdate();

            logger.init();

            // Execute service creation.
            switch (serviceType) {
                case "netcreate":
                    servBean.createNetwork(paraMap, auth, refresh);
                    break;
                case "hybridcloud":
                    servBean.createHybridCloud(paraMap, auth, refresh);
                    break;
                case "omm":
                    servBean.createOperationModelModification(paraMap, auth);
                    break;
                default:
            }

            long endTime = System.currentTimeMillis();
            // Return instance UUID
            logger.end("doCreateService");
            return refUUID;

        } catch (EJBException | SQLException | IOException ex) {
            logger.catching("doCreateService", ex);
            return "<<<CREATION ERROR: " + ex.getMessage();
        }
    }

    private String doOperate(@PathParam("siUUID") String refUUID, @PathParam("action") String action, String auth, String refresh) {
        logger.refuuid(refUUID);
        logger.start("doOperate:" + action);

        try {
            clearVerification(refUUID);
            switch (action) {
                case "cancel":
                    setSuperState(refUUID, 2);
                    cancelInstance(refUUID, auth, refresh);
                    break;
                case "force_cancel":
                    setSuperState(refUUID, 2);
                    forceCancelInstance(refUUID, auth, refresh);
                    break;

                case "reinstate":
                    setSuperState(refUUID, 4);
                    cancelInstance(refUUID, auth, refresh);
                    break;
                case "force_reinstate":
                    setSuperState(refUUID, 4);
                    forceCancelInstance(refUUID, auth, refresh);
                    break;

                case "force_retry":
                    forceRetryInstance(refUUID, auth, refresh);
                    break;

                case "delete":
                case "force_delete":
                    deleteInstance(refUUID, auth);

                    logger.end("doOperate:" + action);
                    return "Deletion Complete.\r\n";

                case "verify":
                case "reverify":
                    servBean.verify(refUUID, refresh);

                    logger.end("doOperate:" + action);
                    return "Verification Complete.\r\n";

                default:
                    logger.warning("doOperate", "Invalid action: " + action);
            }

            auth = servBean.refreshToken(refresh);
            String retString = superStatus(refUUID) + " - " + status(refUUID, auth) + "\r\n";

            logger.end("doOperate:" + action, retString);
            return retString;
        } catch (IOException | SQLException | InterruptedException | EJBException ex) {
            try {
                Connection front_conn;
                Properties front_connectionProps = new Properties();
                front_connectionProps.put("user", front_db_user);
                front_connectionProps.put("password", front_db_pass);
                front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                        front_connectionProps);
                PreparedStatement prep;
                prep = front_conn.prepareStatement("UPDATE service_verification V INNER JOIN service_instance I SET V.verification_state = '-1' WHERE V.service_instance_id = I.service_instance_id AND I.referenceUUID = ?");
                prep.setString(1, refUUID);
                prep.executeUpdate();
            } catch (SQLException ex2) {
                logger.catching("doOperate", ex2);
            }
            logger.catching("doOperate", ex);
            return "<<<OPERATION ERROR - " + action + ": " + ex.getMessage() + "\r\n";
        }
    }

    @GET
    @Path("/delta/{siUUID}")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public String getDeltaBacked(@PathParam("siUUID") String serviceUUID) {
        String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
        try {
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("SELECT COUNT(*) FROM service_delta D, service_instance I WHERE D.service_instance_id = I.service_instance_id AND I.referenceUUID = ?");
            prep.setString(1, serviceUUID);
            ResultSet rs1 = prep.executeQuery();
            rs1.next();

            if (rs1.getInt(1) > 0) {
                URL url = new URL(String.format("%s/service/delta/%s", host, serviceUUID));
                HttpURLConnection status = (HttpURLConnection) url.openConnection();
                String result = servBean.executeHttpMethod(url, status, "GET", null, auth);

                return result;
            } else {
                return "{verified_addition: \"{ }\",verified_reduction: \"{ }\",unverified_addition: \"{ }\",unverified_reduction: \"{ }\"}";
            }
        } catch (IOException | SQLException ex) {
            logger.catching("getDeltaBacked", ex);
            return null;
        }
    }

    // Operation Methods -------------------------------------------------------
    /**
     * Deletes a service instance.
     *
     * @param refUuid instance UUID
     * @return error code |
     */
    private int deleteInstance(String refUuid, String auth) throws SQLException, IOException {
        String result = delete(refUuid, auth);
        if (result.equalsIgnoreCase("Successfully terminated")) {
            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("DELETE FROM `frontend`.`service_instance` WHERE `service_instance`.`referenceUUID` = ?");
            prep.setString(1, refUuid);
            prep.executeUpdate();

            prep = front_conn.prepareStatement("DELETE FROM `frontend`.`acl` WHERE `acl`.`object` = ?");
            prep.setString(1, refUuid);
            prep.executeUpdate();

            return 0;
        } else {
            return 1;
        }
    }

    /**
     * Cancels a service instance. Requires instance to be in 'ready' substate.
     *
     * @param refUuid instance UUID
     * @return error code | -1: Exception thrown. 0: success. 1: stage 1 error
     * (Failed pre-condition). 2: stage 2 error (Failed revert). 3: stage 3
     * error (Failed propagate). 4: stage 4 error (Failed commit). 5: stage 5
     * error (Failed result check).
     */
    private int cancelInstance(String refUuid, String auth, String refresh) throws SQLException, IOException, MalformedURLException, InterruptedException {
        boolean result;
        String instanceState = status(refUuid, auth);
        try {
            if (!instanceState.equalsIgnoreCase("READY")) {
                return 1;
            }

            auth = servBean.refreshToken(refresh);
            result = revert(refUuid, auth);
            if (!result) {
                return 2;
            }

            auth = servBean.refreshToken(refresh);
            result = propagate(refUuid, auth);
            if (!result) {
                return 3;
            }

            auth = servBean.refreshToken(refresh);
            result = commit(refUuid, auth);
            if (!result) {
                return 4;
            }

            while (true) {
                auth = servBean.refreshToken(refresh);

                instanceState = status(refUuid, auth);
                if (instanceState.equals("READY") || instanceState.equals("FAILED")) {
                    servBean.verify(refUuid, refresh);

                    return 0;
                } else if (!(instanceState.equals("COMMITTED"))) {
                    return 5;
                }

                Thread.sleep(5000);
            }
        } catch (EJBException ex) {
            logger.catching("cancelInstance", ex);
            return -1;
        }
    }

    private int forceCancelInstance(String refUuid, String auth, String refresh) throws SQLException, IOException, MalformedURLException, InterruptedException {
        boolean result;
        try {
            forceRevert(refUuid, auth);

            auth = servBean.refreshToken(refresh);
            forcePropagate(refUuid, auth);

            auth = servBean.refreshToken(refresh);
            forceCommit(refUuid, auth);

            for (int i = 0; i < 20; i++) {
                auth = servBean.refreshToken(refresh);
                String instanceState = status(refUuid, auth);
                if (instanceState.equals("READY") || instanceState.equals("FAILED")) {
                    servBean.verify(refUuid, refresh);

                    return 0;
                } else if (!(instanceState.equals("COMMITTED"))) {
                    return 5;
                }
                Thread.sleep(5000);
            }
            return -1;
        } catch (EJBException ex) {
            logger.catching("forceCancelInstance", ex);
            return -1;
        }
    }

    private int forceRetryInstance(String refUuid, String auth, String refresh) throws SQLException, IOException, MalformedURLException, InterruptedException {
        boolean result;
        forcePropagate(refUuid, auth);

        auth = servBean.refreshToken(refresh);
        forceCommit(refUuid, auth);

        for (int i = 0; i < 20; i++) {
            auth = servBean.refreshToken(refresh);
            String instanceState = status(refUuid, auth);
            if (instanceState.equals("READY")) {
                servBean.verify(refUuid, refresh);

                return 0;
            } else if (!(instanceState.equals("COMMITTED") || instanceState.equals("FAILED"))) {
                return 5;
            }
            Thread.sleep(5000);
        }
        return -1;
    }

    // Parsing Methods ---------------------------------------------------------
    // @TODO: PRETTY MUCH UNDOING SERVLET CODE?
    private HashMap<String, String> parseDNC(JSONObject dataJSON, String refUuid) {
        HashMap<String, String> paraMap = new HashMap<>();
        paraMap.put("instanceUUID", refUuid);

        JSONArray linksArr = (JSONArray) dataJSON.get("links");
        for (int i = 1; i <= linksArr.size(); i++) {
            JSONObject linksJSON = (JSONObject) linksArr.get(i - 1);
            String name = (String) linksJSON.get("name");
            String src = (String) linksJSON.get("src");
            String srcVlan = (String) linksJSON.get("src-vlan");
            String des = (String) linksJSON.get("des");
            String desVlan = (String) linksJSON.get("des-vlan");

            String linkUrn = servBean.urnBuilder("dnc", name, refUuid);

            paraMap.put("linkUri" + i, linkUrn);
            paraMap.put("src-conn" + i, src);
            paraMap.put("des-conn" + i, des);
            paraMap.put("src-vlan" + i, srcVlan);
            paraMap.put("des-vlan" + i, desVlan);
        }

        return paraMap;
    }

    private HashMap<String, String> parseNet(JSONObject dataJSON, String refUuid) {
        HashMap<String, String> paraMap = new HashMap<>();
        paraMap.put("instanceUUID", refUuid);

        JSONArray vcnArr = (JSONArray) dataJSON.get("virtual_clouds");
        JSONObject vcnJSON = (JSONObject) vcnArr.get(0);
        paraMap.put("netType", (String) vcnJSON.get("type"));
        paraMap.put("netCidr", (String) vcnJSON.get("cidr"));

        String parent = (String) vcnJSON.get("parent");
        paraMap.put("topoUri", parent);

        // Parse Subnets.
        JSONArray subArr = (JSONArray) vcnJSON.get("subnets");
        int vmCounter = 1;
        for (int i = 0; i < subArr.size(); i++) {
            JSONObject subJSON = (JSONObject) subArr.get(i);

            String subName = (String) subJSON.get("name");
            String subCidr = (String) subJSON.get("cidr");

            // Parse VMs.
            JSONArray vmArr = (JSONArray) subJSON.get("virtual_machines");
            if (vmArr != null) {
                for (Object vmEle : vmArr) {
                    //value format: "vm_name & subnet_index_number & type_detail & host & interfaces"
                    JSONObject vmJSON = (JSONObject) vmEle;

                    // Name
                    String vmString = (String) vmJSON.get("name");
                    // Subnet Index
                    vmString += "&" + (i + 1);

                    // TYPES
                    vmString += vmJSON.containsKey("type") ? "&" + (String) vmJSON.get("type") : "& ";

                    // VM Host
                    vmString += vmJSON.containsKey("host") ? "&" + (String) vmJSON.get("host") : "& ";

                    // INTERFACES
                    if (vmJSON.containsKey("interfaces")) {
                        JSONArray interfaceArr = (JSONArray) vmJSON.get("interfaces");
                        if (!interfaceArr.isEmpty()) {
                            vmString += "&" + interfaceArr.toString();
                        } else {
                            vmString += "& ";
                        }
                    } else {
                        vmString += "& ";
                    }
                    // VM Routes
                    if (vmJSON.containsKey("routes")) {
                        JSONArray routeArr = (JSONArray) vmJSON.get("routes");
                        if (!routeArr.isEmpty()) {
                            vmString += "&" + routeArr.toString();
                        } else {
                            vmString += "& ";
                        }
                    } else {
                        vmString += "& ";
                    }

                    // CephRBD
                    if (vmJSON.containsKey("ceph_rbd")) {
                        JSONArray rbdArr = (JSONArray) vmJSON.get("ceph_rbd");
                        if (!rbdArr.isEmpty()) {
                            vmString += "&" + rbdArr.toString();
                        } else {
                            vmString += "& ";
                        }
                    } else {
                        vmString += "& ";
                    }

                    // Globus
                    if (vmJSON.containsKey("globus_connect")) {
                        JSONObject globusJSON = (JSONObject) vmJSON.get("globus_connect");
                        vmString += "&" + globusJSON.toString();
                    } else {
                        vmString += "& ";
                    }

                    // NFS
                    if (vmJSON.containsKey("nfs")) {
                        JSONObject nfsJSON = (JSONObject) vmJSON.get("nfs");
                        vmString += "&" + nfsJSON.toString();
                    } else {
                        vmString += "& ";
                    }

                    paraMap.put("vm" + vmCounter++, vmString);
                }
            }

            // Parse subroutes.
            JSONArray subRouteArr = (JSONArray) subJSON.get("routes");
            String routeString = "";
            if (subRouteArr != null) {
                routeString = "routes";
                for (Object routeEle : subRouteArr) {
                    JSONObject routeJSON = (JSONObject) routeEle;

                    JSONObject fromJSON = (JSONObject) routeJSON.get("from");
                    if (fromJSON != null) {
                        routeString += "from+" + fromJSON.get("value") + ",";
                    }

                    JSONObject toJSON = (JSONObject) routeJSON.get("to");
                    if (toJSON != null) {
                        routeString += "to+" + toJSON.get("value") + ",";
                    }

                    JSONObject nextJSON = (JSONObject) routeJSON.get("next_hop");
                    if (nextJSON != null) {
                        routeString += "nextHop+" + nextJSON.get("value");
                    }

                    routeString += "\r\n";
                }

                if (!routeString.equals("routes")) {
                    routeString = routeString.substring(0, routeString.length() - 2);
                }
            }

            String subString = "name+" + subName + "&cidr+" + subCidr;
            if (!routeString.equals("routes")) {
                subString += "&" + routeString;
            }

            paraMap.put("subnet" + (i + 1), subString);
        }

        // Parse Network Routes.
        JSONArray netRouteArr = (JSONArray) vcnJSON.get("routes");
        String netRouteString = "";
        if (netRouteArr != null) {
            for (Object routeEle : netRouteArr) {
                JSONObject routeJSON = (JSONObject) routeEle;

                JSONObject fromJSON = (JSONObject) routeJSON.get("from");
                if (fromJSON != null) {
                    netRouteString += "from+" + fromJSON.get("value") + ",";
                }

                JSONObject toJSON = (JSONObject) routeJSON.get("to");
                if (toJSON != null) {
                    netRouteString += "to+" + toJSON.get("value") + ",";
                }

                JSONObject nextJSON = (JSONObject) routeJSON.get("next_hop");
                if (nextJSON != null) {
                    netRouteString += "nextHop+" + nextJSON.get("value");
                }

                netRouteString += "\r\n";
            }
        }
        paraMap.put("netRoutes", netRouteString);

        // Parse Gateways.
        if (vcnJSON.get("gateways") != null) {
            JSONArray gatewayArr = (JSONArray) vcnJSON.get("gateways");
            paraMap.put("gateways", gatewayArr.toString());
        }

        return paraMap;
    }

    private HashMap<String, String> parseHybridCloud(JSONObject dataJSON, String refUuid) {
        HashMap<String, String> paraMap = new HashMap<>();
        paraMap.put("instanceUUID", refUuid);
        paraMap.put("virtual_clouds", dataJSON.get("virtual_clouds").toString());

        return paraMap;
    }

    private HashMap<String, String> parseFlow(JSONObject dataJSON, String refUuid) {
        HashMap<String, String> paraMap = new HashMap<>();
        paraMap.put("instanceUUID", refUuid);

        JSONObject flowJSON = (JSONObject) dataJSON.get("flow");
        String name = (String) flowJSON.get("name");
        String src = (String) flowJSON.get("src");
        String des = (String) flowJSON.get("des");

        String flowUrn = servBean.urnBuilder("flow", name, refUuid);
        paraMap.put("topUri", flowUrn);
        paraMap.put("eth_src", src);
        paraMap.put("eth_des", des);

        return paraMap;
    }

    private HashMap<String, String> parseOperatationalModifications(JSONObject dataJSON, String refUuid) {
        HashMap<String, String> paraMap = new HashMap<>();
        paraMap.put("instanceUUID", refUuid);

        // { "modification" : "params", .. }
        Iterator<?> keys = dataJSON.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            //if ( dataJSON.get(key) instanceof JSONObject ) {
            paraMap.put(key, dataJSON.get(key).toString());
            //}
        }

        return paraMap;
    }

    // Utility Methods ---------------------------------------------------------
    private void setSuperState(String refUuid, int superStateId) {
        try {
            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("UPDATE service_instance SET service_state_id = ? "
                    + "WHERE referenceUUID = ?");
            prep.setInt(1, superStateId);
            prep.setString(2, refUuid);
            prep.executeUpdate();

            int instanceID = servBean.getInstanceID(refUuid);

            prep = front_conn.prepareStatement("INSERT INTO `frontend`.`service_history` (`service_state_id`, `service_instance_id`) "
                    + "VALUES (?, ?)");
            prep.setInt(1, superStateId);
            prep.setInt(2, instanceID);
            prep.executeUpdate();
        } catch (SQLException ex) {
            logger.catching("setSupereState", ex);
        }
    }

    private boolean propagate(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/propagate", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, propagate, "PUT", null, auth);
        //logger.log(Level.INFO, "Sending Propagate Command");
        //logger.log(Level.INFO, "Response Code : {0}", result);

        return result.equalsIgnoreCase("PROPAGATED");
    }

    private boolean forcePropagate(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/propagate_forcedretry", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, propagate, "PUT", null, auth);
        //logger.log(Level.INFO, "Sending Forced Propagate Command");
        //logger.log(Level.INFO, "Response Code : {0}", result);

        return result.equalsIgnoreCase("PROPAGATED");
    }

    private boolean commit(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/commit", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, propagate, "PUT", null, auth);
        //logger.log(Level.INFO, "Sending Commit Command");
        //logger.log(Level.INFO, "Response Code : {0}", result);

        return result.equalsIgnoreCase("COMMITTED");
    }

    private boolean forceCommit(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/commit_forced", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, propagate, "PUT", null, auth);
        //logger.log(Level.INFO, "Sending Forced Commit Command");
        //logger.log(Level.INFO, "Response Code : {0}", result);

        return result.equalsIgnoreCase("COMMITTED");
    }

    private boolean revert(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/revert", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, propagate, "PUT", null, auth);
        //logger.log(Level.INFO, "Sending Revert Command");
        //logger.log(Level.INFO, "Response Code : {0}", result);

        // Revert now returns service delta UUID; pending changes.
        return true;
    }

    private boolean forceRevert(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/revert_forced", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, propagate, "PUT", null, auth);
        //logger.log(Level.INFO, "Sending Forced Revert Command");
        //logger.log(Level.INFO, "Response Code : {0}", result);

        // Revert now returns service delta UUID; pending changes.
        return true;
    }

    private String delete(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, propagate, "DELETE", null, auth);
        //logger.log(Level.INFO, "Sending Delete Command");
        //logger.log(Level.INFO, "Response Code : {0}", result);

        return result;
    }

    private boolean clearVerification(String refUuid) {
        try {
            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);
            PreparedStatement prep;

            int instanceID = servBean.getInstanceID(refUuid);

            prep = front_conn.prepareStatement("UPDATE `frontend`.`service_verification` SET `verification_state` = ? WHERE `service_verification`.`service_instance_id` = ?");
            prep.setNull(1, java.sql.Types.INTEGER);
            prep.setInt(2, instanceID);
            prep.executeUpdate();

            return true;
        } catch (SQLException ex) {
            logger.catching("clearVerification", ex);
            return false;
        }
    }

    private String superStatus(String refUuid) {
        try {
            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("SELECT X.super_state FROM"
                    + " service_instance I, service_state X WHERE I.referenceUUID = ? AND I.service_state_id = X.service_state_id");
            prep.setString(1, refUuid);
            ResultSet rs1 = prep.executeQuery();
            while (rs1.next()) {
                return rs1.getString("X.super_state");
            }
            return "ERROR";
        } catch (SQLException ex) {
            logger.catching("superStatus", ex);
            return "ERROR";
        }
    }

    private String status(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/status", host, refUuid));
        HttpURLConnection status = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, status, "GET", null, auth);

        return result;
    }

    private String getServiceType(String refUuid) {
        Connection front_conn;
        Properties front_connectionProps = new Properties();
        front_connectionProps.put("user", front_db_user);
        front_connectionProps.put("password", front_db_pass);
        try {
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("select S.name from service_instance I, service S "
                    + "where I.referenceUUID=? AND I.service_id=S.service_id");
            prep.setString(1, refUuid);
            ResultSet rs1 = prep.executeQuery();
            while (rs1.next()) {
                return rs1.getString("name");
            }
        } catch (SQLException ex) {
            //Logger.getLogger(WebResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        throw new EJBException("getServiceType failed to find service type for service uuid=" + refUuid);
    }

    private String resolveManifest(String refUuid, String jsonTemplate, String auth) {
        try {
            URL url = new URL(String.format("%s/service/manifest/%s", host, refUuid));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String data = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<serviceManifest>\n<serviceUUID/>\n<jsonTemplate>\n%s</jsonTemplate>\n</serviceManifest>",
                    jsonTemplate);
            String result = servBean.executeHttpMethod(url, conn, "POST", data, auth);
            return result;
        } catch (Exception ex) {
            throw new EJBException("resolveManifest cannot fetch manifest for service uuid=" + refUuid, ex);
        }
    }

    private String authUsername(String subject) {
        KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class
                .getName());
        AccessToken accessToken = securityContext.getToken();
        if (accessToken.getSubject().equals(subject)) {
            return accessToken.getPreferredUsername();
        } else {
            return null;
        }
    }

    private String JSONtoxml(JSONObject JSONdata, String drivertype) {
        String xmldata = "<driverInstance><properties>\n";
        xmldata += "\t<entry><key>topologyUri</key><value>" + JSONdata.get("TOPURI") + "</value></entry>\n";
        xmldata += "\t<entry><key>driverEjbPath</key><value>java:module/" + drivertype + "</value></entry>\n";

        Set<String> key = new HashSet<>(JSONdata.keySet());
        for (String i : key) {
            if (!(i.equals("TOPURI")) && !(i.equals("drivertype"))) {
                xmldata += "\t<entry><key>" + i + "</key><value>" + JSONdata.get(i) + "</value></entry>\n";
            }
        }
        xmldata += "</properties></driverInstance>";
        return xmldata;
    }

    private boolean verifyUserRole(String scope, String role) {
        KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class
                .getName());
        final AccessToken accessToken = securityContext.getToken();

        Set<String> roleSet;
        if (scope.equals("realm")) {
            roleSet = accessToken.getRealmAccess().getRoles();
        } else {
            roleSet = accessToken.getResourceAccess("StackV").getRoles();
        }

        return roleSet.contains(role);
    }
}
