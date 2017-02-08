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
import java.lang.String;
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
import java.util.logging.Level;
import java.util.logging.Logger;
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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.security.RolesAllowed;
import javax.net.ssl.HttpsURLConnection;
import net.maxgigapop.mrs.common.ModelUtil;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

/**
 * REST Web Service
 *
 * @author rikenavadur
 */
@Path("app")
public class WebResource {

    private static final Logger logger = Logger.getLogger(WebResource.class.getName());
    private final String front_db_user = "front_view";
    private final String front_db_pass = "frontuser";
    String host = "http://127.0.0.1:8080/StackV-web/restapi";
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

    // >ACL   
    @POST
    @Path(value = "/acl/{refUUID}")
    @Consumes(value = {"application/json", "application/xml"})
    @RolesAllowed("ACL")
    public void addACLEntry(@PathParam("refUUID") String refUUID, final String subject) {
        try {
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
        } catch (SQLException ex) {
            Logger.getLogger(WebResource.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @DELETE
    @Path(value = "/acl/{refUUID}")
    @Consumes(value = {"application/json", "application/xml"})
    @RolesAllowed("ACL")
    public void removeACLEntry(@PathParam("refUUID") String refUUID, final String subject) {
        try {
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
        } catch (SQLException ex) {
            Logger.getLogger(WebResource.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @GET
    @Path("/acl/{refUuid}")
    @Produces("application/json")
    @RolesAllowed("ACL")
    public ArrayList<ArrayList<String>> getACLwithInfo(@PathParam("refUuid") String refUUID) {
        try {
            ArrayList<ArrayList<String>> retList = new ArrayList<>();
            ArrayList<String> sqlList = new ArrayList<>();
            Connection front_conn;
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("SELECT A.subject FROM acl A WHERE A.object = ?");
            prep.setString(1, refUUID);
            ResultSet rs1 = prep.executeQuery();

            while (rs1.next()) {
                sqlList.add(rs1.getString("subject"));
            }

            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL("https://k152.maxgigapop.net:8543/auth/admin/realms/StackV/users");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", auth);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            System.out.println(conn.getResponseCode() + " - " + conn.getResponseMessage());
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

            return retList;
        } catch (SQLException | IOException | ParseException e) {
            Logger.getLogger(WebResource.class
                    .getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }

    // >Drivers
    @DELETE
    @Path(value = "/driver/{username}/delete/{topuri}")
    @RolesAllowed("Drivers")
    public String deleteDriverProfile(@PathParam(value = "username") String username, @PathParam(value = "topuri") String topuri) throws SQLException {
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
    }

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

    @GET
    @Path("/driver/{user}/get")
    @Produces("application/json")
    @RolesAllowed("Drivers")
    public ArrayList<String> getDriver(@PathParam("user") String username) throws SQLException {
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
    }

    @PUT
    @Path("install/driver")
    @Consumes("application/json")
    @Produces("text/plain")
    @RolesAllowed("Drivers")
    public String installDriver(final String dataInput) throws SQLException, ParseException {
        String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");

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
        } catch (Exception e) {
            return "PLUGIN FAILED: Exception" + e;
        }

        return "PLUGIN SUCCEEDED";
    }

    @PUT
    @Path("/driver/{user}/install/{topuri}")
    @Produces("text/plain")
    @RolesAllowed("Drivers")
    public String installDriverProfile(@PathParam("user") String username, @PathParam(value = "topuri") String topuri) throws SQLException, ParseException {
        String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");

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
        } catch (Exception e) {
            return "PLUGIN FAILED: Exception" + e;
        }

        return "PLUGIN SUCCEEDED";
    }

    @PUT
    @Path("driver/{user}/add")
    @Consumes(value = {"application/json"})
    @RolesAllowed("Drivers")
    public void addDriver(@PathParam("user") String username, final String dataInput) throws SQLException {
        JSONObject inputJSON = new JSONObject();
        try {
            Object obj = parser.parse(dataInput);
            inputJSON = (JSONObject) obj;

        } catch (ParseException ex) {
            Logger.getLogger(WebResource.class.getName()).log(Level.SEVERE, null, ex);
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
    }

    // >Keycloak
    @GET
    @Path("/keycloak/users")
    @Produces("application/json")
    @RolesAllowed("Keycloak")
    public ArrayList<ArrayList<String>> getUserInfo() {
        try {
            ArrayList<ArrayList<String>> retList = new ArrayList<>();
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL("https://k152.maxgigapop.net:8543/auth/admin/realms/StackV/users");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", auth);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            System.out.println(conn.getResponseCode() + " - " + conn.getResponseMessage());
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

            return retList;
        } catch (IOException | ParseException e) {
            Logger.getLogger(WebResource.class
                    .getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }

    @GET
    @Path("/keycloak/users/{user}/groups")
    @Produces("application/json")
    @RolesAllowed("Keycloak")
    public ArrayList<String> getUserGroups(@PathParam("user") String subject) {
        try {
            ArrayList<String> retList = new ArrayList<>();
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL("https://k152.maxgigapop.net:8543/auth/admin/realms/StackV/users/" + subject + "/groups");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", auth);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            System.out.println(conn.getResponseCode() + " - " + conn.getResponseMessage());
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
                JSONObject groupJSON = (JSONObject) group;
                retList.add((String) groupJSON.get("name"));
            }

            return retList;
        } catch (IOException | ParseException e) {
            Logger.getLogger(WebResource.class
                    .getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }

    @GET
    @Path("/keycloak/users/{user}/roles")
    @Produces("application/json")
    @RolesAllowed("Keycloak")
    public ArrayList<ArrayList<String>> getUserRoles(@PathParam("user") String subject) {
        try {
            ArrayList<ArrayList<String>> retList = new ArrayList<>();
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL("https://k152.maxgigapop.net:8543/auth/admin/realms/StackV/users/" + subject + "/role-mappings/clients/" + keycloakStackVClientID + "/composite");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", auth);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            System.out.println(conn.getResponseCode() + " - " + conn.getResponseMessage());
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
            return retList;
        } catch (IOException | ParseException e) {
            Logger.getLogger(WebResource.class
                    .getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }

    @POST
    @Path("/keycloak/users/{user}/roles")
    @Produces("application/json")
    @RolesAllowed("Keycloak")
    public void addUserRole(@PathParam("user") String subject, final String inputString) {
        try {
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL("https://k152.maxgigapop.net:8543/auth/admin/realms/StackV/users/" + subject + "/role-mappings/clients/" + keycloakStackVClientID + "");
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

            System.out.println(conn.getResponseCode() + " - " + conn.getResponseMessage());            
        } catch (IOException | ParseException e) {
            Logger.getLogger(WebResource.class
                    .getName()).log(Level.SEVERE, null, e);
        }
    }
        
    @DELETE
    @Path("/keycloak/users/{user}/roles")
    @Produces("application/json")
    @RolesAllowed("Keycloak")
    public void removeUserRole(@PathParam("user") String subject, final String inputString) {
        try {
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL("https://k152.maxgigapop.net:8543/auth/admin/realms/StackV/users/" + subject + "/role-mappings/clients/" + keycloakStackVClientID + "");
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

            System.out.println(conn.getResponseCode() + " - " + conn.getResponseMessage());            
        } catch (IOException | ParseException e) {
            Logger.getLogger(WebResource.class
                    .getName()).log(Level.SEVERE, null, e);
        }
    }
    
    @GET
    @Path("/keycloak/roles")
    @Produces("application/json")
    @RolesAllowed("Keycloak")
    public ArrayList<ArrayList<String>> getRoles() {
        try {
            ArrayList<ArrayList<String>> retList = new ArrayList<>();
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL("https://k152.maxgigapop.net:8543/auth/admin/realms/StackV/clients/" + keycloakStackVClientID + "/roles");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", auth);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            System.out.println(conn.getResponseCode() + " - " + conn.getResponseMessage());
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

            return retList;
        } catch (IOException | ParseException e) {
            Logger.getLogger(WebResource.class
                    .getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }

    @GET
    @Path("/keycloak/groups")
    @Produces("application/json")
    @RolesAllowed("Keycloak")
    public ArrayList<ArrayList<String>> getGroups() {
        try {
            ArrayList<ArrayList<String>> retList = new ArrayList<>();
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL("https://k152.maxgigapop.net:8543/auth/admin/realms/StackV/roles");
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", auth);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            System.out.println(conn.getResponseCode() + " - " + conn.getResponseMessage());
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

            return retList;
        } catch (IOException | ParseException e) {
            Logger.getLogger(WebResource.class
                    .getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }

    // >Labels
    @GET
    @Path("/label/{user}")
    @Produces("application/json")
    @RolesAllowed("Labels")
    public ArrayList<ArrayList<String>> getLabels(@PathParam("user") String username) {
        ArrayList<ArrayList<String>> retList = new ArrayList<>();
        try {

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

            return retList;
        } catch (SQLException e) {
            Logger.getLogger(WebResource.class.getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }

    @PUT
    @Path(value = "/label")
    @Consumes(value = {"application/json", "application/xml"})
    @RolesAllowed("Labels")
    public String label(final String inputString) {
        JSONObject inputJSON = new JSONObject();
        try {
            Object obj = parser.parse(inputString);
            inputJSON = (JSONObject) obj;

        } catch (ParseException ex) {
            Logger.getLogger(WebResource.class.getName()).log(Level.SEVERE, null, ex);
        }

        String user = (String) inputJSON.get("user");
        String identifier = (String) inputJSON.get("identifier");
        String label = (String) inputJSON.get("label");
        String color = (String) inputJSON.get("color");

        try {
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
        } catch (SQLException ex) {
            return "<<<Failed - " + ex.getMessage() + " - " + ex.getSQLState();
        }
        return "Added";
    }

    @DELETE
    @Path(value = "/label/{username}/delete/{identifier}")
    @RolesAllowed("Labels")
    public String deleteLabel(@PathParam(value = "username") String username, @PathParam(value = "identifier") String identifier) {
        try {
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("DELETE FROM `frontend` .`label` WHERE username = ? AND identifier = ?");
            prep.setString(1, username);
            prep.setString(2, identifier);
            prep.executeUpdate();
        } catch (SQLException ex) {
            return "<<<Failed - " + ex.getMessage() + " - " + ex.getSQLState();
        }
        return "Deleted";
    }

    @DELETE
    @Path(value = "/label/{username}/clearall")
    @RolesAllowed("Labels")
    public String clearLabels(@PathParam(value = "username") String username) {
        try {
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("DELETE FROM `frontend`.`label` WHERE username = ? ");
            prep.setString(1, username);
            prep.executeUpdate();
        } catch (SQLException ex) {
            return "<<<Failed - " + ex.getMessage() + " - " + ex.getSQLState();
        }
        return "Labels Cleared";
    }

    // >Manifests
    @GET
    @Path("/manifest/{svcUUID}")
    @Produces("application/json")
    @RolesAllowed("Manifests")
    public String getManifest(@PathParam("svcUUID") String svcUUID) {
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
        if (obj == null || !obj.has("jsonTemplate")) {
            throw new EJBException("getManifest cannot get manifest for service uuid=" + svcUUID);
        }
        return obj.getString("jsonTemplate");
    }

    @GET
    @Path("/manifest/{svcUUID}")
    @Produces("application/xml")
    @RolesAllowed("Manifests")
    public String getManifestXml(@PathParam("svcUUID") String svcUUID) {
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
    public ArrayList<ArrayList<String>> loadInstances(@PathParam("userId") String userId) throws SQLException {
        ArrayList<ArrayList<String>> retList = new ArrayList<>();
        ArrayList<String> banList = new ArrayList<>();
        String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");

        // Verify user
        String username = authUsername(userId);
        if (username == null) {
            logger.log(Level.WARNING, "Logged-in user does not match requested user information!");
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
                    logger.log(Level.INFO, "Instance Status Check Failed on UUID = {0}", instanceUUID);
                }
            }
        }

        return retList;
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
                logger.log(Level.WARNING, "Logged-in user does not match requested user information!");
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
        } catch (SQLException e) {
            Logger.getLogger(WebResource.class.getName()).log(Level.SEVERE, null, e);
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
                ArrayList<String> wizardList = new ArrayList<>();
                wizardList.add(rs1.getString("name"));
                wizardList.add(rs1.getString("description"));
                wizardList.add(rs1.getString("filename"));

                retList.add(wizardList);
            }
            return retList;
        } catch (SQLException e) {
            Logger.getLogger(WebResource.class
                    .getName()).log(Level.SEVERE, null, e);
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
        } catch (SQLException e) {
            Logger.getLogger(WebResource.class
                    .getName()).log(Level.SEVERE, null, e);
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
        } catch (SQLException e) {
            Logger.getLogger(WebResource.class
                    .getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }

    @GET
    @Path("/details/{uuid}/instance")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<String> loadInstanceDetails(@PathParam("uuid") String uuid) throws SQLException {
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
    }

    @GET
    @Path("/details/{uuid}/delta")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<ArrayList<String>> loadInstanceDelta(@PathParam("uuid") String uuid) throws SQLException {
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
    }

    @GET
    @Path("/details/{uuid}/verification")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<String> loadInstanceVerification(@PathParam("uuid") String uuid) throws SQLException {
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
    }

    @GET
    @Path("/details/{uuid}/acl")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public ArrayList<String> loadInstanceACL(@PathParam("uuid") String uuid) throws SQLException {
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
    }

    @GET
    @Path("/service/lastverify/{siUUID}")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public HashMap<String, String> getVerificationResults(@PathParam("siUUID") String serviceUUID) {
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

        } catch (SQLException e) {
            Logger.getLogger(WebResource.class
                    .getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }

    @GET
    @Path("/service/availibleitems/{siUUID}")
    @Produces("application/json")
    @RolesAllowed("Panels")
    public String getVerificationResultsUnion(@PathParam("siUUID") String serviceUUID) throws Exception {
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

        } catch (SQLException e) {
            Logger.getLogger(WebResource.class
                    .getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }

    // >Profiles
    @GET
    @Path("/profile/{wizardId}")
    @Produces("application/json")
    @RolesAllowed("Profiles")
    public String getProfile(@PathParam("wizardId") int wizardId) {
        try {
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("SELECT wizard_json FROM service_wizard WHERE service_wizard_id = ?");
            prep.setInt(1, wizardId);
            ResultSet rs1 = prep.executeQuery();
            while (rs1.next()) {
                return rs1.getString(1);
            }

            return "";

        } catch (SQLException e) {
            Logger.getLogger(WebResource.class
                    .getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }

    // Edit an existing profile
    @PUT
    @Path("/profile/{wizardId}/edit")
    @RolesAllowed("Profiles")
    public void editProfile(@PathParam("wizardId") int wizardId, final String inputString) {
        try {
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

        } catch (SQLException e) {
            Logger.getLogger(WebResource.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    // Create a new profile based on an existing one
    @PUT
    @Path("/profile/new")
    @RolesAllowed("Profiles")
    public String newProfile(final String inputString) {
        try {
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
            return null;
        } catch (SQLException | ParseException e) {
            Logger.getLogger(WebResource.class.getName()).log(Level.SEVERE, null, e);
            return e.toString();
        }
    }

    @DELETE
    @Path("/profile/{wizardId}")
    @RolesAllowed("Profiles")
    public void deleteProfile(@PathParam("wizardId") int wizardId) {
        try {
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("DELETE FROM service_wizard WHERE service_wizard_id = ?");
            prep.setInt(1, wizardId);
            prep.executeUpdate();

        } catch (SQLException e) {
            Logger.getLogger(WebResource.class
                    .getName()).log(Level.SEVERE, null, e);
        }
    }

    // >Services
    @GET
    @Path("/service/{siUUID}/status")
    @RolesAllowed("Services")
    public String checkStatus(@PathParam("siUUID") String svcInstanceUUID) {
        String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        if (refresh != null) {
            auth = servBean.refreshToken(refresh);
        }

        try {
            Thread.sleep(300);
            return superStatus(svcInstanceUUID) + " - " + status(svcInstanceUUID, auth) + "\n";
        } catch (SQLException | IOException | InterruptedException ex) {
            return "<<<CHECK STATUS ERROR: " + ex.getMessage();
        }
    }

    @GET
    @Path("/service/{siUUID}/substatus")
    @RolesAllowed("Services")
    public String subStatus(@PathParam("siUUID") String svcInstanceUUID) {
        String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
        final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
        if (refresh != null) {
            auth = servBean.refreshToken(refresh);
        }

        try {
            Thread.sleep(300);
            return status(svcInstanceUUID, auth);
        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(WebResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @POST
    @Path(value = "/service")
    @Consumes(value = {"application/json", "application/xml"})
    @RolesAllowed("Services")
    public void createService(@Suspended
            final AsyncResponse asyncResponse, final String inputString) {
        try {
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            final String refresh = httpRequest.getHttpHeaders().getHeaderString("Refresh");
            Object obj = parser.parse(inputString);
            final JSONObject inputJSON = (JSONObject) obj;
            String serviceType = (String) inputJSON.get("type");

            // Authorize service.
            KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest.getAttribute(KeycloakSecurityContext.class
                    .getName());
            final AccessToken accessToken = securityContext.getToken();

            String username = accessToken.getPreferredUsername();
            System.out.println("User:" + username);
            inputJSON.remove("username");
            inputJSON.put("username", username);

            //System.out.println("Service API:: inputJSON: " + inputJSON.toJSONString());
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    asyncResponse.resume(doCreateService(inputJSON, auth, refresh));
                }
            });

        } catch (ParseException ex) {
            Logger.getLogger(WebResource.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

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
            System.out.println("Service API Start::Name="
                    + Thread.currentThread().getName() + "::ID="
                    + Thread.currentThread().getId());

            String serviceType = (String) inputJSON.get("type");
            String alias = (String) inputJSON.get("alias");
            String username = (String) inputJSON.get("username");

            JSONObject dataJSON = (JSONObject) inputJSON.get("data");

            // Find user ID.
            Connection front_conn;
            try {
                Class.forName("com.mysql.jdbc.Driver").newInstance();

            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
                Logger.getLogger(WebResource.class
                        .getName()).log(Level.SEVERE, null, ex);
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
            String refUuid = servBean.executeHttpMethod(url, connection, "GET", null, auth);

            // Create Parameter Map
            HashMap<String, String> paraMap = new HashMap<>();
            switch (serviceType) {
                case "dnc":
                    paraMap = parseDNC(dataJSON, refUuid);
                    break;
                case "netcreate":
                    paraMap = parseNet(dataJSON, refUuid);
                    break;
                case "fl2p":
                    paraMap = parseFlow(dataJSON, refUuid);
                    break;
                case "hybridcloud":
                    paraMap = parseHybridCloud(dataJSON, refUuid);
                    break;
                case "omm":
                    paraMap = parseOperatationalModifications(dataJSON, refUuid);
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
            prep.setString(4, refUuid);
            prep.setString(5, alias);
            prep.setInt(6, 1);
            prep.executeUpdate();

            int instanceID = servBean.getInstanceID(refUuid);

            prep = front_conn.prepareStatement("INSERT INTO `frontend`.`service_history` "
                    + "(`service_history_id`, `service_state_id`, `service_instance_id`) VALUES (1, 1, ?)");
            prep.setInt(1, instanceID);
            prep.executeUpdate();

            prep = front_conn.prepareStatement("INSERT INTO `frontend`.`service_verification` "
                    + "(`service_instance_id`) VALUES (?)");
            prep.setInt(1, instanceID);
            prep.executeUpdate();

            prep = front_conn.prepareStatement("INSERT INTO `frontend`.`acl` (`subject`, `is_group`, `object`) "
                    + "VALUES (?, '0', ?)");
            prep.setString(1, username);
            prep.setString(2, refUuid);
            prep.executeUpdate();

            System.out.println("Past Initialization");

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

            System.out.println("Past Creation");

            long endTime = System.currentTimeMillis();
            System.out.println("Service API End::Name="
                    + Thread.currentThread().getName() + "::ID="
                    + Thread.currentThread().getId() + "::Time Taken="
                    + (endTime - startTime) + " ms.");

            // Return instance UUID
            return refUuid;

        } catch (EJBException | SQLException | IOException e) {
            System.out.println("<<<CREATION ERROR: " + e.getMessage());
            return "<<<CREATION ERROR: " + e.getMessage();
        }
    }

    private String doOperate(@PathParam("siUUID") String refUuid, @PathParam("action") String action, String auth, String refresh) {
        long startTime = System.currentTimeMillis();
        System.out.println("Async API Operate (" + action + ") Start::Name="
                + Thread.currentThread().getName() + "::ID="
                + Thread.currentThread().getId());
        long endTime;

        try {
            clearVerification(refUuid);
            switch (action) {
                case "cancel":
                    setSuperState(refUuid, 2);
                    cancelInstance(refUuid, auth, refresh);
                    break;
                case "force_cancel":
                    setSuperState(refUuid, 2);
                    forceCancelInstance(refUuid, auth, refresh);
                    break;

                case "reinstate":
                    setSuperState(refUuid, 4);
                    cancelInstance(refUuid, auth, refresh);
                    break;
                case "force_reinstate":
                    setSuperState(refUuid, 4);
                    forceCancelInstance(refUuid, auth, refresh);
                    break;

                case "force_retry":
                    forceRetryInstance(refUuid, auth, refresh);
                    break;

                case "delete":
                case "force_delete":
                    deleteInstance(refUuid, auth);

                    endTime = System.currentTimeMillis();
                    System.out.println("Async API Operate (" + action + ") End::Name="
                            + Thread.currentThread().getName() + "::ID="
                            + Thread.currentThread().getId() + "::Time Taken="
                            + (endTime - startTime) + " ms.");
                    return "Deletion Complete.\r\n";

                case "verify":
                    servBean.verify(refUuid, refresh);

                    endTime = System.currentTimeMillis();
                    System.out.println("Async API Operate (" + action + ") End::Name="
                            + Thread.currentThread().getName() + "::ID="
                            + Thread.currentThread().getId() + "::Time Taken="
                            + (endTime - startTime) + " ms.");
                    return "Verification Complete.\r\n";

                default:
                    return "Error! Invalid Action.\r\n";
            }

            endTime = System.currentTimeMillis();
            System.out.println("Async API Operate (" + action + ") End::Name="
                    + Thread.currentThread().getName() + "::ID="
                    + Thread.currentThread().getId() + "::Time Taken="
                    + (endTime - startTime) + " ms.");

            auth = servBean.refreshToken(refresh);
            return superStatus(refUuid) + " - " + status(refUuid, auth) + "\r\n";
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
                prep.setString(1, refUuid);
                prep.executeUpdate();
            } catch (SQLException ex2) {
                Logger.getLogger(serviceBeans.class.getName()).log(Level.SEVERE, null, ex2);
            }
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
        } catch (IOException | SQLException e) {
            Logger.getLogger(WebResource.class
                    .getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }

    /*
    @GET
    @Path("/manifest/{svcUUID}")
    @Produces("application/json")
    public String getManifest(@PathParam("svcUUID") String svcUUID) {
        try {
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);
            PreparedStatement prep = front_conn.prepareStatement("select v.verified_addition, s.name, si.alias_name "
                    + " from service_instance as si, service_verification as v, service as s "
                    + " where v.service_instance_id = si.service_instance_id and si.referenceUUID = ? "
                    + " and si.service_id = s.service_id");
            prep.setString(1, svcUUID);
            ResultSet rs1 = prep.executeQuery();
            if (!rs1.next()) {
                throw new EJBException("Unrecognized service UUID=" + svcUUID);
            }
            String verifiedModel = rs1.getString("v.verified_addition");
            String serviceType = rs1.getString("s.name");
            String serviceAlias = rs1.getString("si.alias_name");
            JSONObject jsonManifest = new JSONObject();
            jsonManifest.put("uuid", svcUUID);
            jsonManifest.put("type", serviceType);
            jsonManifest.put("alias", serviceAlias);
            //@TODO: get /restapi/model through API for now / will use frontend cache in future
            JSONObject jsonManifestData = ServiceManifest.generateManifest(verifiedModel, serviceType);
            jsonManifest.put("manifest", jsonManifestData);
            return jsonManifest.toJSONString();

        } catch (SQLException e) {
            Logger.getLogger(WebResource.class
                    .getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }
     */
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
            Logger.getLogger(serviceBeans.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(serviceBeans.class.getName()).log(Level.SEVERE, null, ex);
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
    private void setSuperState(String refUuid, int superStateId) throws SQLException {
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
    }

    private boolean propagate(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/propagate", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, propagate, "PUT", null, auth);
        logger.log(Level.INFO, "Sending Propagate Command");
        logger.log(Level.INFO, "Response Code : {0}", result);

        return result.equalsIgnoreCase("PROPAGATED");
    }

    private boolean forcePropagate(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/propagate_forcedretry", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, propagate, "PUT", null, auth);
        logger.log(Level.INFO, "Sending Forced Propagate Command");
        logger.log(Level.INFO, "Response Code : {0}", result);

        return result.equalsIgnoreCase("PROPAGATED");
    }

    private boolean commit(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/commit", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, propagate, "PUT", null, auth);
        logger.log(Level.INFO, "Sending Commit Command");
        logger.log(Level.INFO, "Response Code : {0}", result);

        return result.equalsIgnoreCase("COMMITTED");
    }

    private boolean forceCommit(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/commit_forced", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, propagate, "PUT", null, auth);
        logger.log(Level.INFO, "Sending Forced Commit Command");
        logger.log(Level.INFO, "Response Code : {0}", result);

        return result.equalsIgnoreCase("COMMITTED");
    }

    private boolean revert(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/revert", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, propagate, "PUT", null, auth);
        logger.log(Level.INFO, "Sending Revert Command");
        logger.log(Level.INFO, "Response Code : {0}", result);

        // Revert now returns service delta UUID; pending changes.
        return true;
    }

    private boolean forceRevert(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/revert_forced", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, propagate, "PUT", null, auth);
        logger.log(Level.INFO, "Sending Forced Revert Command");
        logger.log(Level.INFO, "Response Code : {0}", result);

        // Revert now returns service delta UUID; pending changes.
        return true;
    }

    private String delete(String refUuid, String auth) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, propagate, "DELETE", null, auth);
        logger.log(Level.INFO, "Sending Delete Command");
        logger.log(Level.INFO, "Response Code : {0}", result);

        return result;
    }

    private boolean clearVerification(String refUuid) throws SQLException {
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
    }

    private String superStatus(String refUuid) throws SQLException {
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
            Logger.getLogger(WebResource.class.getName()).log(Level.SEVERE, null, ex);
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
}
