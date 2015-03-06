/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.rest.api;

import java.util.concurrent.Future;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import net.maxgigapop.mrs.system.HandleSystemCall;

/**
 *
 * @author max
 */
@Path("delta")
public class DeltaResource {
    @Context
    private UriInfo context;

    @EJB
    HandleSystemCall systemCallHandler;
    
    public DeltaResource(){
    }
    
//    @POST
//    @Consumes({"application/xml","application/json"})
//    @Path("/{refUUID}/{deltaId}/{action}")
//    public String propagate(@PathParam("refUUID")String refUUID){
//        
//    }
    
    @PUT
    @Consumes({"application/xml","application/json"})
    @Path("/{refUUID}")
    public String commit(@PathParam("refUUID")String refUUID){
        try{
            systemCallHandler.commitDelta(refUUID);
        }catch(EJBException e){
            return e.getMessage();
        }
        return "commit successfully";
    }
    
    
}
