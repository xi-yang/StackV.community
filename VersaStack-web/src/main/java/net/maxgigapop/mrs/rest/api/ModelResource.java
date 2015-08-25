/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.rest.api;

import com.hp.hpl.jena.ontology.OntModel;
import java.io.StringWriter;
import java.util.UUID;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import net.maxgigapop.mrs.bean.ModelBase;
import net.maxgigapop.mrs.bean.VersionGroup;
import net.maxgigapop.mrs.bean.persist.VersionGroupPersistenceManager;
import net.maxgigapop.mrs.rest.api.model.ApiModelBase;
import net.maxgigapop.mrs.system.HandleSystemCall;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.rest.api.model.ApiModelViewRequest;

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
    @Produces("application/xml")
    @Path("/{refUUID}")
    public ApiModelBase pullXml(@PathParam("refUUID") String refUUID) throws Exception{
        VersionGroup vg = VersionGroupPersistenceManager.findByReferenceId(refUUID);
        ModelBase modelBase = systemCallHandler.retrieveVersionGroupModel(refUUID);
        ApiModelBase apiModelBase = new ApiModelBase();
        apiModelBase.setId(modelBase.getId());
        apiModelBase.setVersion(refUUID);
        apiModelBase.setCreationTime(ModelUtil.modelDateToString(modelBase.getCreationTime()));
        apiModelBase.setStatus(vg.getStatus());
        apiModelBase.setTtlModel(ModelUtil.marshalOntModel(modelBase.getOntModel()));
        return apiModelBase;
    }
    
    @GET
    @Produces("application/json")
    @Path("/{refUUID}")
    public ApiModelBase pullJson(@PathParam("refUUID") String refUUID) throws Exception{
        VersionGroup vg = VersionGroupPersistenceManager.findByReferenceId(refUUID);
        ModelBase modelBase = systemCallHandler.retrieveVersionGroupModel(refUUID);
        ApiModelBase apiModelBase = new ApiModelBase();
        apiModelBase.setId(modelBase.getId());
        apiModelBase.setVersion(refUUID);
        apiModelBase.setCreationTime(ModelUtil.modelDateToString(modelBase.getCreationTime()));
        apiModelBase.setStatus(vg.getStatus());        
        apiModelBase.setTtlModel(ModelUtil.marshalOntModelJson(modelBase.getOntModel()));
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
    public ApiModelBase update(@PathParam("refUUID") String refUUID) throws Exception{
        systemCallHandler.updateHeadVersionGroup(refUUID);
        return this.pullXml(refUUID);
    }

    @PUT
    @Produces("application/json")
    @Path("/{refUUID}")
    public ApiModelBase updateJson(@PathParam("refUUID") String refUUID) throws Exception{
        systemCallHandler.updateHeadVersionGroup(refUUID);
        return this.pullJson(refUUID);
    }

    
    @GET
    @Produces("application/xml")
    public ApiModelBase creatHeadVersionGroup() throws Exception{
        VersionGroup vg = systemCallHandler.createHeadVersionGroup(UUID.randomUUID().toString());
        return this.pullXml(vg.getRefUuid());
    }
    
    @GET
    @Produces("application/json")
    public ApiModelBase creatHeadVersionGroupJson() throws Exception{
        VersionGroup vg = systemCallHandler.createHeadVersionGroup(UUID.randomUUID().toString());
        return this.pullJson(vg.getRefUuid());
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

    
    @POST
    @Consumes({"application/xml","application/json"})
    @Produces("application/xml")
    @Path("/view/{refUUID}")
    public ApiModelBase queryView(@PathParam("refUUID")String refUUID, ApiModelViewRequest viewRequest) throws Exception{
        OntModel ontModel = systemCallHandler.queryModelView(refUUID, viewRequest.getFilters());
        if (ontModel == null) {
            throw new EJBException("systemCallHandler.queryModelView return null model."); 
        }
        ApiModelBase apiModelBase = new ApiModelBase();
        apiModelBase.setVersion(refUUID);
        java.util.Date now = new java.util.Date();
        apiModelBase.setCreationTime(ModelUtil.modelDateToString(new java.sql.Date(now.getTime())));
        apiModelBase.setTtlModel(ModelUtil.marshalOntModel(ontModel));
        return apiModelBase;
    }

    
    @POST
    @Consumes({"application/xml","application/json"})
    @Produces({"application/json"})
    @Path("/view/{refUUID}")
    public ApiModelBase queryViewJson(@PathParam("refUUID")String refUUID, ApiModelViewRequest viewRequest) throws Exception{
        OntModel ontModel = systemCallHandler.queryModelView(refUUID, viewRequest.getFilters());
        if (ontModel == null) {
            throw new EJBException("systemCallHandler.queryModelView return null model."); 
        }
        ApiModelBase apiModelBase = new ApiModelBase();
        apiModelBase.setVersion(refUUID);
        java.util.Date now = new java.util.Date();
        apiModelBase.setCreationTime(ModelUtil.modelDateToString(new java.sql.Date(now.getTime())));
        apiModelBase.setTtlModel(ModelUtil.marshalOntModelJson(ontModel));
        return apiModelBase;
    }
}
