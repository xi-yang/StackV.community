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
import net.maxgigapop.mrs.rest.api.model.ApiModelBase;
import net.maxgigapop.mrs.rest.api.model.ApiVersionGroup;
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
        net.maxgigapop.mrs.bean.ModelBase modelBase;
        try{
            modelBase= systemCallHandler.retrieveVersionGroupModel(refUUID);
        }catch(Exception e){
            throw new NotFoundException("Not Found");
        }
        ApiModelBase apiModelBase = new ApiModelBase();
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
    public ApiVersionGroup update(@PathParam("refUUID") String refUUID){
        VersionGroup vg = systemCallHandler.updateHeadVersionGroup(refUUID);
        ApiVersionGroup apiVersionGroup = new ApiVersionGroup();
        apiVersionGroup.setId(vg.getId());
        apiVersionGroup.setRefUuid(vg.getRefUuid());
        apiVersionGroup.setStatus(vg.getStatus());
        apiVersionGroup.setVersionItems(vg.getVersionItems());
        return apiVersionGroup;
    }

    @GET
    @Produces({"application/xml", "application/json"})
    public ApiModelBase creatHeadVersionGroup(){
        VersionGroup vg = systemCallHandler.createHeadVersionGroup(UUID.randomUUID().toString());
        return this.pull(vg.getRefUuid());
//        ApiVersionGroup apiVersionGroup = new ApiVersionGroup();
//        apiVersionGroup.setId(vg.getId());
//        apiVersionGroup.setRefUuid(vg.getRefUuid());
//        apiVersionGroup.setStatus(vg.getStatus());
//        apiVersionGroup.setVersionItems(vg.getVersionItems());
//        return apiVersionGroup;
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
