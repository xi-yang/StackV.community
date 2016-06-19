    /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import net.maxgigapop.mrs.common.ModelUtil;

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
    @Path("/label/{user}")
    @Produces("application/json")
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

    @POST
    @Path(value = "/service")
    @Consumes(value = {"application/json", "application/xml"})
    public void createService(@Suspended final AsyncResponse asyncResponse, final String inputString) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                asyncResponse.resume(doCreateService(inputString));
            }
        });
    }

    @PUT
    @Path(value = "/service/{siUUID}/{action}")
    public void operate(@Suspended final AsyncResponse asyncResponse, @PathParam(value = "siUUID") final String refUuid, @PathParam(value = "action") final String action) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                asyncResponse.resume(doOperate(refUuid, action));
            }
        });
    }

    // Async Methods -----------------------------------------------------------
    private String doCreateService(String inputString) {
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
            String alias = (String) inputJSON.get("alias");
            String user = (String) inputJSON.get("user");
            String userID = "";

            JSONObject dataJSON = (JSONObject) inputJSON.get("data");
            
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
                case "fl2p":
                    paraMap = parseFlow(dataJSON, refUuid);
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
                    + "(`service_id`, `user_id`, `creation_time`, `referenceUUID`, `alias_name`, `service_state_id`) VALUES (?, ?, ?, ?, ?, ?)");
            prep.setInt(1, serviceID);
            prep.setString(2, userID);
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
            
            // Replicate properties into DB.
            for (String key : paraMap.keySet()) {
                if (!paraMap.get(key).isEmpty()) {
                    url = new URL(String.format("%s/service/property/%s/%s/", host, refUuid, key));
                    connection = (HttpURLConnection) url.openConnection();
                    servBean.executeHttpMethod(url, connection, "POST", paraMap.get(key));
                }
            }
            
            // Execute service creation.
            switch (serviceType) {
                case "dnc":
                    servBean.createConnection(paraMap);
                    break;
                case "netcreate":
                    servBean.createNetwork(paraMap);
                    break;
                case "fl2p":
                    servBean.createflow(paraMap);
                    break;
                default:
            }
            
            // Verify creation.
            verify(refUuid);

            long endTime = System.currentTimeMillis();
            System.out.println("Service API End::Name="
                    + Thread.currentThread().getName() + "::ID="
                    + Thread.currentThread().getId() + "::Time Taken="
                    + (endTime - startTime) + " ms.");

            // Return instance UUID
            return refUuid;

        } catch (EJBException | SQLException | IOException | InterruptedException e) {
            return "<<<CREATION ERROR: " + e.getMessage();
        }
    }

    private String doOperate(@PathParam("siUUID") String refUuid, @PathParam("action") String action) {
        long startTime = System.currentTimeMillis();
        System.out.println("Async API Operate Start::Name="
                + Thread.currentThread().getName() + "::ID="
                + Thread.currentThread().getId());
        long endTime;

        try {
            switch (action) {
                case "propagate":
                    propagate(refUuid, 0);
                    break;
                case "commit":
                    commit(refUuid, 0);
                    break;
                case "revert":
                    revert(refUuid, 0);
                    break;

                case "cancel":
                    setSuperState(refUuid, 2);
                    cancelInstance(refUuid);
                    break;

                case "reinstate":
                    setSuperState(refUuid, 4);
                    cancelInstance(refUuid);
                    break;

                case "delete":
                    deleteInstance(refUuid);

                    endTime = System.currentTimeMillis();
                    System.out.println("Async API Operate End::Name="
                            + Thread.currentThread().getName() + "::ID="
                            + Thread.currentThread().getId() + "::Time Taken="
                            + (endTime - startTime) + " ms.");
                    return "Deletion Complete.\r\n";

                case "verify":
                    verify(refUuid);

                    endTime = System.currentTimeMillis();
                    System.out.println("Async API Operate End::Name="
                            + Thread.currentThread().getName() + "::ID="
                            + Thread.currentThread().getId() + "::Time Taken="
                            + (endTime - startTime) + " ms.");
                    return "Verification Complete.\r\n";

                default:
                    return "Error! Invalid Action.\r\n";
            }
            
            endTime = System.currentTimeMillis();
            System.out.println("Async API Operate End::Name="
                    + Thread.currentThread().getName() + "::ID="
                    + Thread.currentThread().getId() + "::Time Taken="
                    + (endTime - startTime) + " ms.");

            return superStatus(refUuid) + " - " + status(refUuid) + "\r\n";
        } catch (IOException | SQLException | InterruptedException ex) {
            return "<<<OPERATION ERROR: " + ex.getMessage() + "\r\n";
        }
    }

    @GET
    @Path("/service/delta/{siUUID}")
    @Produces("application/json")
    public ArrayList<String> getDeltas(@PathParam("siUUID") String serviceUUID) {
        try {
            ArrayList<String> retList = new ArrayList<>();
            Properties front_connectionProps = new Properties();
            front_connectionProps.put("user", front_db_user);
            front_connectionProps.put("password", front_db_pass);
            Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("SELECT delta FROM service_instance I, service_delta D WHERE I.referenceUUID = ? AND D.service_instance_id = I.service_instance_id");
            prep.setString(1, serviceUUID);
            ResultSet rs1 = prep.executeQuery();
            while (rs1.next()) {
                retList.add(rs1.getString("delta"));
            }

            return retList;
        } catch (SQLException e) {
            Logger.getLogger(WebResource.class.getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }
    
    @GET
    @Path("/service/lastverify/{siUUID}")
    @Produces("application/json")
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
            Logger.getLogger(WebResource.class.getName()).log(Level.SEVERE, null, e);
            return null;
        }
    }

    @GET
    @Path("/service/availibleitems/{siUUID}")
    @Produces("application/json")
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
                      
           if (verified_addition  != null && unverified_reduction != null) {
                vAddition = ModelUtil.unmarshalOntModelJson(verified_addition);
                uReduction = ModelUtil.unmarshalOntModelJson(unverified_reduction);

                ArrayList<OntModel> modelList = new ArrayList<>();
                modelList.add(vAddition);
                modelList.add(uReduction);
                OntModel newModel = ModelUtil.createUnionOntModel(modelList);
                return ModelUtil.marshalOntModelJson(newModel);
           }
           
           if (verified_addition != null) return verified_addition;
           else if (unverified_reduction != null) return unverified_reduction;
           else return null;
           
        } catch (SQLException e) {
            Logger.getLogger(WebResource.class.getName()).log(Level.SEVERE, null, e);
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

            result = revert(refUuid, 2);
            if (!result) {
                return 2;
            }
            result = propagate(refUuid, 2);
            if (!result) {
                return 3;
            }
            result = commit(refUuid, 2);
            if (!result) {
                return 4;
            }

            while (true) {
                instanceState = status(refUuid);
                if (instanceState.equals("READY")) {                    
                    verify(refUuid);
                   
                    return 0;
                } else if (!instanceState.equals("COMMITTED")) {
                    return 5;
                }
                Thread.sleep(5000);
            }
            

        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(WebResource.class.getName()).log(Level.SEVERE, null, ex);
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

            String linkUrn = servBean.urnBuilder("dnc", name, refUuid);
            String connString = src + "&vlan_tag+" + srcVlan + "\r\n" + des + "&vlan_tag+" + desVlan;

            paraMap.put("linkUri" + (i + 1), linkUrn);
            paraMap.put("conn" + (i + 1), connString);
        }

        return paraMap;
    }

    
    /*
     paraMap.put("topoUri", "urn:ogf:network:openstack.com:openstack-cloud");
     paraMap.put("netCidr", "10.1.0.0/16");
     paraMap.put("driverType","ops");
     paraMap.put("subnet1", "name+ &cidr+10.0.0.0/24&routesto+0.0.0.0/0,nextHop+internet");
     paraMap.put("subnet2", "name+ &cidr+10.1.1.0/24");
     paraMap.put("vm1", "vm_OPS&1& &m1.medium&icecube_key&rains&msx1&206.196.180.148&urn:ogf:network:domain=dragon.maxgigapop.net:node=CLPK:port=1-2-3:link=*&aa:bb:cc:00:00:12&10.10.0.1/30&to+192.168.0.0/24,next_hop+10.10.0.2\r\nto+206.196.179.0/24,next_hop+10.10.0.2");
     //value format: "vm_name&subnet_index_number&image_type&instance_type&keypair_name&security_group_name&host&floating_IP&sriov_destination&sriov_mac_address&sriov_ip_address&sriov_routes"
     */
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
        for (int i = 0; i < subArr.size(); i++) {
            JSONObject subJSON = (JSONObject) subArr.get(i);

            String subName = (String) subJSON.get("name");
            String subCidr = (String) subJSON.get("cidr");

            // Parse VMs.
            JSONArray vmArr = (JSONArray) subJSON.get("virtual_machines");
            if (vmArr != null) {
                int vmCounter = 1;
                for (Object vmEle : vmArr) {
                    //value format: "vm_name & subnet_index_number & type_detail & host & interfaces"
                    JSONObject vmJSON = (JSONObject) vmEle;
                                                                                
                    // Name
                    String vmString = (String) vmJSON.get("name");
                    // Subnet Index
                    vmString += "&" + (i + 1);

                    // TYPES
                    vmString += vmJSON.containsKey("type")? "&" + (String) vmJSON.get("type") : "& ";

                    // VM Host
                    vmString += vmJSON.containsKey("host")? "&" + (String) vmJSON.get("host") : "& ";
                    
                    // INTERFACES
                    if(vmJSON.containsKey("interfaces")){
                        JSONArray interfaceArr = (JSONArray) vmJSON.get("interfaces");
                        vmString += "&" + interfaceArr.toString();
                    }else
                        vmString += "& ";
//                    HashMap<String, String> interfaceMap = new HashMap<>();
//                    int SRIOVCounter = 1;
//                    if (vmJSON.containsKey("interfaces")) {
//                        JSONArray interfaceArr = (JSONArray) vmJSON.get("interfaces");
//                        for (Object obj : interfaceArr) {
//                            JSONObject interfaceJSON = (JSONObject) obj;
//                            
//                            String typeString = (String) interfaceJSON.get("type");
//                            String addressString = (String) interfaceJSON.get("address");
//                            if (typeString.equalsIgnoreCase("Ethernet")) {
//                                interfaceMap.put("float", addressString.split("\\+")[1]);
//                            } else if (typeString.equalsIgnoreCase("SRIOV")) {
//                                
//                            }                            
//                        }                        
//                    }
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
        if(vcnJSON.get("gateways") != null){
            JSONArray gatewayArr = (JSONArray)vcnJSON.get("gateways");
            paraMap.put("gateways", gatewayArr.toString());
        }
            
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

    // Utility Methods ---------------------------------------------------------
    /*
    
     JSONArray Arr = (JSONArray) JSON.get("");
     for (int i = 0; i < Arr.size(); i++) {
     JSONObject JSON = (JSONObject) Arr.get(i);
     }
    
     */

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

    private boolean propagate(String refUuid, int stateId) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/propagate", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, propagate, "PUT", null);
        return result.equalsIgnoreCase("PROPAGATED");
    }

    private boolean commit(String refUuid, int stateId) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/commit", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, propagate, "PUT", null);
        return result.equalsIgnoreCase("COMMITTED");
    }

    private boolean revert(String refUuid, int stateId) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/revert", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, propagate, "PUT", null);
        
        // Revert now returns service delta UUID; pending changes.
        return true;
    }

    private String delete(String refUuid) throws MalformedURLException, IOException {
        URL url = new URL(String.format("%s/service/%s/", host, refUuid));
        HttpURLConnection propagate = (HttpURLConnection) url.openConnection();
        String result = servBean.executeHttpMethod(url, propagate, "DELETE", null);

        return result;
    }

    private boolean verify(String refUuid) throws MalformedURLException, IOException, InterruptedException, SQLException {
        int instanceID = servBean.getInstanceID(refUuid);

        Connection front_conn;
        Properties front_connectionProps = new Properties();
        front_connectionProps.put("user", front_db_user);
        front_connectionProps.put("password", front_db_pass);
        front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/frontend",
                front_connectionProps);
        PreparedStatement prep;

        for (int i = 1; i <= 5; i++) {
            boolean redVerified = true, addVerified = true;
            URL url = new URL(String.format("%s/service/verify/%s", host, refUuid));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String result = servBean.executeHttpMethod(url, conn, "GET", null);
            /*
             { 
             referenceUUID = service_delta_uuid,
             creationTime = date / time string,
             verifiedModelReduction = verified_model in string,
             unverifiedModelReduction = verified_model in string,
             reductionVerified = "true" or "false" in string,
             verifiedModelAddition = verified_model in string,
             unverifiedModelAddition = verified_model in string,
             additionVerified = "true" or "false" in string,
             }
             */
            
            // Pull data from JSON.
            JSONObject verifyJSON = new JSONObject();
            try {
                Object obj = parser.parse(result);
                verifyJSON = (JSONObject) obj;
            } catch (ParseException ex) {
                Logger.getLogger(WebResource.class.getName()).log(Level.SEVERE, null, ex);
                throw new IOException("Parse Error within Verification: " + ex.getMessage());
            }           
            
            // Update verification results cache.
            prep = front_conn.prepareStatement("UPDATE `service_verification` SET `delta_uuid`=?,`creation_time`=?,`verified_reduction`=?,`verified_addition`=?,`unverified_reduction`=?,`unverified_addition`=?,`reduction`=?,`addition`=?, `verification_run`=? WHERE `service_instance_id`=?");
            prep.setString(1, (String) verifyJSON.get("referenceUUID"));
            prep.setString(2, (String) verifyJSON.get("creationTime"));
            prep.setString(3, (String) verifyJSON.get("verifiedModelReduction"));
            prep.setString(4, (String) verifyJSON.get("verifiedModelAddition"));
            prep.setString(5, (String) verifyJSON.get("unverifiedModelReduction"));
            prep.setString(6, (String) verifyJSON.get("unverifiedModelAddition"));
            prep.setString(7, (String) verifyJSON.get("reductionVerified"));
            prep.setString(8, (String) verifyJSON.get("additionVerified"));
            prep.setInt(9, i);
            prep.setInt(10, instanceID);
            prep.executeUpdate();

            if (verifyJSON.containsKey("reductionVerified") && (verifyJSON.get("reductionVerified") != null) && ((String) verifyJSON.get("reductionVerified")).equals("false")) {
                redVerified = false;
            } 
            if (verifyJSON.containsKey("additionVerified") && (verifyJSON.get("additionVerified") != null) && ((String) verifyJSON.get("additionVerified")).equals("false")) {
                addVerified = false;
            }

            //System.out.println("Verify Result: " + result + "\r\n");
            if (redVerified && addVerified) {
                prep = front_conn.prepareStatement("UPDATE `frontend`.`service_verification` SET `verification_state` = '1' WHERE `service_verification`.`service_instance_id` = ?");
                prep.setInt(1, instanceID);
                prep.executeUpdate();

                return true;
            }

            prep = front_conn.prepareStatement("UPDATE `frontend`.`service_verification` SET `verification_state` = '0' WHERE `service_verification`.`service_instance_id` = ?");
            prep.setInt(1, instanceID);
            prep.executeUpdate();

            Thread.sleep(60000);
        }

        prep = front_conn.prepareStatement("UPDATE `frontend`.`service_verification` SET `verification_state` = '-1' WHERE `service_verification`.`service_instance_id` = ?");
        prep.setInt(1, instanceID);
        prep.executeUpdate();

        return false;
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
