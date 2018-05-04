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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
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

        result.put("vcn", Arrays.asList(
                "Virtual Cloud Network",
                "Network Creation Pilot Testbed."));

        result.put("dnc", Arrays.asList(
                "Dynamic Network Connection",
                "Creation of new network connections."));

        result.put("ahc", Arrays.asList(
                "Advanced Hybrid Cloud",
                "Advanced Hybrid Cloud Service."));

        result.put("ecc", Arrays.asList(
                "EdgeCloud Connection",
                "MAX EdgeCloud Pilot Service."));

        return Collections.unmodifiableMap(result);
    }

    private final StackLogger logger = new StackLogger(WebResource.class.getName(), "WebResource");
    String host = "http://127.0.0.1:8080/StackV-web/restapi";
    String kc_url = System.getProperty("kc_url");
    JSONParser parser = new JSONParser();
    private static final ExecutorService executorService = java.util.concurrent.Executors.newCachedThreadPool();

    private final String keycloakStackVClientID = "5c0fab65-4577-4747-ad42-59e34061390b";

    String ipaBaseServerUrl = System.getProperty("ipa_url");
    String ipaUsername = System.getProperty("ipa_username");
    String ipaPasswd = System.getProperty("ipa_passwd");
    static String ipaCookie;

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
    public void addACLEntry(@PathParam("refUUID") String refUUID, final String subject) throws SQLException {
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

            front_conn = factory.getConnection("frontend");

            prep = front_conn.prepareStatement("INSERT INTO `frontend`.`acl` (`subject`, `is_group`, `object`) "
                    + "VALUES (?, '0', ?)");
            prep.setString(1, subject);
            prep.setString(2, refUUID);
            prep.executeUpdate();

            logger.end(method);
        } catch (SQLException ex) {
            logger.catching("addACLEntry", ex);
            throw ex;
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
    public void removeACLEntry(@PathParam("refUUID") String refUUID, final String subject) throws SQLException {
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

            front_conn = factory.getConnection("frontend");

            prep = front_conn.prepareStatement("DELETE FROM `frontend`.`acl` WHERE subject = ? AND object = ?");
            prep.setString(1, subject);
            prep.setString(2, refUUID);
            prep.executeUpdate();

            logger.end(method);
        } catch (SQLException ex) {
            logger.catching("removeACLEntry", ex);
            throw ex;
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
    public ArrayList<ArrayList<String>> getACLwithInfo(@PathParam("refUuid") String refUUID) throws SQLException, IOException, ParseException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            String method = "getACLwithInfo";
            logger.trace_start(method);
            ArrayList<ArrayList<String>> retList = new ArrayList<>();
            ArrayList<String> sqlList = new ArrayList<>();

            try {
                front_conn = factory.getConnection("frontend");

                prep = front_conn.prepareStatement("SELECT A.subject FROM acl A WHERE A.object = ?");
                prep.setString(1, refUUID);
                rs = prep.executeQuery();

                while (rs.next()) {
                    sqlList.add(rs.getString("subject"));
                }
            } catch (SQLException ex) {
                logger.catching("getACLwithInfo", ex);
                throw ex;
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
            throw ex;
        }
    }

    // >FreeIPA-based ACL
    @POST
    @Path("/acl/ipa/login")
    @Produces("application/json")
    @RolesAllowed("ACL")
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
                Logger.getLogger(WebResource.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(WebResource.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }

            // return the JSON object as a string
            return result.toJSONString();
        } else {
            throw logger.error_throwing("ipaLogin()", "IPA username or password not set");
        }
    }

    @POST
    @Path("/acl/ipa/request")
    @Consumes("application/json")
    @Produces("application/json")
    @RolesAllowed("ACL")
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
                Logger.getLogger(WebResource.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }

            // return the JSONObject as a string
            return result.toJSONString();
        } else {
            throw logger.error_throwing("ipaRequest()", "IPA server url not set");
        }
    }

    @DELETE
    @Path("/acl/ipa/servicepolicies/{serviceUUID}")
    @Produces("application/json")
    @RolesAllowed("ACL")
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

    // >Drivers    
    @PUT
    @Path("/driver/install")
    @Consumes("application/json")
    @Produces("text/plain")
    @RolesAllowed("Drivers")
    public String installDriver(final String dataInput) throws SQLException, IOException, ParseException {
        String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        final TokenHandler token = new TokenHandler(refresh);

        Object obj = parser.parse(dataInput);
        JSONObject JSONtemp = (JSONObject) obj;
        JSONArray JSONtempArray = (JSONArray) JSONtemp.get("jsonData");
        JSONObject JSONdata = (JSONObject) JSONtempArray.get(0);
        String xmldata = JSONtoxml(JSONdata, (String) JSONdata.get("driverType"));
        try {
            URL url = new URL(String.format("%s/driver", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String result = executeHttpMethod(url, connection, "POST", xmldata, token.auth());
            if (!result.equalsIgnoreCase("plug successfully")) //plugin error
            {
                return "PLUGIN FAILED: Driver Resource did not return successfull";
            }
        } catch (IOException ex) {
            logger.catching("installDriver", ex);
            throw ex;
        }

        return "PLUGIN SUCCEEDED";
    }

    @PUT
    @Path("/driver/{user}/install/{topuri}")
    @Produces("text/plain")
    @RolesAllowed("Drivers")
    public String installDriverProfile(@PathParam("user") String username, @PathParam(value = "topuri") String topuri) throws SQLException, IOException, ParseException {
        String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        final TokenHandler token = new TokenHandler(refresh);

        Connection front_conn = factory.getConnection("frontend");
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
            throw ex;
        }

        return "PLUGIN SUCCEEDED";
    }

    @PUT
    @Path("/driver/{user}/add")
    @Consumes(value = {"application/json"})
    @RolesAllowed("Drivers")
    public void addDriver(@PathParam("user") String username, final String dataInput) throws SQLException {
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
            String drivername = (String) inputJSON.get("drivername");
            String desc = (String) inputJSON.get("driverDescription");
            String data = (String) inputJSON.get("data");
            String uri = (String) inputJSON.get("topuri");
            String drivertype = (String) inputJSON.get("drivertype");

            front_conn = factory.getConnection("frontend");

            if (drivertype.equals("raw")) {
                // excluding the topuri if the driver type is raw
                prep = front_conn.prepareStatement("INSERT INTO frontend.driver_wizard VALUES (?, ?, ?, ?, ?, ?)");
                prep.setString(1, user);
                prep.setString(2, drivername);
                prep.setString(3, drivertype);
                prep.setString(4, "");
                prep.setString(5, desc);
                prep.setString(6, data);
                prep.executeUpdate();
            } else {
                prep = front_conn.prepareStatement("INSERT INTO frontend.driver_wizard VALUES (?, ?, ?, ?, ?, ?)");
                prep.setString(1, user);
                prep.setString(2, drivername);
                prep.setString(3, drivertype);
                prep.setString(4, uri);
                prep.setString(5, desc);
                prep.setString(6, data);
                prep.executeUpdate();

            }
        } catch (SQLException ex) {
            logger.catching("addDriver", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    @PUT
    @Path("/driver/{user}/edit/{topuri}")
    @Consumes(value = {"application/json"})
    @RolesAllowed("Drivers")
    public String editDriverProfile(@PathParam(value = "user") String username, @PathParam(value = "topuri") String oldTopUri, final String dataInput) throws SQLException, ParseException {
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

            prep = front_conn.prepareStatement("UPDATE driver_wizard SET TopUri = ?, data = ? WHERE username = ? AND TopUri = ?");
            prep.setString(1, newTopUri);
            prep.setString(2, newData);
            prep.setString(3, username);
            prep.setString(4, oldTopUri);
            prep.executeUpdate();
        } catch (SQLException ex) {
            logger.catching("editDriverProfile", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs);
        }

        return "Saved edits successfully.";
    }

    @DELETE
    @Path(value = "driver/{username}/delete/{topuri}")
    @RolesAllowed("Drivers")
    public String deleteDriverProfile(@PathParam(value = "username") String username, @PathParam(value = "topuri") String topuri) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            front_conn = factory.getConnection("frontend");

            prep = front_conn.prepareStatement("DELETE FROM driver_wizard WHERE username = \'" + username + "\' AND TopUri = \'" + topuri + "\'");
//            prep.setString(1, username);
//            prep.setString(2, topuri);
            prep.executeUpdate();

            return "Deleted";
        } catch (SQLException ex) {
            logger.catching("deleteDriverProfile", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    @GET
    @Path("/driver/{user}/getdetails/{topuri}")
    @Produces("application/json")
    @RolesAllowed("Drivers")
    public JSONObject getDriverDetails(@PathParam(value = "user") String username, @PathParam(value = "topuri") String topuri) throws SQLException, ParseException {
        Connection front_conn = factory.getConnection("frontend");

        PreparedStatement prep = front_conn.prepareStatement("SELECT * FROM driver_wizard WHERE username = \'" + username + "\' AND TopUri = \'" + topuri + "\'");
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

    @GET
    @Path("/driver/{user}/get")
    @Produces("application/json")
    @RolesAllowed("Drivers")
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
    public ArrayList<ArrayList<String>> getUsers() throws IOException, ParseException {
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
            throw ex;
        }
    }

    /*Andrew's Draft for new post method for adding additional roles to groups*/
    @POST
    @Path("/keycloak/groups/{group}")
    @Produces("application/json")
    @RolesAllowed("Keycloak")
    public void addGroupRole(@PathParam("group") String subject, final String inputString) throws IOException, ParseException {
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

    /*Andrew's draft for new delete method for deleting a role from a group*/
    @DELETE
    @Path("keycloak/groups/{group}")
    @Produces("application/json")
    @RolesAllowed("Keycloak")
    public void removeGroupRole(@PathParam("group") String subject, final String inputString) throws IOException, ParseException {
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

    /*Andrew's Draft for searching for the full information of a single role*/
    @GET
    @Path("keycloak/roles/{role}")
    @Produces("application/json")
    public ArrayList<ArrayList<String>> getRoleData(@PathParam("role") String subject) throws IOException, ParseException {
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

    /*Andrew's draft for a new method to get roles for a single group*/
    @GET
    @Path("/keycloak/groups/{group}")
    @Produces("application/json")
    public ArrayList<ArrayList<String>> getGroupRoles(@PathParam("group") String subject) throws IOException, ParseException {
        try {
            String method = "getGroupRoles";
            logger.trace_start(method);
            ArrayList<ArrayList<String>> retList = new ArrayList<>();
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL(kc_url + "/admin/realms/StackV/roles/" + subject + "/composites");
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
            throw ex;
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
    public ArrayList<ArrayList<String>> getGroups() throws IOException, ParseException {
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
            throw ex;
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
    public ArrayList<ArrayList<String>> getRoles() throws IOException, ParseException {
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
            throw ex;
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
    public ArrayList<ArrayList<String>> getUserGroups(@PathParam("user") String subject) throws IOException, ParseException {
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
            throw ex;
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
    public void addUserGroup(@PathParam("user") String subject, final String inputString) throws IOException, ParseException {
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
            throw ex;
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
    public void removeUserGroup(@PathParam("user") String subject, final String inputString) throws IOException, ParseException {
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
            throw ex;
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
    public ArrayList<ArrayList<String>> getUserRoles(@PathParam("user") String subject) throws IOException, ParseException {
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
            throw ex;
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
    public void addUserRole(@PathParam("user") String subject, final String inputString) throws IOException, ParseException {
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
            throw ex;
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
    public void removeUserRole(@PathParam("user") String subject, final String inputString) throws IOException, ParseException {
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
            throw ex;
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
    public ArrayList<ArrayList<String>> getLabels(@PathParam("user") String username) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            ArrayList<ArrayList<String>> retList = new ArrayList<>();

            front_conn = factory.getConnection("frontend");

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
            throw ex;
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
    public String label(final String inputString) throws SQLException {
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

            front_conn = factory.getConnection("frontend");

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
            throw ex;
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
    public String deleteLabel(@PathParam(value = "username") String username, @PathParam(value = "identifier") String identifier) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            String method = "deleteLabel";
            logger.start(method);
            front_conn = factory.getConnection("frontend");

            prep = front_conn.prepareStatement("DELETE FROM `frontend` .`label` WHERE username = ? AND identifier = ?");
            prep.setString(1, username);
            prep.setString(2, identifier);
            prep.executeUpdate();

            logger.end(method);
            return "Deleted";
        } catch (SQLException ex) {
            logger.catching("deleteLabel", ex);
            throw ex;
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
    public String clearLabels(@PathParam(value = "username") String username) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            String method = "clearLabels";
            logger.start(method);
            front_conn = factory.getConnection("frontend");

            prep = front_conn.prepareStatement("DELETE FROM `frontend`.`label` WHERE username = ? ");
            prep.setString(1, username);
            prep.executeUpdate();

            logger.end(method);
            return "Labels Cleared";
        } catch (SQLException ex) {
            logger.catching("clearLabels", ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    // >Logging
    @GET
    @Path("/logging/")
    @Produces("application/json")
    @RolesAllowed("Logging")
    public String getLogLevel() {
        return logger.getLogger().getLevel().name();
    }

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

    @GET
    @Path("/logging/logs")
    @Produces("application/json")
    @RolesAllowed("Logging")
    public String getLogs(@QueryParam("refUUID") String refUUID, @QueryParam("level") String level) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        String method = "getLogs";
        try {
            front_conn = factory.getConnection("frontend");

            // Filtering by UUID alone
            if (refUUID != null && (level == null || level.equalsIgnoreCase("TRACE"))) {
                prep = front_conn.prepareStatement("SELECT * FROM log WHERE referenceUUID = ? ORDER BY timestamp DESC");
                prep.setString(1, refUUID);
            } // Filtering by level alone 
            else if (refUUID == null && level != null) {
                switch (level) {
                    case "TRACE":
                        prep = front_conn.prepareStatement("SELECT * FROM log ORDER BY timestamp DESC");
                        break;
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

            if (prep == null) {
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
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    @GET
    @Path("/logging/logs/serverside")
    @Produces("application/json")
    @RolesAllowed("Logging")
    public String getLogsServerSide(@Context UriInfo uriInfo) throws SQLException {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        String level = queryParams.getFirst("level");
        String refUUID = queryParams.getFirst("refUUID");
        String search = queryParams.getFirst("search[value]");
        int draw = Integer.parseInt(queryParams.getFirst("draw"));
        int start = Integer.parseInt(queryParams.getFirst("start"));
        int length = Integer.parseInt(queryParams.getFirst("length"));

        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        String method = "getLogsServerSide";
        try {
            front_conn = factory.getConnection("frontend");

            String prepString = "SELECT * FROM log";
            // Filtering by UUID alone
            if (refUUID != null && (level == null || level.equalsIgnoreCase("TRACE"))) {
                prepString = prepString + " WHERE referenceUUID = ?";
            } // Filtering by level alone 
            else if (refUUID == null && level != null) {
                switch (level) {
                    case "INFO":
                        prepString = prepString + " WHERE level != 'TRACE'";
                        break;
                    case "WARN":
                        prepString = prepString + " WHERE level != 'TRACE' AND level != 'INFO'";
                        break;
                    case "ERROR":
                        prepString = prepString + " WHERE level = 'ERROR'";
                        break;
                }
            } // Filtering by both
            else if (refUUID != null && level != null) {
                switch (level) {
                    case "TRACE":
                        prepString = prepString + " WHERE referenceUUID = ?";
                        break;
                    case "INFO":
                        prepString = prepString + " WHERE referenceUUID = ? AND level != 'TRACE'";
                        break;
                    case "WARN":
                        prepString = prepString + " WHERE referenceUUID = ? AND level != 'TRACE' AND level != 'INFO'";
                        break;
                    case "ERROR":
                        prepString = prepString + " WHERE referenceUUID = ? AND level = 'ERROR'";
                        break;
                }
            }

            if (!search.equals("")) {
                if (prepString.contains("WHERE")) {
                    prepString = prepString + " AND (logger LIKE '%" + search + "%' "
                            + "OR module LIKE '%" + search + "%' "
                            + "OR method LIKE '%" + search + "%' "
                            + "OR event LIKE '%" + search + "%' "
                            + "OR message LIKE '%" + search + "%') ";
                } else {
                    prepString = prepString + " WHERE (logger LIKE '%" + search + "%' "
                            + "OR module LIKE '%" + search + "%' "
                            + "OR method LIKE '%" + search + "%' "
                            + "OR event LIKE '%" + search + "%' "
                            + "OR message LIKE '%" + search + "%') ";
                }
            }

            String prepStringCount = prepString.replace("SELECT *", "SELECT COUNT(*)");
            prep = front_conn.prepareStatement(prepStringCount);
            if (refUUID != null) {
                prep.setString(1, refUUID);
            }
            rs = prep.executeQuery();
            rs.next();
            int count = rs.getInt(1);

            prepString = prepString + " ORDER BY log_id DESC LIMIT ?,?";
            prep = front_conn.prepareStatement(prepString);
            if (refUUID != null) {
                prep.setString(1, refUUID);
                prep.setInt(2, start);
                prep.setInt(3, length);
            } else {
                prep.setInt(1, start);
                prep.setInt(2, length);
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
            retJSON.put("draw", draw);
            retJSON.put("recordsTotal", count);
            retJSON.put("recordsFiltered", count);

            return retJSON.toJSONString();
        } catch (UnhandledException ex) {
            logger.trace(method, "Logging connection lost?");
            return null;
        } catch (SQLException ex) {
            logger.catching(method, ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    @GET
    @Path("/logging/instances")
    @Produces("application/json")
    @RolesAllowed("Logging")
    public String loadInstanceData() throws SQLException, IOException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        String method = "loadInstanceData";
        try {
            String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class.getName());
            AccessToken accessToken = securityContext.getToken();
            String username = accessToken.getPreferredUsername();

            front_conn = factory.getConnection("frontend");

            if (verifyUserRole("admin")) {
                prep = front_conn.prepareStatement("SELECT DISTINCT I.type, I.referenceUUID, I.alias_name, I.super_state, I.creation_time, I.last_state, I.username, V.verification_state "
                        + "FROM service_instance I, service_verification V "
                        + "WHERE V.service_instance_id = I.service_instance_id "
                        + "ORDER BY I.creation_time");
            } else {
                prep = front_conn.prepareStatement("SELECT DISTINCT I.type, I.referenceUUID, I.alias_name, I.super_state, I.creation_time, I.last_state, I.username, V.verification_state "
                        + "FROM service_instance I, acl A, service_verification V "
                        + "WHERE I.referenceUUID = A.object AND V.service_instance_id = I.service_instance_id AND (A.subject = ? OR I.username = ?) "
                        + "ORDER BY I.creation_time");
                prep.setString(1, username);
                prep.setString(2, username);
            }
            rs = prep.executeQuery();

            JSONObject retJSON = new JSONObject();
            JSONArray logArr = new JSONArray();
            while (rs.next()) {
                JSONObject logJSON = new JSONObject();
                String instanceUUID = rs.getString("referenceUUID");
                String instanceState = rs.getString("super_state");
                try {
                    URL url = new URL(String.format("%s/service/%s/status", host, instanceUUID));
                    HttpURLConnection status = (HttpURLConnection) url.openConnection();

                    instanceState = instanceState + " - " + executeHttpMethod(url, status, "GET", null, auth);
                } catch (IOException ex) {
                    AuditService.cleanInstances("frontend");
                    logger.catching(method, ex);
                }

                logJSON.put("alias", rs.getString("alias_name"));
                logJSON.put("type", Services.get(rs.getString("type")).get(0));
                logJSON.put("referenceUUID", instanceUUID);
                logJSON.put("state", instanceState);
                logJSON.put("owner", rs.getString("username"));
                logJSON.put("lastState", rs.getString("last_state"));
                logJSON.put("verification", rs.getString("verification_state"));
                logJSON.put("timestamp", rs.getString("creation_time"));

                logArr.add(logJSON);
            }
            retJSON.put("data", logArr);

            return retJSON.toJSONString();
        } catch (SQLException ex) {
            AuditService.cleanInstances("frontend");
            logger.catching(method, ex);
            throw ex;
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
    @RolesAllowed("Panels")
    public String getManifest(@PathParam("svcUUID") String svcUUID) throws SQLException {
        logger.refuuid(svcUUID);
        String method = "getManifest(svcUUID)";
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
    @RolesAllowed("Panels")
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
    @RolesAllowed("Panels")
    public ArrayList<ArrayList<String>> loadWizard() throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            ArrayList<ArrayList<String>> retList = new ArrayList<>();
            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class.getName());
            AccessToken accessToken = securityContext.getToken();
            String username = accessToken.getPreferredUsername();

            front_conn = factory.getConnection("frontend");

            if (username.equals("admin")) {
                prep = front_conn.prepareStatement("SELECT DISTINCT * FROM service_wizard");
            } else {
                prep = front_conn.prepareStatement("SELECT DISTINCT W.* FROM service_wizard W "
                        + "LEFT JOIN service_wizard_licenses L "
                        + "ON (W.service_wizard_id = L.service_wizard_id) "
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
            commonsClose(front_conn, prep, rs);
        }
    }

    @GET
    @Path("/panel/editor")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<ArrayList<String>> loadEditor() {
        ArrayList<ArrayList<String>> retList = new ArrayList<>();
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
            commonsClose(front_conn, prep, rs);
        }
    }

    @GET
    @Path("/panel/acl")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<String> loadSubjectACL() throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class.getName());
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
            commonsClose(front_conn, prep, rs);
        }
    }

    @GET
    @Path("/details/{uuid}/instance")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<String> loadInstanceDetails(@PathParam("uuid") String uuid) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        logger.refuuid(uuid);
        try {
            ArrayList<String> retList = new ArrayList<>();

            front_conn = factory.getConnection("frontend");

            prep = front_conn.prepareStatement("SELECT * FROM service_instance WHERE referenceUUID = ?");
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
            commonsClose(front_conn, prep, rs);
        }
    }

    @GET
    @Path("/details/{uuid}/verification")
    @Produces("application/json")
    @RolesAllowed("Panels")
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
            commonsClose(front_conn, prep, rs);
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
     * @apiExample {curl} Example Call:
     * curl -X DELETE http://localhost:8080/StackV-web/restapi/app/service/49f3d197-de3e-464c-aaa8-d3fe5f14af0b
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @GET
    @Path(value = "/details/{siUUID}/verification/drone")
    @RolesAllowed("Panels")
    public String hasVerifyDrone(@PathParam(value = "siUUID") final String refUUID) throws SQLException, IOException, InterruptedException {
        String method = "hasVerifyDrone";

        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            front_conn = factory.getConnection("frontend");

            prep = front_conn.prepareStatement("SELECT timestamp FROM service_verification "
                    + "WHERE instanceUUID = ?");
            prep.setString(1, refUUID);
            rs = prep.executeQuery();
            while (rs.next()) {
                BigInteger ONE_BILLION = new BigInteger("1000000000");
                Timestamp time = rs.getTimestamp(1);
                if (time == null) {
                    return "0";
                }
                Timestamp now = new Timestamp(System.currentTimeMillis());

                final BigInteger firstTime = BigInteger.valueOf(time.getTime() / 1000 * 1000).multiply(ONE_BILLION).add(BigInteger.valueOf(time.getNanos()));
                final BigInteger secondTime = BigInteger.valueOf(now.getTime() / 1000 * 1000).multiply(ONE_BILLION).add(BigInteger.valueOf(now.getNanos()));
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
            commonsClose(front_conn, prep, rs);
        }
        return "-1";
    }

    @GET
    @Path("/details/{uuid}/acl")
    @Produces("application/json")
    @RolesAllowed("Panels")
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
            commonsClose(front_conn, prep, rs);
        }
    }

    @GET
    @Path("/service/lastverify/{siUUID}")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public HashMap<String, String> getVerificationResults(@PathParam("siUUID") String serviceUUID) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        logger.refuuid(serviceUUID);
        try {
            HashMap<String, String> retMap = new HashMap<>();
            front_conn = factory.getConnection("frontend");
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
            throw ex;
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
            front_conn = factory.getConnection("frontend");

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
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    @GET
    @Path("/delta/{siUUID}")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public String getDeltaBacked(@PathParam("siUUID") String serviceUUID) throws IOException, SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
        try {
            front_conn = factory.getConnection("frontend");

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
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    @GET
    @Path("/access/{category}/{uuid}")
    @RolesAllowed("Panels")
    public String verifyPanel(@PathParam("category") String category, @PathParam("uuid") String uuid) throws SQLException {
        String method = "verifyPanel";
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
    @RolesAllowed("Profiles-R")
    public String getProfile(@PathParam("wizardID") int wizardID) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            if (verifyAccess("profiles", wizardID)) {
                String method = "getProfile";
                front_conn = factory.getConnection("frontend");
                JSONObject profJSON = new JSONObject();

                prep = front_conn.prepareStatement("SELECT * FROM service_wizard WHERE service_wizard_id = ?");
                prep.setInt(1, wizardID);
                rs = prep.executeQuery();
                while (rs.next()) {
                    profJSON.put("name", rs.getString("name"));
                    profJSON.put("wizard_json", rs.getString("wizard_json"));
                    profJSON.put("owner", rs.getString("owner"));
                    profJSON.put("editable", rs.getString("editable"));
                    profJSON.put("authorized", rs.getString("authorized"));
                    profJSON.put("description", rs.getString("description"));
                }

                prep = front_conn.prepareStatement("SELECT DISTINCT * FROM service_wizard_licenses WHERE service_wizard_id = ?");
                prep.setInt(1, wizardID);
                rs = prep.executeQuery();
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
            return "";
        } catch (SQLException ex) {
            logger.catching("getProfile", ex);
            throw ex;
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
    @RolesAllowed("Profiles-W")
    public void editProfile(@PathParam("wizardID") int wizardID, final String inputString) throws SQLException, ParseException {
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
                // TODO: Sanitize the input!
                prep = front_conn.prepareStatement("UPDATE service_wizard SET wizard_json = ?, editable = ?, last_edited = ? WHERE service_wizard_id = ? ");
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
            commonsClose(front_conn, prep, rs);
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
     * @apiExample {curl} Example Call:
     * curl -X PUT -d @newprofile.json -H "Content-Type: application/json"
     * http://localhost:8080/StackV-web/restapi/app/profile/11/edit
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @PUT
    @Path("/profile/{wizardID}/meta")
    @RolesAllowed("Profiles-W")
    public void editProfileMetadata(@PathParam("wizardID") int wizardID, final String inputString) throws SQLException, ParseException {
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
                // TODO: Sanitize the input!
                prep = front_conn.prepareStatement("UPDATE service_wizard SET name = ?, description = ? WHERE service_wizard_id = ? ");
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
            commonsClose(front_conn, prep, rs);
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
     * @apiExample {curl} Example Call:
     * curl -X PUT -d @newprofile.json -H "Content-Type: application/json"
     * http://localhost:8080/StackV-web/restapi/app/profile/11/edit
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @POST
    @Path("/profile/{wizardID}/licenses")
    @Consumes(value = {"application/json"})
    @RolesAllowed("Profiles-W")
    public void addProfileLicenses(@PathParam("wizardID") int wizardID, final String inputString) throws SQLException, ParseException {
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
                prep = front_conn.prepareStatement("INSERT INTO service_wizard_licenses (service_wizard_id, type, remaining, username) VALUES (?,?,?,?)");
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
            commonsClose(front_conn, prep, rs);
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
     * @apiExample {curl} Example Call:
     * curl -X PUT -d @newprofile.json -H "Content-Type: application/json"
     * http://localhost:8080/StackV-web/restapi/app/profile/11/edit
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @PUT
    @Path("/profile/{wizardID}/licenses")
    @Consumes(value = {"application/json"})
    @RolesAllowed("Profiles-W")
    public void editProfileLicenses(@PathParam("wizardID") int wizardID, final String inputString) throws SQLException, ParseException {
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
                    prep = front_conn.prepareStatement("DELETE FROM service_wizard_licenses WHERE service_wizard_id = ? AND username = ?");
                    prep.setInt(1, wizardID);
                    prep.setString(2, (String) inputJSON.get("username"));
                    prep.executeUpdate();
                } else {
                    prep = front_conn.prepareStatement("UPDATE service_wizard_licenses SET remaining = ?, type = ? WHERE service_wizard_id = ? AND username = ?");
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
            commonsClose(front_conn, prep, rs);
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
    @Consumes(value = {"application/json"})
    @RolesAllowed("Profiles-R")
    public String getProfileLicenseUsage(@PathParam("wizardID") int wizardID, @PathParam("username") String username) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        String method = "getProfileLicenseUsage";
        try {
            if (verifyAccess("profiles", wizardID)) {
                // Connect to the DB
                front_conn = factory.getConnection("frontend");

                prep = front_conn.prepareStatement("SELECT COUNT(*) FROM service_instance WHERE"
                        + " service_wizard_id = ? AND username = ?");
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
            commonsClose(front_conn, prep, rs);
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
     * @apiExample {curl} Example Call:
     * curl -X PUT -d @newprofile.json -H "Content-Type: application/json"
     * http://localhost:8080/StackV-web/restapi/app/profile/11/edit
     * -H "Authorization: bearer $KC_ACCESS_TOKEN"
     */
    @PUT
    @Path("/profile/new")
    @RolesAllowed("Profiles-W")
    public String newProfile(final String inputString) throws SQLException, ParseException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            String method = "newProfile";
            logger.start(method);

            front_conn = factory.getConnection("frontend");
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

            int authorized = (verifyUserRole("admin")) ? 1 : 0;
            prep = front_conn.prepareStatement("INSERT INTO `frontend`.`service_wizard` "
                    + "(owner, name, wizard_json, description, editable, authorized) VALUES (?, ?, ?, ?, ?, ?)");
            prep.setString(1, username);
            prep.setString(2, name);
            prep.setString(3, inputDataString);
            prep.setString(4, description);
            prep.setInt(5, 0);
            prep.setInt(6, authorized);
            prep.executeUpdate();

            prep = front_conn.prepareStatement("SELECT service_wizard_id FROM service_wizard WHERE owner = ? AND name = ?");
            prep.setString(1, username);
            prep.setString(2, name);
            rs = prep.executeQuery();
            rs.next();
            int wizardID = rs.getInt("service_wizard_id");

            if (licenseArr != null) {
                for (Object obj2 : licenseArr) {
                    JSONObject licenseObj = (JSONObject) obj2;
                    prep = front_conn.prepareStatement("INSERT INTO `frontend`.`service_wizard_licenses` "
                            + "(service_wizard_id, username, remaining) VALUES (?, ?, ?)");
                    prep.setInt(1, wizardID);
                    prep.setString(2, (String) licenseObj.get("username"));
                    prep.setInt(3, (Integer) licenseObj.get("remaining"));
                    prep.executeUpdate();
                }
            }

            logger.end(method);
            return null;
        } catch (SQLException | ParseException ex) {
            logger.catching("newProfile", ex);
            throw ex;
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
    @RolesAllowed("Profiles-W")
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
            commonsClose(front_conn, prep, rs);
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
    @Consumes(value = {"application/json", "application/xml"})
    @RolesAllowed("Profiles-E")
    public Response executeProfile(final String inputString) throws SQLException, IOException, ParseException, InterruptedException {
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
            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class
                    .getName());
            final AccessToken accessToken = securityContext.getToken();
            Set<String> roleSet = accessToken.getResourceAccess("StackV").getRoles();
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

            if (roleSet.contains(serviceType) || profileAuthorized == 1) {
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
                            } catch (SQLException | EJBException | IOException | InterruptedException | ParseException ex) {
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
                            } catch (SQLException | EJBException | IOException | InterruptedException | ParseException ex) {
                                logger.catching(method, ex);
                            }
                        }
                    });
                }
            } else {
                logger.status(method, "User " + username + " not authorized for service " + serviceType);
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            logger.end(method);
        } catch (ParseException ex) {
            logger.catching(method, ex);
        } finally {
            commonsClose(front_conn, prep, rs);
        }
        return Response.status(Response.Status.OK).build();
    }

    // >Services   
    @GET
    @Path("/service/{siUUID}/status")
    @RolesAllowed("Services")
    public String checkStatus(@PathParam("siUUID") String refUUID) throws SQLException, IOException {
        final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        final TokenHandler token = new TokenHandler(refresh);

        ServiceHandler instance = new ServiceHandler(refUUID, token);

        return instance.superState.name() + " - " + instance.status() + "\n";
    }

    @GET
    @Path("/service/{siUUID}/substatus")
    @RolesAllowed("Services")
    public String subStatus(@PathParam("siUUID") String refUUID) throws SQLException, IOException {
        final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        final TokenHandler token = new TokenHandler(refresh);

        ServiceHandler instance = new ServiceHandler(refUUID, token);

        return instance.status();
    }

    @POST
    @Path(value = "/service")
    @Consumes(value = {"application/json", "application/xml"})
    @RolesAllowed({"Services"})
    public String createService(final String inputString) throws IOException, EJBException, SQLException, InterruptedException {
        return createService(null, inputString);
    }

    @POST
    @Path(value = "/service/{siUUID}")
    @Consumes(value = {"application/json", "application/xml"})
    @RolesAllowed("Services")
    public String createService(@PathParam(value = "siUUID") final String siUUID, final String inputString) throws IOException, EJBException, SQLException, InterruptedException {
        final String method = "createService";
        try {
            logger.start(method, "Thread:" + Thread.currentThread());
            final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
            final TokenHandler token = new TokenHandler(refresh);
            Object obj = parser.parse(inputString);
            final JSONObject inputJSON = (JSONObject) obj;
            String serviceType = (String) inputJSON.get("service");

            // Authorize service.
            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class
                    .getName());
            final AccessToken accessToken = securityContext.getToken();
            Set<String> roleSet = accessToken.getResourceAccess("StackV").getRoles();
            String username = accessToken.getPreferredUsername();

            // Instance Creation
            final String refUUID;
            if (siUUID == null) {
                try {
                    URL url = new URL(String.format("%s/service/instance", host));
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    refUUID = executeHttpMethod(url, connection, "GET", null, token.auth());
                } catch (IOException ex) {
                    throw logger.throwing("doCreateService", ex);
                }
            } else {
                refUUID = siUUID;
            }

            if (roleSet.contains(serviceType)) {
                inputJSON.remove("username");
                inputJSON.put("username", username);
                inputJSON.put("uuid", refUUID);
                ((JSONObject) inputJSON.get("data")).put("uuid", refUUID);

                String sync = (String) inputJSON.get("synchronous");
                String proceed = (String) inputJSON.get("proceed");
                if (siUUID != null) { // negotiate instead of creating new
                    negotiateService(inputJSON, token, refUUID);
                } else if (sync != null && sync.equals("true")) {
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
                            } catch (SQLException | EJBException | IOException | InterruptedException | ParseException ex) {
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
                            } catch (SQLException | EJBException | IOException | InterruptedException | ParseException ex) {
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
    @RolesAllowed("Services")
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
    @RolesAllowed("Services")
    public String generateUUID() {
        return UUID.randomUUID().toString();
    }

    @PUT
    @Path(value = "/service/{siUUID}/superstate/{state}")
    @RolesAllowed("Services")
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
            prep = front_conn.prepareStatement("UPDATE `frontend`.`service_instance` SET `super_state` = ? WHERE `service_instance`.`referenceUUID` = ?");
            prep.setString(1, stateStr);
            prep.setString(2, refUUID);
            prep.executeUpdate();

            logger.end(method);
            return null;
        } catch (SQLException ex) {
            logger.catching(method, ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    @PUT
    @Path(value = "/service/{siUUID}/{action}")
    @RolesAllowed("Services")
    public String operate(@PathParam(value = "siUUID")
            final String refUuid, @PathParam(value = "action")
            final String action) throws IOException {
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
    @RolesAllowed("Services")
    public void operateSync(@PathParam(value = "siUUID")
            final String refUuid, @PathParam(value = "action")
            final String action) throws SQLException, InterruptedException, IOException {
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        final TokenHandler token = new TokenHandler(refresh);
        String method = "operateSync";
        logger.trace_start(method);
        doOperate(refUuid, action, token);
        logger.trace_end(method);
    }

    @GET
    @Path(value = "/service/{siUUID}/call_verify")
    @RolesAllowed("Services")
    public String callVerify(@PathParam(value = "siUUID")
            final String refUUID) throws SQLException, InterruptedException, IOException {
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        final TokenHandler token = new TokenHandler(refresh);
        String method = "callVerify";
        logger.trace_start(method);

        VerificationHandler verify = new VerificationHandler(refUUID, token, 1, 10, true);
        return verify.startVerification();
    }

    @DELETE
    @Path(value = "/service/{siUUID}")
    @RolesAllowed("Services")
    public void delete(@PathParam(value = "siUUID") final String refUuid) throws SQLException, IOException, InterruptedException {
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        final TokenHandler token = new TokenHandler(refresh);
        String method = "operate";
        logger.trace_start(method, "Thread:" + Thread.currentThread());
        doOperate(refUuid, "delete", token);
        logger.trace_end(method);
    }

    // Async Methods -----------------------------------------------------------
    private void doCreateService(JSONObject inputJSON, TokenHandler token, String refUUID, boolean autoProceed) throws EJBException, SQLException, IOException, InterruptedException, ParseException {
        TemplateEngine template = new TemplateEngine();

        String retString = template.apply(inputJSON);
        retString = retString.replace("&lt;", "<").replace("&gt;", ">");
        // hack - not a reliable way to tell public cloud from private (openstack)
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

        ServiceHandler instance = new ServiceHandler(inputJSON, token, refUUID, autoProceed);
    }

    private void negotiateService(JSONObject inputJSON, TokenHandler token, String refUUID) throws EJBException, SQLException, IOException, InterruptedException {
        TemplateEngine template = new TemplateEngine();

        System.out.println("\n\n\nTemplate Input:\n" + inputJSON.toString());
        String retString = template.apply(inputJSON);
        retString = retString.replace("&lt;", "<").replace("&gt;", ">");
        System.out.println("\n\n\nResult:\n" + retString);
        inputJSON.put("data", retString);
        ServiceEngine.orchestrateInstance(refUUID, inputJSON, (String) inputJSON.get("uuid"), token, false);
    }

    private String doOperate(@PathParam("siUUID") String refUUID, @PathParam("action") String action, TokenHandler token) throws SQLException, IOException, InterruptedException {
        ServiceHandler instance = new ServiceHandler(refUUID, token);
        instance.operate(action);

        return instance.superState.name() + " -- " + instance.status();
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

    private boolean verifyAccess(String category, int id) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        String method = "verifyAccess";
        try {
            front_conn = factory.getConnection("frontend");

            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class
                    .getName());
            final AccessToken accessToken = securityContext.getToken();
            Set<String> roleSet = accessToken.getResourceAccess("StackV").getRoles();
            String username = accessToken.getPreferredUsername();

            boolean result = false;
            switch (category) {
                case "profiles":
                    HashSet<String> nameSet = new HashSet<>();
                    prep = front_conn.prepareStatement("SELECT DISTINCT owner FROM service_wizard WHERE service_wizard_id = ?");
                    prep.setInt(1, id);
                    rs = prep.executeQuery();
                    while (rs.next()) {
                        nameSet.add(rs.getString("owner"));
                    }
                    prep = front_conn.prepareStatement("SELECT DISTINCT username FROM service_wizard_licenses WHERE service_wizard_id = ?");
                    prep.setInt(1, id);
                    rs = prep.executeQuery();
                    while (rs.next()) {
                        nameSet.add(rs.getString("username"));
                    }

                    result = nameSet.contains(username);
                    break;
            }

            if (result) {
                return true;
            } else {
                logger.warning(method, "User " + username + " refused access to resource [" + category + "], ID: " + id);
                return false;
            }
        } catch (SQLException ex) {
            logger.catching(method, ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }

    private boolean verifyAccess(String category, String uuid) throws SQLException {
        Connection front_conn = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        String method = "verifyAccess";
        try {
            front_conn = factory.getConnection("frontend");

            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class
                    .getName());
            final AccessToken accessToken = securityContext.getToken();
            Set<String> roleSet = accessToken.getResourceAccess("StackV").getRoles();
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
                logger.warning(method, "User " + username + " refused access to resource [" + category + "], UUID: " + uuid);
                return false;
            }
        } catch (SQLException ex) {
            logger.catching(method, ex);
            throw ex;
        } finally {
            commonsClose(front_conn, prep, rs);
        }
    }
}
