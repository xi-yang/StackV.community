/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.rest.api;

import java.util.UUID;
import javax.ejb.EJB;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import net.maxgigapop.mrs.bean.VersionGroup;
import net.maxgigapop.mrs.rest.model.ModelBase;
import net.maxgigapop.mrs.system.HandleSystemCall;

/**
 * REST Web Service
 *
 * @author max
 */

@Path("VersionGroup")
public class VersionGroupResource {

    @Context
    private UriInfo context;
    
    @EJB
    HandleSystemCall systemCallHandler;
    
    public VersionGroupResource(){
    }
    
    @POST
//    @Consumes({"application/xml","application/json"})
    public String createHeadVersionGroup(){
        return systemCallHandler.createHeadVersionGroup(UUID.randomUUID().toString()).getRefUuid();
    } 
    
    @PUT
    @Path("/{refUUID}")
    public void update(@PathParam("refUUID") String refUUID){
        systemCallHandler.updateHeadVersionGroup(refUUID);
    }
    
    @GET
    @Produces({"application/xml", "application/json"})
    @Path("/{refUUID}")
    public ModelBase pull(@PathParam("refUUID") String refUUID){
        net.maxgigapop.mrs.bean.ModelBase mbDB;
        try{
            mbDB= systemCallHandler.retrieveVersionGroupModel(refUUID);
        }catch(Exception e){
            throw new NotFoundException("Not Found");
        }
        ModelBase mbRest = new ModelBase();
        mbRest.setId(mbDB.getId());
        mbRest.setVersion(mbDB.getCxtVersionTag());
        mbRest.setCreationTime(mbDB.getCreationTime());
//        mbRest.setStatus();
        mbRest.setTtlModel(mbDB.getTtlModel());
        return mbRest;
    }
   
}
