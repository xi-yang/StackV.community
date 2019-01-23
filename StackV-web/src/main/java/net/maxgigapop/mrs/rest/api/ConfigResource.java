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
import net.maxgigapop.mrs.common.StackLogger;
import static net.maxgigapop.mrs.rest.api.WebResource.commonsClose;
import net.maxgigapop.mrs.rest.api.model.ApiDriverInstance;
import net.maxgigapop.mrs.system.HandleConfigCall;
import net.maxgigapop.mrs.system.HandleSystemCall;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

/**
 *
 * @author max
 */
@Path("config")
public class ConfigResource {

    private final StackLogger logger = new StackLogger(WebResource.class.getName(), "ConfigResource");
    private final JNDIFactory factory = new JNDIFactory();

    @Context
    private UriInfo context;

    @EJB
    HandleConfigCall configCallHandler;

    public ConfigResource() {
    }

    @GET
    @Path("/{property}")
    public String getProperty(@PathParam("property") String property) {
        String method = "getProperty";
        logger.targetid(property);
        logger.trace_start(method);
        try {
            return configCallHandler.getConfigProperty(property);
        } catch (Exception e) {
            throw logger.throwing(method, e);
        }
    }
    
    @GET
    @Produces({"application/json"})
    public String getAllProperties() {
        String method = "getAllProperties";
        logger.trace_start(method);
        try {
            return configCallHandler.getAllConfig();
        } catch (Exception e) {
            throw logger.throwing(method, e);
        }
    } 
    
    @PUT
    @Path("/{property}/{value}")
    public void setProperty(@PathParam("property") String property, @PathParam("value") String value) {
        String method = "setProperty";
        logger.targetid(property);
        logger.trace_start(method);
        try {
            configCallHandler.setConfigProperty(property, value);
            logger.trace_end(method);
        } catch (Exception e) {
            throw logger.throwing(method, e);
        }
    }
    
    @DELETE
    @Path("/{property}")
    public void deleteProperty(@PathParam("driverId") String property) {
        String method = "deleteProperty";
        logger.targetid(property);
        logger.trace_start(method);
        try {
            configCallHandler.deleteConfigProperty(property);
            logger.trace_end(method);
        } catch (Exception e) {
            throw logger.throwing(method, e);
        }
    }   
}
