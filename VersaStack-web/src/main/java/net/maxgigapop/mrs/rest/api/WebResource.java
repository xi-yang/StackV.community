/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.rest.api;

import java.io.IOException;
import static java.lang.Thread.sleep;
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
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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

/**
 * REST Web Service
 *
 * @author rikenavadur
 */
@Path("app")
public class WebResource {        

    private final String front_db_user = "front_view";
    private final String front_db_pass = "frontuser";
    String host = "http://127.0.0.1:8080/VersaStack-web/restapi";
    private final serviceBeans servBean = new serviceBeans();
    JSONParser parser = new JSONParser();
    private final ExecutorService executorService = java.util.concurrent.Executors.newCachedThreadPool();

    @Context
    private UriInfo context;

    @EJB
    HandleSystemCall systemCallHandler;

    /**
     * Creates a new instance of WebResource
     */
    public WebResource() {
    }

    @GET
    @Path("/users")
    @Produces("application/json")
    public ArrayList<String> getUsers() throws SQLException {

        ArrayList<String> retList = new ArrayList<>();

        Connection front_conn;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
            Logger.getLogger(WebResource.class.getName()).log(Level.SEVERE, null, ex);
        }

        Properties front_connectionProps = new Properties();
        front_connectionProps.put("user", front_db_user);
        front_connectionProps.put("password", front_db_pass);
        front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                front_connectionProps);

        PreparedStatement prep = front_conn.prepareStatement("SELECT username FROM user_info");
        ResultSet rs1 = prep.executeQuery();
        while (rs1.next()) {
            retList.add(rs1.getString(1));
        }

        return retList;
    }

    @GET
    @Path("/service/{siUUID}/status")
    public String checkStatus(@PathParam("siUUID") String svcInstanceUUID) {
        String retString = "";
        try {
            return superStatus(svcInstanceUUID) + " - " + status(svcInstanceUUID) + "\n";
        } catch (SQLException | IOException e) {
            return "<<<CHECK STATUS ERROR: " + e.getMessage();
        }
    }

    @PUT
    @Path(value = "/service/{siUUID}/{action}")
    public void operate(@Suspended final AsyncResponse asyncResponse, @PathParam(value = "siUUID") final String refUuid, @PathParam(value = "action") final String action) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                asyncResponse.resume(doOperate(refUuid, action));
            }
        });
    }

    @POST
    @Path("/service")
    @Consumes({"application/json", "application/xml"})
    public String createService(String inputString) {
        try {
            long startTime = System.currentTimeMillis();
            System.out.println("Service API Start::Name="
                    + Thread.currentThread().getName() + "::ID="
                    + Thread.currentThread().getId());

            // Pull data from JSON.
            JSONObject inputJSON = new JSONObject();
            try {
                Object obj = parser.parse(inputString);
                inputJSON = (JSONObject) obj;

            } catch (ParseException ex) {
                Logger.getLogger(WebResource.class.getName()).log(Level.SEVERE, null, ex);
            }

            String serviceType = (String) inputJSON.get("type");
            JSONObject dataJSON = (JSONObject) inputJSON.get("data");
            String user = (String) inputJSON.get("user");
            String userID = "";

            // Find user ID.
            Connection front_conn;
            try {
                Class.forName("com.mysql.jdbc.Driver").newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException ex) {
                Logger.getLogger(WebResource.class.getName()).log(Level.SEVERE, null, ex);
            }

            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("SELECT user_id FROM user_info WHERE username = ?");
            prep.setString(1, user);
            ResultSet rs1 = prep.executeQuery();
            while (rs1.next()) {
                userID = rs1.getString("user_id");
            }

            // Instance Creation
            URL url = new URL(String.format("%s/service/instance", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String refUuid = servBean.executeHttpMethod(url, connection, "GET", null);

            // Create Parameter Map
            HashMap<String, String> paraMap = new HashMap<>();
            switch (serviceType) {
                case "dnc":
                    paraMap = parseDNC(dataJSON, refUuid);
                    break;
                case "netcreate":
                    paraMap = parseNet(dataJSON, refUuid);
                    break;
                default:
            }

            // Initialize service parameters.
            prep = front_conn.prepareStatement("SELECT service_id"
                    + " FROM service WHERE filename = ?");
            prep.setString(1, serviceType);
            rs1 = prep.executeQuery();
            rs1.next();
            int serviceID = rs1.getInt(1);
            Timestamp timeStamp = new Timestamp(System.currentTimeMillis());

            // Install Instance into DB.
            prep = front_conn.prepareStatement("INSERT INTO frontend.service_instance "
                    + "(`service_id`, `user_id`, `creation_time`, `referenceUUID`, `service_state_id`) VALUES (?, ?, ?, ?, ?)");
            prep.setInt(1, serviceID);
            prep.setString(2, userID);
            prep.setTimestamp(3, timeStamp);
            prep.setString(4, refUuid);
            prep.setInt(5, 1);
            prep.executeUpdate();

            // Replicate properties into DB.
            for (String key : paraMap.keySet()) {
                if (!paraMap.get(key).isEmpty()) {
                    url = new URL(String.format("%s/service/property/%s/%s/", host, refUuid, key));
                    connection = (HttpURLConnection) url.openConnection();
                    servBean.executeHttpMethod(url, connection, "POST", paraMap.get(key));
                }
            }

            // Execute service Creation.
            switch (serviceType) {
                case "dnc":
                    servBean.createConnection(paraMap);
                    break;
                case "netcreate":
                    servBean.createNetwork(paraMap);
                    break;
                default:
            }

            long endTime = System.currentTimeMillis();
            System.out.println("Service API End::Name="
                    + Thread.currentThread().getName() + "::ID="
                    + Thread.currentThread().getId() + "::Time Taken="
                    + (endTime - startTime) + " ms.");

            // Return instance UUID
            return refUuid;

        } catch (EJBException | SQLException | IOException e) {
            return e.getMessage();
        }

    }

    // Async Methods -----------------------------------------------------------
    private String doOperate(@PathParam("siUUID") String refUuid, @PathParam("action") String action) {
        long startTime = System.currentTimeMillis();
        System.out.println("Async API Operate Start::Name="
                + Thread.currentThread().getName() + "::ID="
                + Thread.currentThread().getId());
        
        try {
            switch (action) {
                case "propagate":
                    propagate(refUuid);
                    break;
                case "commit":
                    commit(refUuid);
                    break;
                case "revert":
                    setSuperState(refUuid, 2);
                    revert(refUuid);
                    break;                    

                case "cancel":
                    cancelInstance(refUuid);
                    break;
                    
                    
                case "delete":
                    deleteInstance(refUuid);

                    long endTime = System.currentTimeMillis();
                    System.out.println("Async API Operate End::Name="
                            + Thread.currentThread().getName() + "::ID="
                            + Thread.currentThread().getId() + "::Time Taken="
                            + (endTime - startTime) + " ms.");
                    return "Deletion Complete.\r\n";                    
                default:
                    return "Error! Invalid Action\n";
            }
            long endTime = System.currentTimeMillis();
            System.out.println("Async API Operate End::Name="
                    + Thread.currentThread().getName() + "::ID="
                    + Thread.currentThread().getId() + "::Time Taken="
                    + (endTime - startTime) + " ms.");

            return superStatus(refUuid) + " - " + status(refUuid) + "\r\n";
        } catch (IOException | SQLException ex) {
            return "Operation Error: " + ex.getMessage() + "\r\n";
        }
    }
    
    // Operation Methods -------------------------------------------------------
    /**
     * Deletes a service instance.
     * 
     * @param refUuid instance UUID
     * @return error code |
     */
    private int deleteInstance(String refUuid) throws SQLException {
        System.out.println("Deletion Beginning.");
        try {
            String result = delete(refUuid);
            System.out.println("Result from Backend: " + result);
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
                
                return 0;
            } else {
                return 1;
            }
        } catch (IOException ex) {
            return -1;
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
    private int cancelInstance(String refUuid) throws SQLException {
        boolean result;
        try {
            String instanceState = status(refUuid);
            if (!instanceState.equalsIgnoreCase("READY")) {
                return 1;
            }

            setSuperState(refUuid, 2);
            result = revert(refUuid);
            if (!result) {
                return 2;
            }
            result = propagate(refUuid);
            if (!result) {
                return 3;
            }
            result = commit(refUuid);
            if (!result) {
                return 4;
            }

            /*while (true) {
                instanceState = status(refUuid);
                if (instanceState.equals("READY")) {
                    return 0;
                } else if (!instanceState.equals("COMMITTED")) {
                    return 5;
                }
                sleep(5000);
            }*/
            return 0;
            
        } catch (IOException ex) {
            return -1;
        }
    }

    // Parsing Methods ---------------------------------------------------------
    private HashMap<String, String> parseDNC(JSONObject dataJSON, String refUuid) {
        HashMap<String, String> paraMap = new HashMap<>();
        paraMap.put("instanceUUID", refUuid);

        JSONArray linksArr = (JSONArray) dataJSON.get("links");
        for (int i = 0; i < linksArr.size(); i++) {
            JSONObject linksJSON = (JSONObject) linksArr.get(i);
            String name = (String) linksJSON.get("name");
            String src = (String) linksJSON.get("src");
            String srcVlan = (String) linksJSON.get("src-vlan");
            String des = (String) linksJSON.get("des");
            String desVlan = (String) linksJSON.get("des-vlan");

            String linkUrn = urnBuilder("dnc", name, refUuid);
            String connString = src + "&vlan_tag+" + srcVlan + "\r\n" + des + "&vlan_tag" + desVlan;

            paraMap.put("linkUri" + (i + 1), linkUrn);
            paraMap.put("conn" + (i + 1), connString);
        }

        System.out.println(paraMap);
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
        if (parent.contains("aws") || parent.contains("amazon")) {
            paraMap.put("driverType", "aws");
        } else if (parent.contains("openstack")) {
            paraMap.put("driverType", "os");
        }

        int VMCounter = 1;
        // Parse Subnets.
        JSONArray subArr = (JSONArray) vcnJSON.get("subnets");
        for (int i = 0; i < subArr.size(); i++) {
            JSONObject subJSON = (JSONObject) subArr.get(i);

            String subName = (String) subJSON.get("name");
            String subCidr = (String) subJSON.get("cidr");

            JSONArray vmArr = (JSONArray) subJSON.get("virtual_machines");
            if (vmArr != null) {
                for (Object vmEle : vmArr) {
                    JSONObject vmJSON = (JSONObject) vmEle;
                    String VMString = (String) vmJSON.get("name") + "&" + (i + 1);
                    paraMap.put("vm" + VMCounter++, VMString);
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

                routeString = routeString.substring(0, routeString.length() - 2);
            }

            String subString = "name+" + subName + "&cidr+" + subCidr;
            if (!routeString.isEmpty()) {
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

        // Parse Direct Connect.
        JSONArray gateArr = (JSONArray) vcnJSON.get("gateways");
        if (gateArr != null) {
            JSONObject gateJSON = (JSONObject) gateArr.get(0);
            JSONArray destArr = (JSONArray) gateJSON.get("to");
            if (destArr != null) {
                JSONObject destJSON = (JSONObject) destArr.get(0);
                paraMap.put("directConn", (String) destJSON.get("value"));
            }

        }

        System.out.println(paraMap);
        return paraMap;
    }    

    // Utility Methods ---------------------------------------------------------
    /*
    
     JSONArray Arr = (JSONArray) JSON.get("");
     for (int i = 0; i < Arr.size(); i++) {
     JSONObject JSON = (JSONObject) Arr.get(i);
     }
    
     */
    private String urnBuilder(String serviceType, String name, String refUuid) {
        switch (serviceType) {
            case "dnc":
                return "urn:ogf:network:service+" + refUuid + ":resource+links:tag+" + name;
            case "netcreate":
                return "urn:ogf:network:service+" + refUuid + ":resource+virtual_clouds:tag+" + name;
            default:
                return "ERROR";
        }
    }

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
    }

    private boolean propagate(String refUuid) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/propagate", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, propagate, "PUT", null);
        return result.equalsIgnoreCase("PROPAGATED");
    }

    private boolean commit(String refUuid) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/commit", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, propagate, "PUT", null);
        return result.equalsIgnoreCase("COMMITTED");
    }

    private boolean revert(String refUuid) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/revert", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, propagate, "PUT", null);
        return result.equalsIgnoreCase("COMMITTED-PARTIAL");
    }

    private String delete(String refUuid) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, propagate, "DELETE", null);

        return result;
    }

    private String superStatus(String refUuid) throws SQLException {
        Connection front_conn;
        Properties front_connectionProps = new Properties();
        front_connectionProps.put("user", front_db_user);
        front_connectionProps.put("password", front_db_pass);
        front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                front_connectionProps);

        PreparedStatement prep = front_conn.prepareStatement("SELECT X.superState FROM"
                + " service_instance I, service_state X WHERE I.referenceUUID = ? AND I.service_state_id = X.service_state_id");
        prep.setString(1, refUuid);
        ResultSet rs1 = prep.executeQuery();
        while (rs1.next()) {
            return rs1.getString("superState");
        }
        return "ERROR";
    }

    private String status(String refUuid) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/status", host, refUuid));
        HttpURLConnection status = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, status, "GET", null);

        return result;
    }

}
