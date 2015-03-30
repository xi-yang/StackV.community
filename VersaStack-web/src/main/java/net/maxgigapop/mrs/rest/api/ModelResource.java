/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.rest.api;

import java.util.UUID;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.DELETE;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.bean.VersionGroup;
import net.maxgigapop.mrs.bean.persist.VersionGroupPersistenceManager;
import net.maxgigapop.mrs.rest.api.model.ApiModelBase;
import net.maxgigapop.mrs.system.HandleSystemCall;

/**
 * REST Web Service
 *
 * @author max
 */
@Path("model")
@RequestScoped
public class ModelResource {

    @Context
    private UriInfo context;
    
    @EJB
    HandleSystemCall systemCallHandler;

    /**
     * Creates a new instance of ModelResource
     */
    public ModelResource() {
    }

    /**
     * Retrieves representation of an instance of net.maxgigapop.mrs.rest.api.ModelResource
     * @return an instance of java.lang.String
     */
    @GET
    @Produces({"application/xml", "application/json"})
    @Path("/{refUUID}")
    public ApiModelBase pull(@PathParam("refUUID") String refUUID){
        VersionGroup vg = VersionGroupPersistenceManager.findByReferenceId(refUUID);
//        if (vg == null) {
//           throw new EJBException(String.format("retrieveVersionModel cannot find a VG with refUuid=%s", refUUID));
//        }
        ModelBase modelBase = new ModelBase();
        try{
            modelBase= systemCallHandler.retrieveVersionGroupModel(refUUID);
        }catch(Exception e){
            throw new NotFoundException("Not Found");
        }        
        ApiModelBase apiModelBase = new ApiModelBase();
        apiModelBase.setId(modelBase.getId());
        apiModelBase.setVersion(refUUID);
        apiModelBase.setCreationTime(modelBase.getCreationTime());
        apiModelBase.setStatus(vg.getStatus());
        apiModelBase.setTtlModel(modelBase.getTtlModel());
        return apiModelBase;
    }

    /**
     * PUT method for updating or creating an instance of ModelResource
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Produces({"application/xml", "application/json"})
    @Path("/{refUUID}")
    public ApiModelBase update(@PathParam("refUUID") String refUUID){
        systemCallHandler.updateHeadVersionGroup(refUUID);
        return this.pull(refUUID);
    }

    @GET
    @Produces({"application/xml", "application/json"})
    public ApiModelBase creatHeadVersionGroup(){
        VersionGroup vg = systemCallHandler.createHeadVersionGroup(UUID.randomUUID().toString());
        return this.pull(vg.getRefUuid());
    }
    
    @GET
    @Path("/systeminstance")
    @Produces({"application/xml","application/json"})
    public String push(){
        return systemCallHandler.createInstance().getReferenceUUID();
    }
    
    @DELETE
    @Path("/systeminstance/{refUUID}")
    public String terminate(@PathParam("refUUID") String refUUID){
        try{
            systemCallHandler.terminateInstance(refUUID);
            return "Successfully terminated";
        }catch(EJBException e){
            return(e.getMessage());
        }
    }


}
