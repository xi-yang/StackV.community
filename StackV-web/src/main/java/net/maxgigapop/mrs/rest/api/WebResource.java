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
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.QueryParam;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.service.ServiceHandler;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.common.TokenHandler;
import org.apache.commons.dbutils.DbUtils;
import org.apache.logging.log4j.Level;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.apache.logging.log4j.core.config.Configurator;
import org.jboss.resteasy.spi.UnhandledException;

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

        result.put("driver", Arrays.asList(
                "Driver Management",
                "Installation and Uninstallation of Driver Instances."));

        result.put("netcreate", Arrays.asList(
                "Virtual Cloud Network",
                "Network Creation Pilot Testbed."));

        result.put("dnc", Arrays.asList(
                "Dynamic Network Connection",
                "Creation of new network connections."));

        result.put("hybridcloud", Arrays.asList(
                "Advanced Hybrid Cloud",
                "Advanced Hybrid Cloud Service."));

        return Collections.unmodifiableMap(result);
    }

    private final StackLogger logger = new StackLogger(WebResource.class.getName(), "WebResource");

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
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            String method = "addACLEntry";
            logger.start(method);
            // Authorize service.
            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class
                    .getName());
            final AccessToken accessToken = securityContext.getToken();

            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("INSERT INTO `frontend`.`acl` (`subject`, `is_group`, `object`) "
                    + "VALUES (?, '0', ?)");
            prep.setString(1, subject);
            prep.setString(2, refUUID);
            prep.executeUpdate();

            logger.end(method);
        } catch (SQLException ex) {
            logger.catching("addACLEntry", ex);
        } finally {
            commonsClose(front_conn, prep, rs);
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
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            String method = "removeACLEntry";
            logger.start(method);
            // Authorize service.
            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class
                    .getName());
            final AccessToken accessToken = securityContext.getToken();

            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("DELETE FROM `frontend`.`acl` WHERE subject = ? AND object = ?");
            prep.setString(1, subject);
            prep.setString(2, refUUID);
            prep.executeUpdate();

            logger.end(method);
        } catch (SQLException ex) {
            logger.catching("removeACLEntry", ex);
        } finally {
            commonsClose(front_conn, prep, rs);
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
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            String method = "getACLwithInfo";
            logger.trace_start(method);
            ArrayList<ArrayList<String>> retList = new ArrayList<>();
            ArrayList<String> sqlList = new ArrayList<>();
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            try {
                front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                        front_connectionProps);

                prep = front_conn.prepareStatement("SELECT A.subject FROM acl A WHERE A.object = ?");
                prep.setString(1, refUUID);
                rs = prep.executeQuery();

                while (rs.next()) {
                    sqlList.add(rs.getString("subject"));
                }
            } catch (SQLException ex) {
                logger.catching("getACLwithInfo", ex);
            } finally {
                commonsClose(front_conn, prep, rs);
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
        final TokenHandler token = new TokenHandler(refresh);
        if (refresh != null) {
            auth = token.auth();
        }

        Object obj = parser.parse(dataInput);
        JSONObject JSONtemp = (JSONObject) obj;
        JSONArray JSONtempArray = (JSONArray) JSONtemp.get("jsonData");
        JSONObject JSONdata = (JSONObject) JSONtempArray.get(0);

        String xmldata = JSONtoxml(JSONdata, (String) JSONdata.get("drivertype"));

        try {
            URL url = new URL(String.format("%s/driver", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String result = executeHttpMethod(url, connection, "POST", xmldata, auth);
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
        final TokenHandler token = new TokenHandler(refresh);
        if (refresh != null) {
            auth = token.auth();
        }

        Properties prop = new Properties();
        prop.put("user", front_db_user);
        prop.put("password", front_db_pass);
        Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                prop);

        PreparedStatement prep = front_conn.prepareStatement("SELECT * FROM driver_wizard WHERE username = ? AND TopUri = ?");
        prep.setString(1, username);
        prep.setString(2, topuri);
        ResultSet rs = prep.executeQuery();

        rs.next();
        Object obj = parser.parse(rs.getString("data"));
        JSONObject JSONtemp = (JSONObject) obj;
        JSONArray JSONtempArray = (JSONArray) JSONtemp.get("jsonData");
        JSONObject JSONdata = (JSONObject) JSONtempArray.get(0);

        String xmldata = JSONtoxml(JSONdata, rs.getString("drivertype"));

        commonsClose(front_conn, prep, rs);

        try {
            URL url = new URL(String.format("%s/driver", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String result = executeHttpMethod(url, connection, "POST", xmldata, auth);
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
    @Path("/driver/{user}/add")
    @Consumes(value = {"application/json"})
    @RolesAllowed("Drivers")
    public void addDriver(@PathParam("user") String username, final String dataInput) {
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

            String user = (String) inputJSON.get("username");
            String driver = (String) inputJSON.get("drivername");
            String desc = (String) inputJSON.get("driverDescription");
            String data = (String) inputJSON.get("data");
            String uri = (String) inputJSON.get("topuri");
            String drivertype = (String) inputJSON.get("drivertype");

            Properties prop = new Properties();
            prop.put("user", front_db_user);
            prop.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend", prop);

            if (drivertype.equals("raw")) {
                prep = front_conn.prepareStatement("INSERT INTO frontend.driver_wizard VALUES (?, ?, ?, ?, ?, ?)");
                prep.setString(1, user);
                prep.setString(2, driver);
                prep.setString(3, desc);
                prep.setString(4, data);
                prep.setString(5, "");
                prep.setString(6, drivertype);
                prep.executeUpdate();
            } else {
                prep = front_conn.prepareStatement("INSERT INTO frontend.driver_wizard VALUES (?, ?, ?, ?, ?, ?)");
                prep.setString(1, user);
                prep.setString(2, driver);
                prep.setString(3, desc);
                prep.setString(4, data);
                prep.setString(5, uri);
                prep.setString(6, drivertype);
                prep.executeUpdate();

            }
        } catch (SQLException ex) {
            logger.catching("addDriver", ex);
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    @PUT
    @Path("/driver/{user}/edit/{topuri}")
    @RolesAllowed("Drivers")
    public String editDriverProfile(@PathParam("user") String username, @PathParam("topuri") String uri) throws SQLException {
        Properties front_connectionProps = new Properties();
        front_connectionProps.put("user", front_db_user);
        front_connectionProps.put("password", front_db_pass);
        Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                front_connectionProps);

        PreparedStatement prep = front_conn.prepareStatement("SELECT * FROM driver_wizard WHERE username = \'"+username + "\' AND TopUri = \'"+uri+"\'");
//        prep.setString(1, username);
//        prep.setString(2, uri);
        ResultSet rs = prep.executeQuery();

        commonsClose(front_conn, prep, rs);

        return "Deleted";
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
    @Path(value = "driver/{username}/delete/{topuri}")
    @RolesAllowed("Drivers")
    public String deleteDriverProfile(@PathParam(value = "username") String username, @PathParam(value = "topuri") String topuri) {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("DELETE FROM driver_wizard WHERE username = \'"+username + "\' AND TopUri = \'"+topuri+"\'");
//            prep.setString(1, username);
//            prep.setString(2, topuri);
            prep.executeUpdate();

            return "Deleted";
        } catch (SQLException ex) {
            logger.catching("deleteDriverProfile", ex);
            return "Failed";
        } finally {
            commonsClose(front_conn, prep, rs);
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

        PreparedStatement prep = front_conn.prepareStatement("SELECT * FROM driver_wizard WHERE username = \'"+username + "\' AND TopUri = \'"+topuri+"\'");
//        prep.setString(1, username);
//        prep.setString(2, topuri);
        ResultSet rs = prep.executeQuery();

        rs.next();

        Object obj = parser.parse(rs.getString("data"));
        JSONObject JSONtemp = (JSONObject) obj;
        JSONArray JSONtempArray = (JSONArray) JSONtemp.get("jsonData");
        JSONObject JSONdata = (JSONObject) JSONtempArray.get(0);

        commonsClose(front_conn, prep, rs);

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
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            ArrayList<String> list = new ArrayList<>();

            Properties prop = new Properties();
            prop.put("user", front_db_user);
            prop.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    prop);

            prep = front_conn.prepareStatement("SELECT * FROM driver_wizard WHERE username = ?");
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
        } finally {
            commonsClose(front_conn, prep, rs);
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
    
    
    
    
    
    
    
    
    /*Andrew's Draft for new post method for adding additional roles to groups*/
    
    @POST
    @Path("/keycloak/groups/{group}")
    @Produces("application/json")
    @RolesAllowed("Keycloak")
    public void addGroupRole(@PathParam("group") String subject, final String inputString) {
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
//                JSONArray roleArr = new JSONArray();
//                roleArr.add(inputJSON);

                out.write(roleArr.toString());
//                System.out.println("Check Here");
//                System.out.println(roleArr.toString());
                
            }
            logger.trace("addGroupRole", conn.getResponseCode() + " - " + conn.getResponseMessage(), "result");
        } catch (IOException | ParseException ex) {
            logger.catching("addGroupRole", ex);
        }
    }
    
    /*Andrew's draft for new delete method for deleting a role from a group*/
    @DELETE
    @Path("keycloak/groups/{group}")
    @Produces("application/json")
    @RolesAllowed("Keycloak")
    public void removeGroupRole(@PathParam("group") String subject, final String inputString) {
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
                String actual = inputString.toString();
                Object obj = parser.parse(actual);
                final JSONArray roleArr = (JSONArray) obj;
               
                out.write(roleArr.toString());
            }

            logger.trace("removeGroupRole", conn.getResponseCode() + " - " + conn.getResponseMessage(), "result");
        } catch (IOException | ParseException ex) {
            logger.catching("removeGroupRole", ex);
        }
    }
    
    /*Andrew's Draft for searching for the full information of a single role*/
    @GET
    @Path("keycloak/roles/{role}")
    @Produces("application/json")
    @RolesAllowed("Keycloak")
    public ArrayList<ArrayList<String>> getRoleData(@PathParam("role") String subject){
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
            HashMap<String,ArrayList<String>> search = new HashMap<String,ArrayList<String>>();
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
            return null;
        }
        
        
        
        
    }
    
    
    
    /*Andrew's draft for a new method to get roles for a single group*/
    @GET
    @Path("/keycloak/groups/{group}")
    @Produces("application/json")
    @RolesAllowed("Keycloak")
    public ArrayList<ArrayList<String>> getGroupRoles(@PathParam("group") String subject){
        try {
            String method = "getGroupRoles";
            logger.trace_start(method);
            ArrayList<ArrayList<String>> retList = new ArrayList<>();
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL(kc_url + "/admin/realms/StackV/roles/"+subject+"/composites");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", auth);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            logger.trace("getGroupRoles", conn.getResponseCode() + " - " + conn.getResponseMessage(), "result");
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
            logger.catching("getGroupRoles", ex);
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
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            ArrayList<ArrayList<String>> retList = new ArrayList<>();

            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("SELECT * FROM label WHERE username = ?");
            prep.setString(1, username);
            rs = prep.executeQuery();
            while (rs.next()) {
                ArrayList<String> labelList = new ArrayList<>();

                labelList.add(rs.getString("identifier"));
                labelList.add(rs.getString("label"));
                labelList.add(rs.getString("color"));

                retList.add(labelList);
            }

            return retList;
        } catch (SQLException ex) {
            logger.catching("getLabels", ex);
            return null;
        } finally {
            commonsClose(front_conn, prep, rs);
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
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
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
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("INSERT INTO `frontend`.`label` (`identifier`, `username`, `label`, `color`) VALUES (?, ?, ?, ?)");
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
        } finally {
            commonsClose(front_conn, prep, rs);
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
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            String method = "deleteLabel";
            logger.start(method);
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("DELETE FROM `frontend` .`label` WHERE username = ? AND identifier = ?");
            prep.setString(1, username);
            prep.setString(2, identifier);
            prep.executeUpdate();

            logger.end(method);
            return "Deleted";
        } catch (SQLException ex) {
            logger.catching("deleteLabel", ex);
            return "Failed";
        } finally {
            commonsClose(front_conn, prep, rs);
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
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            String method = "clearLabels";
            logger.start(method);
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("DELETE FROM `frontend`.`label` WHERE username = ? ");
            prep.setString(1, username);
            prep.executeUpdate();

            logger.end(method);
            return "Labels Cleared";
        } catch (SQLException ex) {
            logger.catching("clearLabels", ex);
            return "Failed";
        } finally {
            commonsClose(front_conn, prep, rs);
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
        if (verifyUserRole("admin")) {
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
     * @apiParam {String} username Optional - username
     * @apiParam {String} username Optional - username
     *
     * @apiExample {curl} Example Call:
     * curl -k -v http://127.0.0.1:8080/StackV-web/restapi/app/logging/logs?refUUID=e4d3bfd6-c269-4063-b02b-44aaef71d5b6 -H "Authorization: bearer $KC_ACCESS_TOKEN"
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
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        String method = "getLogs";
        try {
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            // Filtering by UUID alone
            if (refUUID != null && (level == null || level.equalsIgnoreCase("TRACE"))) {
                prep = front_conn.prepareStatement("SELECT * FROM log WHERE referenceUUID = ? ORDER BY timestamp DESC");
                prep.setString(1, refUUID);
            } // Filtering by level alone 
            else if (refUUID == null && level != null) {
                switch (level) {
                    case "INFO":
                        prep = front_conn.prepareStatement("SELECT * FROM log WHERE level != 'TRACE' ORDER BY timestamp DESC");
                        break;
                    case "WARN":
                        prep = front_conn.prepareStatement("SELECT * FROM log WHERE level != 'TRACE' AND level != 'INFO' ORDER BY timestamp DESC");
                        break;
                    case "ERROR":
                        prep = front_conn.prepareStatement("SELECT * FROM log WHERE level = 'ERROR' ORDER BY timestamp DESC");
                        break;
                }                
            } // Filtering by both
            else if (refUUID != null && level != null) {                
                switch (level) {
                    case "TRACE":
                        prep = front_conn.prepareStatement("SELECT * FROM log WHERE referenceUUID = ? ORDER BY timestamp DESC");
                        prep.setString(1, refUUID);
                        break;
                    case "INFO":
                        prep = front_conn.prepareStatement("SELECT * FROM log WHERE referenceUUID = ? AND level != 'TRACE' ORDER BY timestamp DESC");
                        prep.setString(1, refUUID);
                        break;
                    case "WARN":
                        prep = front_conn.prepareStatement("SELECT * FROM log WHERE referenceUUID = ? AND level != 'TRACE' AND level != 'INFO' ORDER BY timestamp DESC");
                        prep.setString(1, refUUID);
                        break;
                    case "ERROR":
                        prep = front_conn.prepareStatement("SELECT * FROM log WHERE referenceUUID = ? AND level = 'ERROR' ORDER BY timestamp DESC");
                        prep.setString(1, refUUID);
                        break;
                }
            }
            
            if (prep == null ) {
                prep = front_conn.prepareStatement("SELECT * FROM log ORDER BY timestamp DESC");
            }           
            rs = prep.executeQuery();
            JSONObject retJSON = new JSONObject();
            JSONArray logArr = new JSONArray();
            while (rs.next()) {
                JSONObject logJSON = new JSONObject();

                logJSON.put("referenceUUID", rs.getString("referenceUUID"));
                logJSON.put("marker", rs.getString("marker"));
                logJSON.put("timestamp", rs.getString("timestamp"));
                logJSON.put("level", rs.getString("level"));
                logJSON.put("logger", rs.getString("logger"));
                logJSON.put("message", rs.getString("message"));
                logJSON.put("event", rs.getString("event"));
                logJSON.put("exception", rs.getString("exception"));

                logArr.add(logJSON);
            }
            retJSON.put("data", logArr);

            return retJSON.toJSONString();
        } catch (UnhandledException ex) {
            logger.trace(method, "Logging connection lost?");
            return null;
        } catch (SQLException ex) {
            logger.catching(method, ex);
            return null;
        } finally {
            commonsClose(front_conn, prep, rs);
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

        String serviceType = null;
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        Properties front_connectionProps = new Properties();
        front_connectionProps.put("user", front_db_user);
        front_connectionProps.put("password", front_db_pass);
        try {
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("select S.name from service_instance I, service S "
                    + "where I.referenceUUID=? AND I.service_id=S.service_id");
            prep.setString(1, svcUUID);
            rs = prep.executeQuery();
            while (rs.next()) {
                serviceType = rs.getString("name");
            }
        } catch (SQLException ex) {
            //Logger.getLogger(WebResource.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            commonsClose(front_conn, prep, rs);
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
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
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

            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            if (verifyUserRole("admin")) {
                prep = front_conn.prepareStatement("SELECT DISTINCT I.type, I.referenceUUID, I.alias_name, I.super_state "
                        + "FROM service_instance I, acl A");
            } else {
                prep = front_conn.prepareStatement("SELECT DISTINCT I.type, I.referenceUUID, I.alias_name, I.super_state "
                        + "FROM service_instance I, acl A "
                        + "WHERE I.referenceUUID = A.object AND (A.subject = ? OR I.username = ?)");
                prep.setString(1, username);
                prep.setString(2, username);
            }
            rs = prep.executeQuery();
            while (rs.next()) {
                ArrayList<String> instanceList = new ArrayList<>();

                String instanceName = Services.get(rs.getString("type")).get(0);
                String instanceUUID = rs.getString("referenceUUID");
                String instanceSuperState = rs.getString("super_state");
                String instanceAlias = rs.getString("alias_name");
                if (!banList.contains(instanceName)) {
                    try {
                        URL url = new URL(String.format("%s/service/%s/status", host, instanceUUID));
                        HttpURLConnection status = (HttpURLConnection) url.openConnection();

                        String instanceState = instanceSuperState + " - " + executeHttpMethod(url, status, "GET", null, auth);

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
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    @GET
    @Path("/panel/{userId}/wizard")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<ArrayList<String>> loadWizard(@PathParam("userId") String userId) {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            ArrayList<ArrayList<String>> retList = new ArrayList<>();

            // Verify user
            String username = authUsername(userId);
            if (username == null) {
                logger.error("loadInstances", "Logged-in user does not match requested user information");
                return retList;
            }

            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);

            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            if (username.equals("admin")) {
                prep = front_conn.prepareStatement("SELECT DISTINCT W.name, W.description, W.editable, W.service_wizard_id "
                        + "FROM service_wizard W");
            } else {
                prep = front_conn.prepareStatement("SELECT DISTINCT W.name, W.description, W.editable, W.service_wizard_id "
                        + "FROM service_wizard W WHERE W.username = ? OR W.username IS NULL");
                prep.setString(1, username);
            }
            rs = prep.executeQuery();
            while (rs.next()) {
                ArrayList<String> wizardList = new ArrayList<>();

                wizardList.add(rs.getString("name"));
                wizardList.add(rs.getString("description"));
                wizardList.add(rs.getString("service_wizard_id"));

                retList.add(wizardList);
            }

            return retList;
        } catch (SQLException ex) {
            logger.catching("loadWizard", ex);
            return null;
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    @GET
    @Path("/panel/{userId}/editor")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<ArrayList<String>> loadEditor(@PathParam("userId") String userId) {
        ArrayList<ArrayList<String>> retList = new ArrayList<>();

        KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class.getName());
        AccessToken accessToken = securityContext.getToken();
        Set<String> roleSet = accessToken.getResourceAccess("StackV").getRoles();

        for (Map.Entry<String, List<String>> entry : Services.entrySet()) {
            if (verifyUserRole(entry.getKey())) {
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
    @RolesAllowed("Panels")
    public ArrayList<String> loadObjectACL(@PathParam("refUuid") String refUuid) {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            ArrayList<String> retList = new ArrayList<>();

            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("SELECT A.subject FROM acl A WHERE A.object = ?");
            prep.setString(1, refUuid);
            rs = prep.executeQuery();

            while (rs.next()) {
                retList.add(rs.getString("subject"));
            }
            return retList;
        } catch (SQLException ex) {
            logger.catching("loadObjectACL", ex);
            return null;
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    @GET
    @Path("/panel/acl")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<String> loadSubjectACL() {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class.getName());
            AccessToken accessToken = securityContext.getToken();
            String username = accessToken.getPreferredUsername();

            ArrayList<String> retList = new ArrayList<>();

            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("SELECT A.object FROM acl A WHERE A.subject = ?");
            prep.setString(1, username);
            rs = prep.executeQuery();

            while (rs.next()) {
                retList.add(rs.getString("subject"));
            }
            return retList;
        } catch (SQLException ex) {
            logger.catching("loadObjectACL", ex);
            return null;
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    @GET
    @Path("/details/{uuid}/instance")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<String> loadInstanceDetails(@PathParam("uuid") String uuid) {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        logger.refuuid(uuid);
        try {
            ArrayList<String> retList = new ArrayList<>();

            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);

            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("SELECT I.type, I.creation_time, I.alias_name, I.super_state, V.verification_state, I.last_state FROM service_instance I, service_verification V "
                    + "WHERE I.referenceUUID = ? AND I.service_instance_id = V.service_instance_id");
            prep.setString(1, uuid);

            rs = prep.executeQuery();
            while (rs.next()) {
                retList.add(rs.getString("verification_state"));
                retList.add(Services.get(rs.getString("type")).get(0));
                retList.add(rs.getString("alias_name"));
                retList.add(rs.getString("creation_time"));
                retList.add(rs.getString("super_state"));
                retList.add(rs.getString("last_state"));
            }

            return retList;
        } catch (SQLException ex) {
            logger.catching("loadInstanceDetails", ex);
            return null;
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    /*@GET
    @Path("/details/{uuid}/delta")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<ArrayList<String>> loadInstanceDelta(@PathParam("uuid") String uuid) {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        logger.refuuid(uuid);
        try {
            ArrayList<ArrayList<String>> retList = new ArrayList<>();

            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);

            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("SELECT D.service_delta_id, D.delta, D.type, I.super_state FROM service_delta D, service_instance I, service_history H "
                    + "WHERE I.referenceUUID = ? AND I.service_instance_id = D.service_instance_id AND D.service_history_id = H.service_history_id AND D.service_instance_id = H.service_instance_id");
            prep.setString(1, uuid);

            rs = prep.executeQuery();
            while (rs.next()) {
                ArrayList<String> deltaList = new ArrayList<>();
                deltaList.add(rs.getString("type"));
                deltaList.add(rs.getString("service_delta_id"));
                deltaList.add(rs.getString("super_state"));
                deltaList.add(rs.getString("delta"));
                retList.add(deltaList);
            }

            return retList;
        } catch (SQLException ex) {
            logger.catching("loadInstanceDelta", ex);
            return null;
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }*/

    @GET
    @Path("/details/{uuid}/verification")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<String> loadInstanceVerification(@PathParam("uuid") String uuid) {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        logger.refuuid(uuid);
        try {
            ArrayList<String> retList = new ArrayList<>();

            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);

            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("SELECT * FROM service_verification V, service_instance I "
                    + "WHERE I.referenceUUID = ? AND V.service_instance_id = I.service_instance_id");
            prep.setString(1, uuid);

            rs = prep.executeQuery();
            while (rs.next()) {
                retList.add(rs.getString("verification_run"));
                retList.add(rs.getString("creation_time"));
                retList.add(rs.getString("addition"));
                retList.add(rs.getString("reduction"));
                retList.add(rs.getString("service_instance_id"));
                retList.add(String.valueOf(rs.getBoolean("enabled")));
            }

            return retList;
        } catch (SQLException ex) {
            logger.catching("loadInstanceVerification", ex);
            return null;
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    @GET
    @Path("/details/{uuid}/acl")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<String> loadInstanceACL(@PathParam("uuid") String uuid) {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        logger.refuuid(uuid);
        try {
            ArrayList<String> retList = new ArrayList<>();

            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);

            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("SELECT * FROM `acl`");

            rs = prep.executeQuery();
            while (rs.next()) {
            }

            return retList;
        } catch (SQLException ex) {
            logger.catching("loadInstanceACL", ex);
            return null;
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    @GET
    @Path("/service/lastverify/{siUUID}")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public HashMap<String, String> getVerificationResults(@PathParam("siUUID") String serviceUUID) {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        logger.refuuid(serviceUUID);
        try {
            HashMap<String, String> retMap = new HashMap<>();
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("SELECT V.* FROM service_instance I, service_verification V WHERE I.referenceUUID = ? AND V.service_instance_id = I.service_instance_id");
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
            return null;
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    @GET
    @Path("/service/availibleitems/{siUUID}")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public String getVerificationResultsUnion(@PathParam("siUUID") String serviceUUID) throws Exception {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        logger.refuuid(serviceUUID);
        try {
            HashMap<String, String> retMap = new HashMap<>();
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("SELECT V.* FROM service_instance I, service_verification V WHERE I.referenceUUID = ? AND V.service_instance_id = I.service_instance_id");
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
            return null;
        } finally {
            commonsClose(front_conn, prep, rs);
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
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            String method = "getProfile";
            logger.trace_start(method);
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("SELECT wizard_json FROM service_wizard WHERE service_wizard_id = ?");
            prep.setInt(1, wizardID);
            rs = prep.executeQuery();
            while (rs.next()) {
                return rs.getString(1);
            }

            logger.trace_end(method);
            return "";
        } catch (SQLException ex) {
            logger.catching("getProfile", ex);
            return null;
        } finally {
            commonsClose(front_conn, prep, rs);
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
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            String method = "editProfile";
            logger.start(method);
            // Connect to the DB
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend", front_connectionProps);

            // TODO: Sanitize the input!
            prep = front_conn.prepareStatement("UPDATE service_wizard SET wizard_json = ? WHERE service_wizard_id = ? ");
            prep.setString(1, inputString);
            prep.setInt(2, wizardId);
            prep.executeUpdate();

            logger.end(method);
        } catch (SQLException ex) {
            logger.catching("editProfile", ex);
        } finally {
            commonsClose(front_conn, prep, rs);
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
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            String method = "newProfile";
            logger.start(method);
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);

            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend", front_connectionProps);
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

            prep = front_conn.prepareStatement("INSERT INTO `frontend`.`service_wizard` (service_id, username, name, wizard_json, description, editable) VALUES (?, ?, ?, ?, ?, ?)");
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
        } finally {
            commonsClose(front_conn, prep, rs);
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
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            String method = "deleteProfile";
            logger.start(method);
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("DELETE FROM service_wizard WHERE service_wizard_id = ?");
            prep.setInt(1, wizardId);
            prep.executeUpdate();

            logger.end(method);
        } catch (SQLException ex) {
            logger.catching("deleteProfile", ex);
        } finally {
            commonsClose(front_conn, prep, rs);
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
    public String checkStatus(@PathParam("siUUID") String refUUID) {
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        final TokenHandler token = new TokenHandler(refresh);

        ServiceHandler instance = new ServiceHandler(refUUID, token);

        return instance.superState.name() + " - " + instance.status() + "\n";
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
    public String subStatus(@PathParam("siUUID") String refUUID) {
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        final TokenHandler token = new TokenHandler(refresh);

        ServiceHandler instance = new ServiceHandler(refUUID, token);

        return instance.status();
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
        String method = "createService";
        try {
            logger.start(method, "Thread:" + Thread.currentThread());
            final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
            final TokenHandler token = new TokenHandler(refresh);
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
                        asyncResponse.resume(doCreateService(inputJSON, token));
                    }
                });
            } else {
                logger.warning(method, "User not allowed access to " + serviceType);
            }

        } catch (ParseException ex) {
            logger.catching(method, ex);
        }
        logger.end(method);
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
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        final TokenHandler token = new TokenHandler(refresh);
        String method = "operate";
        logger.trace_start(method, "Thread:" + Thread.currentThread());
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                asyncResponse.resume(doOperate(refUuid, action, token));
            }
        });
        logger.trace_end(method);
    }

    // Async Methods -----------------------------------------------------------
    private String doCreateService(JSONObject inputJSON, TokenHandler token) {
        ServiceHandler instance = new ServiceHandler(inputJSON, token);
        return instance.refUUID;
    }

    private String doOperate(@PathParam("siUUID") String refUUID, @PathParam("action") String action, TokenHandler token) {
        ServiceHandler instance = new ServiceHandler(refUUID, token);
        instance.operate(action);

        return instance.superState.name();
    }

    @GET
    @Path("/delta/{siUUID}")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public String getDeltaBacked(@PathParam("siUUID") String serviceUUID) {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
        try {
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            prep = front_conn.prepareStatement("SELECT COUNT(*) FROM service_delta D, service_instance I WHERE D.service_instance_id = I.service_instance_id AND I.referenceUUID = ?");
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
        } catch (IOException | SQLException ex) {
            logger.catching("getDeltaBacked", ex);
            return null;
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    // Utility Methods ---------------------------------------------------------
    /**
     * Executes HTTP Request.
     *
     * @param url destination url
     * @param conn connection object
     * @param method request method
     * @param body request body
     * @param authHeader authorization header
     * @return response string.
     * @throws IOException
     */
    public static String executeHttpMethod(URL url, HttpURLConnection conn, String method, String body, String authHeader) throws IOException {
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-type", "application/xml");
        conn.setRequestProperty("Accept", "application/json");

        if (authHeader != null) {
            conn.setRequestProperty("Authorization", authHeader);
            //logger.log(Level.INFO, "{0} header added", authHeader);
        }

        if (body != null && !body.isEmpty()) {
            conn.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.writeBytes(body);
                wr.flush();
            }
        }

        int responseCode = conn.getResponseCode();
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

    private boolean verifyUserRole(String role) {
        KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class
                .getName());
        final AccessToken accessToken = securityContext.getToken();

        Set<String> roleSet;
        roleSet = accessToken.getRealmAccess().getRoles();
        roleSet.addAll(accessToken.getResourceAccess("StackV").getRoles());

        return roleSet.contains(role);
    }

    public static void commonsClose(Connection front_conn, PreparedStatement prep, ResultSet rs) {
        try {
            if (rs != null) {
                DbUtils.close(rs);
            }
            if (prep != null) {
                DbUtils.close(prep);
            }
            if (front_conn != null) {
                DbUtils.close(front_conn);
            }
        } catch (SQLException ex) {

        }
    }
}
