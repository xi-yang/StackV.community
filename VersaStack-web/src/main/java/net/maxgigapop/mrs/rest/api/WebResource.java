/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.rest.api;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
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
import net.maxgigapop.mrs.rest.api.model.WebServiceBase;
import net.maxgigapop.mrs.system.HandleSystemCall;
import web.beans.serviceBeans;

/**
 * REST Web Service
 *
 * @author rikenavadur
 */
@Path("web")
public class WebResource {

    private final String front_db_user = "front_view";
    private final String front_db_pass = "frontuser";
    String host = "http://localhost:8080/VersaStack-web/restapi";
    private final serviceBeans servBean = new serviceBeans();

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
        front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Frontend",
                front_connectionProps);

        PreparedStatement prep = front_conn.prepareStatement("SELECT username FROM user_info");
        ResultSet rs1 = prep.executeQuery();
        while (rs1.next()) {
            retList.add(rs1.getString(1));
        }

        return retList;
    }

    @POST
    @Path("/service")
    @Consumes({"application/json", "application/xml"})
    public String createService(WebServiceBase input) {
        try {
            long startTime = System.currentTimeMillis();
            System.out.println("Service API Start::Name="
                    + Thread.currentThread().getName() + "::ID="
                    + Thread.currentThread().getId());

            // Pull data from JSON.
            String serviceType = input.getServiceType();
            String user = input.getUser();
            String userID = "";
            String rawServiceData= input.getServiceData();
          
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
            front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/Frontend",
                    front_connectionProps);

            PreparedStatement prep = front_conn.prepareStatement("SELECT user_id FROM user_info WHERE username = ?");
            prep.setString(1, user);
            ResultSet rs1 = prep.executeQuery();
            while (rs1.next()) {
                userID = rs1.getString("user_id");
            }

            // Create Parameter Map
            HashMap<String, String> paraMap = new HashMap<>();
            paraMap.put("userID", userID);
            
            List<String> stage1Arr;
            switch(serviceType) {
                case "dnc":
                    stage1Arr = Arrays.asList(rawServiceData.split("\\s*,\\s*"));
                    for (String ele : stage1Arr) {
                        String[] stage2Arr = ele.split("#");
                        paraMap.put(stage2Arr[0], stage2Arr[1]);
                    }                    
                    break;
                case "netcreate":
                    stage1Arr = Arrays.asList(rawServiceData.split("\\s*,\\s*"));
                    for (String ele : stage1Arr) {
                        String[] stage2Arr = ele.split("#");
                        paraMap.put(stage2Arr[0], stage2Arr[1].replaceAll("!", ","));
                    }          
                    break;
                default:
            }       

            // Instance Creation
            URL url = new URL(String.format("%s/service/instance", host));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            String refUuid = servBean.executeHttpMethod(url, connection, "GET", null);
            paraMap.put("instanceUUID", refUuid);

            // Initialize service parameters.
            prep = front_conn.prepareStatement("SELECT service_id"
                    + " FROM service WHERE filename = ?");
            prep.setString(1, serviceType);
            rs1 = prep.executeQuery();
            rs1.next();
            int serviceID = rs1.getInt(1);
            Timestamp timeStamp = new Timestamp(System.currentTimeMillis());

            // Install Instance into DB.
            prep = front_conn.prepareStatement("INSERT INTO Frontend.service_instance "
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
            switch(serviceType) {
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
            return ("\nInstance UUID: " + refUuid + "\n");

        } catch (EJBException | SQLException | IOException e) {
            return e.getMessage();
        }
    }
    
    @PUT
    @Path("/service/{siUUID}/{action}")
    public String push(@PathParam("siUUID") String svcInstanceUUID, @PathParam("action") String action) {
        if (action.equalsIgnoreCase("revert")) {
            
        } else if (action.equalsIgnoreCase("terminate")) {
            
        }
        
        return "Error! Invalid Action\n";
    }
}
