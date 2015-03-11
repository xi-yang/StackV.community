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
import net.maxgigapop.mrs.bean.VersionGroup;
import net.maxgigapop.mrs.rest.api.model.APIModelBase;
import net.maxgigapop.mrs.rest.api.model.APIVersionGroup;
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
    public APIModelBase pull(@PathParam("refUUID") String refUUID){
        net.maxgigapop.mrs.bean.ModelBase modelBase;
        try{
            modelBase= systemCallHandler.retrieveVersionGroupModel(refUUID);
        }catch(Exception e){
            throw new NotFoundException("Not Found");
        }
        APIModelBase apiModelBase = new APIModelBase();
        apiModelBase.setId(modelBase.getId());
        apiModelBase.setVersion(modelBase.getCxtVersionTag());
        apiModelBase.setCreationTime(modelBase.getCreationTime());
//        apiModelBase.setStatus();
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
    public APIVersionGroup update(@PathParam("refUUID") String refUUID){
        VersionGroup vg = systemCallHandler.updateHeadVersionGroup(refUUID);
        APIVersionGroup apiVersionGroup = new APIVersionGroup();
        apiVersionGroup.setId(vg.getId());
        apiVersionGroup.setRefUuid(vg.getRefUuid());
        apiVersionGroup.setStatus(vg.getStatus());
        apiVersionGroup.setVersionItems(vg.getVersionItems());
        return apiVersionGroup;
    }

    @POST
    @Produces({"application/xml", "application/json"})
    public APIVersionGroup creatHeadVersionGroup(){
        VersionGroup vg = systemCallHandler.createHeadVersionGroup(UUID.randomUUID().toString());
        APIVersionGroup apiVersionGroup = new APIVersionGroup();
        apiVersionGroup.setId(vg.getId());
        apiVersionGroup.setRefUuid(vg.getRefUuid());
        apiVersionGroup.setStatus(vg.getStatus());
        apiVersionGroup.setVersionItems(vg.getVersionItems());
        return apiVersionGroup;
    }
    
    @POST
    @Path("/systeminstance")
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
