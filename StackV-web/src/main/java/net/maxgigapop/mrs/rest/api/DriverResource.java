/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2014

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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.rest.api.model.ApiDriverInstance;
import net.maxgigapop.mrs.system.HandleSystemCall;

/**
 *
 * @author max
 */
@Path("app/driver")
public class DriverResource {

    @Context
    private UriInfo context;

    @EJB
    HandleSystemCall systemCallHandler;

    public DriverResource() {
    }

    @GET
    @Produces({"application/xml", "application/json"})
    public String pullAll() {
        Set<String> instanceSet = systemCallHandler.retrieveAllDriverInstanceMap().keySet();
        String allInstance = "";
        for (String instance : instanceSet) {
            allInstance += instance + "\n";
        }
        return allInstance;
    }

    @GET
    @Produces({"application/xml", "application/json"})
    @Path("/{topoUri}")
    public ApiDriverInstance pull(@PathParam("topoUri") String topoUri) {
        DriverInstance driverInstance = systemCallHandler.retrieveDriverInstance(topoUri);
        ApiDriverInstance adi = new ApiDriverInstance();
        adi.setProperties(driverInstance.getProperties());
        return adi;
    }

    @DELETE
    @Path("/{topoUri}")
    public String unplug(@PathParam("topoUri") String topoUri) {
        try {
            systemCallHandler.unplugDriverInstance(topoUri);
        } catch (EJBException e) {
            return e.getMessage();
        }
        return "unplug successfully";
    }

    @POST
    @Consumes({"application/xml", "application/json"})
    public String plug(ApiDriverInstance di) {
        try {
            systemCallHandler.plugDriverInstance(di.getProperties());
        } catch (EJBException e) {
            return e.getMessage();
        }
        return "plug successfully";
    }
}
