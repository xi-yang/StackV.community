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
package net.stackv.rest;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import org.jboss.resteasy.spi.HttpRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.maxgigapop.mrs.common.StackLogger;
import net.stackv.rest.model.ApiDriverInstance;
import net.maxgigapop.mrs.system.HandleSystemCall;

/**
 *
 * @author max
 */
@Path("driver")
public class DriverResource {
    private static String host = "http://127.0.0.1:8080/StackV-web/restapi";
    private static final StackLogger logger = new StackLogger(WebResource.class.getName(), "DriverResource");
    private static final JNDIFactory factory = new JNDIFactory();
    private static final OkHttpClient client = new OkHttpClient();

    @Context
    private UriInfo context;
    @Context
    private HttpRequest httpRequest;

    @EJB
    HandleSystemCall systemCallHandler;

    public DriverResource() {
    }

    @GET
    @Produces({ "application/json" })
    public Response pullAll() {
        String method = "pullAll";
        try (Connection front_conn = factory.getConnection("rainsdb");
                PreparedStatement prep = front_conn.prepareStatement("SELECT * FROM driver_instance");
                ResultSet ret = prep.executeQuery();) {
            JSONArray jsonArrayRet = new JSONArray();
            while (ret.next()) {
                JSONObject jsonRet = new JSONObject();
                jsonArrayRet.add(jsonRet);
                jsonRet.put("topologyUri", ret.getString("topologyUri"));
                jsonRet.put("driverEjbPath", ret.getString("driverEjbPath"));
                String driverId = ret.getString("id");
                jsonRet.put("id", driverId);
                // add additional properties
                PreparedStatement prep2 = front_conn.prepareStatement(
                        "SELECT * FROM driver_instance_property WHERE driverInstanceId = ? AND property = ?");
                prep2.setString(1, driverId);
                prep2.setString(2, "disabled");
                ResultSet ret2 = prep2.executeQuery();
                if (ret2.next()) {
                    jsonRet.put("disabled", ret2.getString("value"));
                } else {
                    jsonRet.put("disabled", "false");
                }
                ret2.close();
                prep2.close();

                PreparedStatement prep3 = front_conn.prepareStatement(
                        "SELECT * FROM driver_instance_property WHERE driverInstanceId = ? AND property = ?");
                prep3.setString(1, driverId);
                prep3.setString(2, "contErrors");
                ResultSet ret3 = prep3.executeQuery();
                if (ret3.next()) {
                    jsonRet.put("contErrors", ret3.getString("value"));
                } else {
                    jsonRet.put("contErrors", "0");
                }
                ret3.close();
                prep3.close();
            }
            return Response.status(200).entity(jsonArrayRet).build();
        } catch (SQLException ex) {
            logger.catching(method, ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("SQLException: " + ex).build();
        }
    }

    @GET
    @Produces({ "application/json" })
    @Path("/{driverId}")
    public Response pull(@PathParam("driverId") String driverId) throws SQLException {
        JSONObject jsonRet = new JSONObject();
        logger.targetid(driverId);

        Connection front_conn = factory.getConnection("rainsdb");
        PreparedStatement prep;
        if (driverId.startsWith("urn")) {
            prep = front_conn.prepareStatement("SELECT id FROM driver_instance WHERE topologyUri = ?");
            prep.setString(1, driverId);
            ResultSet ret = prep.executeQuery();
            if (ret.next()) {
                driverId = ret.getString("id");
            } else {
                return Response.status(Response.Status.NOT_FOUND).entity("no driver instance for " + driverId).build();
            }
        }
        prep = front_conn.prepareStatement("SELECT * FROM driver_instance_property WHERE driverInstanceId = ?");
        prep.setString(1, driverId);
        ResultSet ret = prep.executeQuery();

        while (ret.next()) {
            jsonRet.put(ret.getString("property"), ret.getString("value"));
        }
        return Response.status(200).entity(jsonRet).build();
    }

    @GET
    @Produces({ "application/json" })
    @Path("/{driverId}/{property}")
    public String pullProperty(@PathParam("driverId") String driverId, @PathParam("property") String property)
            throws SQLException {
        // String method = "pullProperty";
        logger.targetid(driverId);

        Connection front_conn = factory.getConnection("rainsdb");
        PreparedStatement prep;
        if (driverId.startsWith("urn")) {
            prep = front_conn.prepareStatement("SELECT id FROM driver_instance WHERE topologyUri = ?");
            prep.setString(1, driverId);
            ResultSet ret = prep.executeQuery();
            if (ret.next()) {
                driverId = ret.getString("id");
            } else {
                return null;
            }
        }
        prep = front_conn.prepareStatement(
                "SELECT value FROM driver_instance_property WHERE driverInstanceId = ? AND property = ?");
        prep.setString(1, driverId);
        prep.setString(2, property);
        ResultSet ret = prep.executeQuery();

        if (ret.next()) {
            return ret.getString("value");
        }
        return null;
    }

    @PUT
    @Produces({ "application/json" })
    @Path("/{driverId}/{property}/{value}")
    public void setProperty(@PathParam("driverId") String driverId, @PathParam("property") String property,
            @PathParam("value") String value) {
        String method = "putProperty";
        logger.targetid(driverId);

        try {
            systemCallHandler.setDriverInstanceProperty(driverId, property, value);
        } catch (Exception e) {
            throw logger.throwing(method, e);
        }
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

            // DEREGISTER
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL(String.format("%s/md2/register/drivers/%s/", host, topoUri));
            Request request = new Request.Builder().url(url).delete().header("Authorization", auth).build();
            client.newCall(request).execute();
        } catch (Exception e) {
            throw logger.throwing(method, e);
        }
        logger.trace_end(method);
        return Response.ok().entity("unplug successfully").build();
    }

    @POST
    @Consumes({ "application/xml", "application/json" })
    public String plug(ApiDriverInstance di) {
        String method = "plug";
        try {
            logger.targetid(di.getTopologyUri());
            logger.trace_start(method);
            systemCallHandler.plugDriverInstance(di.getProperties());

            // REGISTER
            final String auth = httpRequest.getHttpHeaders().getHeaderString("Authorization");
            URL url = new URL(String.format("%s/md2/register/drivers/%s/", host, di.getTopologyUri()));
            Request request = new Request.Builder().url(url).post(null).header("Authorization", auth).build();
            client.newCall(request).execute();
        } catch (Exception e) {
            throw logger.throwing(method, e);
        }
        logger.trace_end(method);
        return "plug successfully";
    }

    @POST
    @Produces({ "application/json" })
    @Path("/{driverId}")
    public void update(@PathParam("driverId") String driverId, ApiDriverInstance di) {
        String method = "putProperty";
        logger.targetid(driverId);

        try {
            systemCallHandler.updateDriverInstance(driverId, di.getProperties());
        } catch (Exception e) {
            throw logger.throwing(method, e);
        }
    }
}
