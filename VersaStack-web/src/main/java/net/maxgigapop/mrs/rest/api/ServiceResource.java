/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.rest.api;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
import net.maxgigapop.mrs.bean.SystemDelta;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.rest.api.model.ServiceApiDelta;
import net.maxgigapop.mrs.service.HandleServiceCall;
import java.util.logging.Logger;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.PUT;
/**
 *
 * @author max
 */
@Path("service")
public class ServiceResource {
    private static final Logger log = Logger.getLogger(ServiceResource.class.getName());

    @Context
    private UriInfo context;

    @EJB
    HandleServiceCall serviceCallHandler;
    
    public ServiceResource(){
    }
    
    @GET
    @Path("/instance")
    @Produces({"application/xml","application/json"})
    public String create(){
        return serviceCallHandler.createInstance().getReferenceUUID();
    }
    
    @DELETE
    @Path("/instance/{uuid}")    
    public String terminate(@PathParam("uuid")String siUuid){
        try{
            serviceCallHandler.terminateInstance(siUuid);
            return "Successfully terminated";
        }catch(EJBException e){
            return(e.getMessage());
        }
    } 
    
    //POST to run workflow to compile and add service delta (SPA)
    @POST
    @Consumes({"application/xml","application/json"})
    @Path("/{siUUID}")
    public String compile(@PathParam("siUUID") String svcInstanceUUID, ServiceApiDelta apiDelta) {
        String status = "success";
        String workerClassPath = apiDelta.getWorkerClassPath();
        SystemDelta sysDelta = serviceCallHandler.compileAddDelta(svcInstanceUUID, workerClassPath, apiDelta.getUuid(), apiDelta.getModelAddition(), apiDelta.getModelReduction());
        return status;
    }

    //PUT to push and sync deltas
    @PUT
    @Path("/{siUUID}")
    public String push(@PathParam("siUUID")String svcInstanceUUID) {
        return serviceCallHandler.pushSyncDeltas(svcInstanceUUID);
    }
}
