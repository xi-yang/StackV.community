/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import net.maxgigapop.mrs.service.HandleServiceCall;

/**
 *
 * @author max
 */
@Path("service")
public class ServiceResource {
    @Context
    private UriInfo context;

    @EJB
    HandleServiceCall serviceCallHandler;
    
    public ServiceResource(){
    }
    
    @GET
    @Path("/serviceinstance")
    @Produces({"application/xml","application/json"})
    public String create(){
        //return serviceCallHandler.createInstance().getReferenceUUID();
        throw new UnsupportedOperationException();
    }
    
    @DELETE
    @Path("/serviceinstance/{uuid}")    
    public String terminate(@PathParam("uuid")String siUuid){
        throw new UnsupportedOperationException();
    } 
    
    //POST to push workflow (SPA)
    
    //GET to check status
}
