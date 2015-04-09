/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.rest.api;

import java.util.HashMap;
import java.util.Map;
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
@Path("driver")
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
    
    @GET
    @Produces({"application/xml","application/json"})
    @Path("/{topoUri}")
    public DriverInstance pull(@PathParam("topoUri")String topoUri){
        return systemCallHandler.retrieveDriverInstance(topoUri);
    }
    
    @DELETE
    @Consumes({"application/xml","application/json"})
    public String unplug(ApiDriverInstance di){
        try{
            systemCallHandler.unplugDriverInstance(di.getTopologyUri());
        }catch(EJBException e){
            return e.getMessage();
        }
        return "unplug successfully";
    } 
    
    @POST
    @Consumes({"application/xml","application/json"})
    public String plug(ApiDriverInstance di){
        Map<String,String> driverProperties = new HashMap();
        driverProperties.put("topoUri", di.getTopologyUri());
        driverProperties.put("driverEjbPath", di.getDriverEjbPath());
        try{
            systemCallHandler.plugDriverInstance(driverProperties);
        }catch(EJBException e){
            return e.getMessage();
        }
        return "plug successfully";
    }    
}
