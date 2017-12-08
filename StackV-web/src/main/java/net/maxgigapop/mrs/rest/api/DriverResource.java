/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2014 Jared Welsh 2016

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 */

package net.maxgigapop.mrs.rest.api;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.rest.api.model.ApiDriverInstance;
import net.maxgigapop.mrs.system.HandleSystemCall;

/**
 *
 * @author max
 */
@Path("driver")
public class DriverResource {
    
    private final StackLogger logger = new StackLogger(DriverResource.class.getName(), "DriverResource");

    private final String front_db_user = "root";
    private final String front_db_pass = "root";

    @Context
    private UriInfo context;

    @EJB
    HandleSystemCall systemCallHandler;

    public DriverResource() {
    }

    @GET
    @Produces({"application/json"})
    public ArrayList<String> pullAll() throws SQLException {
        String method = "pullAll";
        logger.trace_start(method);
        Set<String> instanceSet = systemCallHandler.retrieveAllDriverInstanceMap().keySet();
        ArrayList<String> retList = new ArrayList<>();
        
        Properties prop = new Properties();
        prop.put("user", front_db_user);
        prop.put("password", front_db_pass);
        Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/rainsdb",
                prop);
        
        for (String instance : instanceSet) {
            PreparedStatement prep = front_conn.prepareStatement("SELECT * FROM driver_instance WHERE topologyUri = ?");
            prep.setString(1, instance);
            ResultSet ret = prep.executeQuery();
            while (ret.next()) {
                retList.add(ret.getString("id"));
                retList.add(ret.getString("driverEjbPath"));
                retList.add(ret.getString("topologyUri"));
            }
        }
        logger.trace_end(method);
        return retList;
    }
    
    @GET
    @Produces({"application/json"})
    @Path("/{driverId}")
    public ArrayList<String> pull(@PathParam("driverId") String driverId) throws SQLException {
        ArrayList<String> retList = new ArrayList<>();
        String method = "pull";
        logger.targetid(driverId);
        logger.trace_start(method);
        
        Properties prop = new Properties();
        prop.put("user", front_db_user);
        prop.put("password", front_db_pass);
        Connection front_conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/rainsdb",
                prop);
        
        PreparedStatement prep = front_conn.prepareStatement("SELECT * FROM driver_instance_property WHERE driverInstanceId = ?");
        prep.setString(1, driverId);
        ResultSet ret = prep.executeQuery();
        
        while (ret.next()) {
            retList.add(ret.getString("property"));
            retList.add(ret.getString("value"));
        }
        logger.trace_end(method);
        return retList;
    }

    @DELETE
    @Path("/{topoUri}")
    public Response unplug(@PathParam("topoUri") String topoUri) {
        String method = "unplug";
        logger.targetid(topoUri);
        logger.trace_start(method);
        try {
            List<String> retList = systemCallHandler.lookupDriverBoundServiceInstances(topoUri);
            if (retList != null && !retList.isEmpty()) {
                return Response.status(Status.CONFLICT).entity(retList.toString()).build();
            }
            systemCallHandler.unplugDriverInstance(topoUri);
        } catch (Exception e) {
            throw logger.throwing(method, e);
        }
        logger.trace_end(method);
        return Response.ok().entity("unplug successfully").build();
    }   

    @POST
    @Consumes({"application/xml", "application/json"})
    public String plug(ApiDriverInstance di) {
        String method = "plug";
        try {
            logger.targetid(di.getTopologyUri());
        } catch (Exception ex) {
            throw logger.throwing(method, ex);
        }
        logger.trace_start(method);
        try {
            systemCallHandler.plugDriverInstance(di.getProperties());
        } catch (Exception e) {
            throw logger.throwing(method, e);
        }
        logger.trace_end(method);
        return "plug successfully";
    }
}
