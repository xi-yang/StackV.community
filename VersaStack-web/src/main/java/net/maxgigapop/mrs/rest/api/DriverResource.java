/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.rest.api;

import java.util.Map;
import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.system.HandleSystemCall;

/**
 *
 * @author max
 */
public class DriverResource {
    @Context
    private UriInfo context;

    @EJB
    HandleSystemCall systemCallHandler;
    
    public DriverResource(){
    }
    
    @GET
    @Produces({"application/xml","application/json"})
    public Map<String, DriverInstance> pullAll(){
        return systemCallHandler.retrieveAllDriverInstanceMap();
    }
    
    

}
