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
import net.maxgigapop.mrs.bean.ServiceInstance;
import net.maxgigapop.mrs.bean.persist.DeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.ServiceInstancePersistenceManager;
import net.maxgigapop.mrs.rest.api.model.ApiDeltaBase;

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

    public ServiceResource() {
    }

    @GET
    @Path("/instance")
    @Produces({"application/xml", "application/json"})
    public String create() {
        return serviceCallHandler.createInstance().getReferenceUUID();
    }

    @DELETE
    @Path("/{uuid}")
    public String terminate(@PathParam("uuid") String siUuid) {
        try {
            serviceCallHandler.terminateInstance(siUuid);
            return "Successfully terminated";
        } catch (EJBException e) {
            return (e.getMessage());
        }
    }

    //GET instance property
    @GET
    @Path("/property/{siUUID}/{property}")
    public String getProperty(@PathParam("siUUID") String svcInstanceUUID, @PathParam("property") String property) {
        String value = serviceCallHandler.getInstanceProperty(svcInstanceUUID, property);
        if (value == null) {
            throw new EJBException("Unknown property=" + property);
        }
        return value;
    }

    //PUT to set property value
    @PUT
    @Path("/property/{siUUID}/{property}/{value}")
    public void setProperty(@PathParam("siUUID") String svcInstanceUUID, @PathParam("property") String property, @PathParam("value") String value) {
        serviceCallHandler.setInstanceProperty(svcInstanceUUID, property, value);
    }

    //POST to set property value for bigger string
    @POST
    @Path("/property/{siUUID}/{property}")
    public void postProperty(@PathParam("siUUID") String svcInstanceUUID, @PathParam("property") String property, String value) {
        serviceCallHandler.setInstanceProperty(svcInstanceUUID, property, value);
    }

    //POST to run workflow to compile and add service delta (SPA)
    @POST
    @Consumes({"application/xml", "application/json"})
    @Produces("application/xml")
    @Path("/{siUUID}")
    public ApiDeltaBase compile(@PathParam("siUUID") String svcInstanceUUID, ServiceApiDelta svcApiDelta) throws Exception {
        String workerClassPath = svcApiDelta.getWorkerClassPath();
        SystemDelta sysDelta = serviceCallHandler.compileAddDelta(svcInstanceUUID, workerClassPath, svcApiDelta.getUuid(), svcApiDelta.getModelAddition(), svcApiDelta.getModelReduction());
        ApiDeltaBase apiSysDelta = new ApiDeltaBase();
        apiSysDelta.setId(sysDelta.getId().toString());
        java.util.Date now = new java.util.Date();
        apiSysDelta.setCreationTime(new java.sql.Date(now.getTime()).toString());
        apiSysDelta.setReferenceVersion(sysDelta.getReferenceVersionGroup().getRefUuid());
        if (sysDelta.getModelAddition() != null) {
            String modelAddition = sysDelta.getModelAddition().getTtlModel();
            if (modelAddition == null) {
                modelAddition = ModelUtil.marshalOntModel(sysDelta.getModelAddition().getOntModel());
            }
            apiSysDelta.setModelAddition(modelAddition);
        }
        if (sysDelta.getModelReduction() != null) {
            String modelReduction = sysDelta.getModelReduction().getTtlModel();
            if (modelReduction == null) {
                modelReduction = ModelUtil.marshalOntModel(sysDelta.getModelReduction().getOntModel());
            }
            apiSysDelta.setModelReduction(modelReduction);
        }
        return apiSysDelta;
    }

    @POST
    @Consumes({"application/xml", "application/json"})
    @Produces("application/json")
    @Path("/{siUUID}")
    public ApiDeltaBase compileJson(@PathParam("siUUID") String svcInstanceUUID, ServiceApiDelta svcApiDelta) throws Exception {
        String workerClassPath = svcApiDelta.getWorkerClassPath();
        SystemDelta sysDelta = serviceCallHandler.compileAddDelta(svcInstanceUUID, workerClassPath, svcApiDelta.getUuid(), svcApiDelta.getModelAddition(), svcApiDelta.getModelReduction());
        ApiDeltaBase apiSysDelta = new ApiDeltaBase();
        apiSysDelta.setId(sysDelta.getId().toString());
        java.util.Date now = new java.util.Date();
        apiSysDelta.setCreationTime(new java.sql.Date(now.getTime()).toString());
        apiSysDelta.setReferenceVersion(sysDelta.getReferenceVersionGroup().getRefUuid());
        if (sysDelta.getModelAddition() != null) {
            String modelAddition = sysDelta.getModelAddition().getTtlModel();
            if (modelAddition == null) {
                modelAddition = ModelUtil.marshalOntModelJson(sysDelta.getModelAddition().getOntModel());
            }
            apiSysDelta.setModelAddition(modelAddition);
        }
        if (sysDelta.getModelReduction() != null) {
            String modelReduction = sysDelta.getModelReduction().getTtlModel();
            if (modelReduction == null) {
                modelReduction = ModelUtil.marshalOntModelJson(sysDelta.getModelReduction().getOntModel());
            }
            apiSysDelta.setModelReduction(modelReduction);
        }
        return apiSysDelta;
    }

    //PUT to push and sync deltas
    @PUT
    @Path("/{siUUID}/{action}")
    public String push(@PathParam("siUUID") String svcInstanceUUID, @PathParam("action") String action) {
        if (action.equalsIgnoreCase("propagate")) {
            return serviceCallHandler.propagateDeltas(svcInstanceUUID);
       } else if (action.equalsIgnoreCase("commit")) {
            return serviceCallHandler.commitDeltas(svcInstanceUUID);
        } else if (action.equalsIgnoreCase("revert")) {
            return serviceCallHandler.revertDeltas(svcInstanceUUID);
        } else {
            throw new EJBException("Unrecognized action=" + action);
        }
    }

    //PUT to push and sync deltas
    @GET
    @Path("/{siUUID}/{action}")
    public String check(@PathParam("siUUID") String svcInstanceUUID, @PathParam("action") String action) {
        if (action.equalsIgnoreCase("status")) {
            return serviceCallHandler.checkStatus(svcInstanceUUID);
        } else {
            throw new EJBException("Unrecognized action=" + action);
        }
    }
}
