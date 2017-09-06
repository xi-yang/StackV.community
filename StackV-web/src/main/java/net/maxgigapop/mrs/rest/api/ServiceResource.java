/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2015

 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and/or hardware specification (the “Work”) to deal in the 
 * Work without restriction, including without limitation the rights to use, 
 * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
 * the Work, and to permit persons to whom the Work is furnished to do so, 
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Work.

 * THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS  
 * IN THE WORK.
 */

package net.maxgigapop.mrs.rest.api;

import java.util.Map;
import javax.ejb.EJB;
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
import javax.ws.rs.PUT;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.rest.api.model.ApiDeltaBase;
import net.maxgigapop.mrs.rest.api.model.ApiDeltaRetrieval;
import net.maxgigapop.mrs.rest.api.model.ApiDeltaVerification;
import net.maxgigapop.mrs.rest.api.model.ServiceApiManifest;
import net.maxgigapop.mrs.service.ServiceManifest;
import org.json.simple.JSONObject;

/**
 *
 * @author max
 */
@Path("service")
public class ServiceResource {

    private final StackLogger logger = new StackLogger(ModelResource.class.getName(), "ModelResource");

    @Context
    private UriInfo context;

    @EJB
    HandleServiceCall serviceCallHandler;

    public ServiceResource() {
    }

    @GET
    @Path("/ready")
    @Produces({"application/xml", "application/json"})
    public String ready() {
        return (serviceCallHandler.hasSystemBootStrapped() ? "true" : "false");
    }
    
    @PUT
    @Path("/ready/reset")
    @Produces({"application/xml", "application/json"})
    public void reset() {
        serviceCallHandler.resetSystemBootStrapped();
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
        serviceCallHandler.terminateInstance(siUuid);
        return "Successfully terminated";
    }

    //GET instance property
    @GET
    @Path("/property/{siUUID}/{property}")
    public String getProperty(@PathParam("siUUID") String svcInstanceUUID, @PathParam("property") String property) {
        String method = "getProperty";
        logger.refuuid(svcInstanceUUID);
        String value = serviceCallHandler.getInstanceProperty(svcInstanceUUID, property);
        if (value == null) {
            throw logger.error_throwing(method, "Unknown property=" + property);
        }
        return value;
    }
    
    //GET instance property
    @GET
    @Path("/property/{siUUID}")
    @Consumes("application/json")
    public String listProperties(@PathParam("siUUID") String svcInstanceUUID) {
        Map properties = serviceCallHandler.listInstanceProperties(svcInstanceUUID);
        JSONObject json = new JSONObject();
        json.putAll(properties);
        return json.toString();
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
        String method = "compile";
        logger.refuuid(svcInstanceUUID);
        logger.targetid(svcApiDelta.getUuid());
        logger.trace_start(method);
        String workerClassPath = svcApiDelta.getWorkerClassPath();
        SystemDelta sysDelta;
        try {
            sysDelta = serviceCallHandler.compileAddDelta(svcInstanceUUID, workerClassPath, svcApiDelta.getUuid(), svcApiDelta.getModelAddition(), svcApiDelta.getModelReduction());
        } catch (Exception ex) {
            serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
            throw ex;
        }
        if (sysDelta == null) {
            serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
            throw logger.error_throwing(method, "failed to compile target:ServiceDelta");
        }
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
        logger.trace_end(method);
        return apiSysDelta;
    }
    
    @POST
    @Consumes({"application/xml", "application/json"})
    @Produces("application/json")
    @Path("/{siUUID}")
    public ApiDeltaBase compileJson(@PathParam("siUUID") String svcInstanceUUID, ServiceApiDelta svcApiDelta) throws Exception {
        String method = "compileJson";
        logger.refuuid(svcInstanceUUID);
        logger.targetid(svcApiDelta.getUuid());
        logger.trace_start(method);
        String workerClassPath = svcApiDelta.getWorkerClassPath();
        SystemDelta sysDelta;
        try {
            sysDelta = serviceCallHandler.compileAddDelta(svcInstanceUUID, workerClassPath, svcApiDelta.getUuid(), svcApiDelta.getModelAddition(), svcApiDelta.getModelReduction());
        } catch (Exception ex) {
            serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
            throw ex;
        }
        if (sysDelta == null) {
            serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
            throw logger.error_throwing(method, "failed to compile target:ServiceDelta");
        }
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
        logger.trace_end(method);
        return apiSysDelta;
    }

    @PUT
    @Consumes({"application/xml", "application/json"})
    @Produces("application/xml")
    @Path("/{siUUID}")
    public ApiDeltaBase recompile(@PathParam("siUUID") String svcInstanceUUID) throws Exception {
        String method = "recompile";
        logger.refuuid(svcInstanceUUID);
        logger.trace_start(method);
        String workerClassPath = "net.maxgigapop.mrs.service.orchestrate.NegotiableWorker";
        SystemDelta sysDelta;
        try {
            sysDelta = serviceCallHandler.recompileDeltas(svcInstanceUUID, workerClassPath);
        } catch (Exception ex) {
            serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
            throw ex;
        }
        if (sysDelta == null) {
            serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
            throw logger.error_throwing(method, "failed to compile target:ServiceDelta");
        }
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
        logger.trace_end(method);
        return apiSysDelta;
    }

    
    @PUT
    @Consumes({"application/xml", "application/json"})
    @Produces("application/json")
    @Path("/{siUUID}")
    public ApiDeltaBase recompileJson(@PathParam("siUUID") String svcInstanceUUID, ServiceApiDelta svcApiDelta) throws Exception {
        String method = "recompileJson";
        logger.refuuid(svcInstanceUUID);
        logger.trace_start(method);
        String workerClassPath = "net.maxgigapop.mrs.service.orchestrate.NegotiableWorker";
        SystemDelta sysDelta;
        try {
            sysDelta = serviceCallHandler.recompileDeltas(svcInstanceUUID, workerClassPath);
        } catch (Exception ex) {
            serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
            throw ex;
        }
        if (sysDelta == null) {
            serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
            throw logger.error_throwing(method, "failed to compile target:ServiceDelta");
        }
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
        logger.trace_end(method);
        return apiSysDelta;
    }
    
    //PUT to push and sync deltas
    //Propagate:
    // propagate_through (default): use cached VG + no refresh
    // propagate_forward: use persisted/updated VG + no refresh
    // propagate_forced: refresh VG and apply delta to refreshed only
    //      forced only work after FAILED status
    //Commit:
    // sync - failed at service level
    // async - failed at system level -> status update later
    //      forced: only work after FAILED status
    //Revert: 
    // revert forced: after FAILED status
    //Refresh: refresh VG to latest from drivers
    @PUT
    @Path("/{siUUID}/{action}")
    public String push(@PathParam("siUUID") String svcInstanceUUID, @PathParam("action") String action) {
        String method = "push";
        logger.refuuid(svcInstanceUUID);
        logger.trace_start(method);
        if (action.equalsIgnoreCase("propagate")
                || action.equalsIgnoreCase("propagate_through")) {
            try {
                String ret = serviceCallHandler.propagateDeltas(svcInstanceUUID, true, false);
                logger.trace_end(method);
                return ret;
            } catch (Exception ex) {
                serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
                throw logger.throwing(method, ex);
            }
        } else if (action.equalsIgnoreCase("propagate_forward")) {
            try {
                String ret = serviceCallHandler.propagateDeltas(svcInstanceUUID, false, false);
                logger.trace_end(method);
                return ret;
            } catch (Exception ex) {
                serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
                throw logger.throwing(method, ex);
            }
        } else if (action.equalsIgnoreCase("propagate_forced")) {
            try {
                String ret = serviceCallHandler.propagateDeltas(svcInstanceUUID, false, true);
                logger.trace_end(method);
                return ret;
            } catch (Exception ex) {
                serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
                throw logger.throwing(method, ex);
            }
        } else if (action.equalsIgnoreCase("propagate_retry")) {
            try {
                String ret = serviceCallHandler.propagateRetry(svcInstanceUUID, true, false);
                logger.trace_end(method);
                return ret;
            } catch (Exception ex) {
                serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
                throw logger.throwing(method, ex);
            }
        } else if (action.equalsIgnoreCase("propagate_forwardretry")) {
            try {
                String ret = serviceCallHandler.propagateRetry(svcInstanceUUID, false, false);
                logger.trace_end(method);
                return ret;
            } catch (Exception ex) {
                serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
                throw logger.throwing(method, ex);
            }
        } else if (action.equalsIgnoreCase("propagate_forcedretry")) {
            try {
                String ret = serviceCallHandler.propagateRetry(svcInstanceUUID, false, true);
                logger.trace_end(method);
                return ret;
            } catch (Exception ex) {
                serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
                throw logger.throwing(method, ex);
            }
        } else if (action.equalsIgnoreCase("commit")) {
            try {
                String ret = serviceCallHandler.commitDeltas(svcInstanceUUID, false);
                logger.trace_end(method);
                return ret;
            } catch (Exception ex) {
                serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
                throw logger.throwing(method, ex);
            }
        } else if (action.equalsIgnoreCase("commit_forced")) {
            try {
                String ret = serviceCallHandler.commitDeltas(svcInstanceUUID, true);
                logger.trace_end(method);
                return ret;
            } catch (Exception ex) {
                serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
                throw logger.throwing(method, ex);
            }
        } else if (action.equalsIgnoreCase("revert")) {
            try {
                String ret = serviceCallHandler.revertDeltas(svcInstanceUUID, false);
                logger.trace_end(method);
                return ret;
            } catch (Exception ex) {
                serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
                throw logger.throwing(method, ex);
            }
        } else if (action.equalsIgnoreCase("revert_forced")) {
            try {
                String ret = serviceCallHandler.revertDeltas(svcInstanceUUID, true);
                logger.trace_end(method);
                return ret;
            } catch (Exception ex) {
                serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
                throw logger.throwing(method, ex);
            }
        } else if (action.equalsIgnoreCase("refresh")) {
            serviceCallHandler.refreshVersionGroup(svcInstanceUUID);
            logger.trace_end(method, "REFRESHED");
            return "REFRESHED";
        } else {
            throw logger.error_throwing(method, "Unrecognized action=" + action);
        }
    }

    //PUT to push and sync deltas
    @GET
    @Path("/{siUUID}/{action}")
    public String check(@PathParam("siUUID") String svcInstanceUUID, @PathParam("action") String action) {
        String method = "check";
        logger.refuuid(svcInstanceUUID);
        logger.trace_start(method);
        if (action.equalsIgnoreCase("status")) {
            String ret = serviceCallHandler.checkStatus(svcInstanceUUID);
                logger.trace_end(method);
                return ret;
        } else {
            throw logger.error_throwing(method, "Unrecognized action=" + action);
        }
    }

    @GET
    @Produces("application/json")
    @Path("/verify/{siUUID}")
    public ApiDeltaVerification verifyJson(@PathParam("siUUID") String svcUUID) throws Exception {
        String method = "verifyJson";
        logger.refuuid(svcUUID);
        logger.trace_start(method);        
        ApiDeltaVerification apiDeltaVerification = new ApiDeltaVerification();
        java.util.Date now = new java.util.Date();
        apiDeltaVerification.setCreationTime(new java.sql.Date(now.getTime()).toString());
        apiDeltaVerification.setReferenceUUID(svcUUID);
        ModelUtil.DeltaVerification deltaVerification = new ModelUtil.DeltaVerification();
        serviceCallHandler.verifyDelta(svcUUID, deltaVerification, true);
        if (deltaVerification.getModelAdditionVerified() != null) {
            apiDeltaVerification.setVerifiedModelAddition(deltaVerification.getModelAdditionVerified());
        }
        if (deltaVerification.getModelAdditionUnverified() != null) {
            apiDeltaVerification.setUnverifiedModelAddition(deltaVerification.getModelAdditionUnverified());
        }
        if (deltaVerification.getModelReductionVerified() != null) {
            apiDeltaVerification.setVerifiedModelReduction(deltaVerification.getModelReductionVerified());
        }
        if (deltaVerification.getModelReductionUnverified() != null) {
            apiDeltaVerification.setUnverifiedModelReduction(deltaVerification.getModelReductionUnverified());
        }
        if (deltaVerification.getAdditionVerified() != null) {
           apiDeltaVerification.setAdditionVerified(deltaVerification.getAdditionVerified() ? "true" : "false");
        }
        if (deltaVerification.getReductionVerified() != null) {
            apiDeltaVerification.setReductionVerified(deltaVerification.getReductionVerified() ? "true" : "false");
        }
        logger.trace_end(method);        
        return apiDeltaVerification;
    }
    
    @GET
    @Produces("application/xml")
    @Path("/verify/{siUUID}")
    public ApiDeltaVerification verify(@PathParam("siUUID") String svcUUID) throws Exception {
        String method = "verify";
        logger.refuuid(svcUUID);
        logger.trace_start(method);        
        ApiDeltaVerification apiDeltaVerification = new ApiDeltaVerification();
        java.util.Date now = new java.util.Date();
        apiDeltaVerification.setCreationTime(now.toString());
        apiDeltaVerification.setReferenceUUID(svcUUID);
        ModelUtil.DeltaVerification deltaVerification = new ModelUtil.DeltaVerification();
        serviceCallHandler.verifyDelta(svcUUID, deltaVerification, false);
        if (deltaVerification.getModelAdditionVerified() != null) {
            apiDeltaVerification.setVerifiedModelAddition(deltaVerification.getModelAdditionVerified());
        }
        if (deltaVerification.getModelAdditionUnverified() != null) {
            apiDeltaVerification.setUnverifiedModelAddition(deltaVerification.getModelAdditionUnverified());
        }
        if (deltaVerification.getModelReductionVerified() != null) {
            apiDeltaVerification.setVerifiedModelReduction(deltaVerification.getModelReductionVerified());
        }
        if (deltaVerification.getModelReductionUnverified() != null) {
            apiDeltaVerification.setUnverifiedModelReduction(deltaVerification.getModelReductionUnverified());
        }
        if (deltaVerification.getAdditionVerified() != null) {
           apiDeltaVerification.setAdditionVerified(deltaVerification.getAdditionVerified() ? "true" : "false");
        }
        if (deltaVerification.getReductionVerified() != null) {
            apiDeltaVerification.setReductionVerified(deltaVerification.getReductionVerified() ? "true" : "false");
        }
        logger.trace_end(method);
        return apiDeltaVerification;
    }
    
    @GET
    @Produces("application/json")
    @Path("/delta/{svcUUID}")
    public ApiDeltaRetrieval retrieveDeltaJson(@PathParam("svcUUID") String svcUUID) {
        String method = "retrieveDeltaJson";
        logger.refuuid(svcUUID);
        logger.trace_start(method);        
        ApiDeltaRetrieval apiDeltaRetrieval = new ApiDeltaRetrieval();
        ModelUtil.DeltaRetrieval deltaRetrieval = new ModelUtil.DeltaRetrieval();
        serviceCallHandler.retrieveDelta(svcUUID, deltaRetrieval, true);
        apiDeltaRetrieval.setReferenceUUID(deltaRetrieval.getReferenceModelUUID());
        apiDeltaRetrieval.setServiceModelAddition(deltaRetrieval.getModelAdditionSvc());
        apiDeltaRetrieval.setServiceModelReduction(deltaRetrieval.getModelReductionSvc());
        apiDeltaRetrieval.setSystemModelAddition(deltaRetrieval.getModelAdditionSys());
        apiDeltaRetrieval.setSystemModelReduction(deltaRetrieval.getModelReductionSys());
        logger.end(method);
        return apiDeltaRetrieval;
    }
    
    @POST
    @Path("/manifest")
    @Consumes({"application/json","application/xml"})
    @Produces("application/json")
    public ServiceApiManifest resolveManifest(ServiceApiManifest manifest) {
        String method = "resolveManifest";
        logger.refuuid(manifest.getServiceUUID());
        logger.trace_start(method);        
        // if manifest.getJsonModel() == null, get serviceDelta.modelAddition into manifest.jsonTemplate
        String jsonModel = manifest.getJsonModel();
        if (jsonModel == null) {
            manifest = resolveServiceManifest(manifest.getServiceUUID(), manifest);
            logger.trace_end(method);        
            return manifest;
        }
        JSONObject joManifest = ServiceManifest.resolveManifestJsonTemplate(manifest.getJsonTemplate(), jsonModel);
        manifest.setJsonTemplate(joManifest.toJSONString());
        manifest.setJsonModel(null);
        logger.trace_end(method);        
        return manifest;
    }
    
    @POST
    @Path("/manifest/{svcUUID}")
    @Consumes({"application/json","application/xml"})
    @Produces("application/json")
    public ServiceApiManifest resolveServiceManifest(@PathParam("svcUUID") String svcUUID, ServiceApiManifest manifest) {
        String method = "resolveServiceManifest";
        logger.refuuid(svcUUID);
        logger.trace_start(method);        
        // if manifest.getJsonModel() == null, get serviceDelta.modelAddition into manifest.jsonTemplate
        manifest.setServiceUUID(svcUUID);
        ModelUtil.DeltaVerification deltaVerification = new ModelUtil.DeltaVerification();
        serviceCallHandler.verifyDelta(manifest.getServiceUUID(), deltaVerification, true);
        String jsonModel = deltaVerification.getModelAdditionVerified();
        if (jsonModel == null) {
            throw logger.error_throwing(method, "cannot get verified modelAddition for ref:ServiceInstance");
        }
        JSONObject joManifest = ServiceManifest.resolveManifestJsonTemplate(manifest.getJsonTemplate(), jsonModel);
        manifest.setJsonTemplate(joManifest.toJSONString());
        manifest.setJsonModel(null);
        logger.trace_end(method);        
        return manifest;
    }

}
