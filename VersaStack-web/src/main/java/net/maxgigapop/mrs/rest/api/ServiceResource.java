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

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import static java.lang.Thread.sleep;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javax.ws.rs.ProcessingException;
import javax.ws.rs.PUT;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import net.maxgigapop.mrs.bean.ServiceInstance;
import net.maxgigapop.mrs.bean.persist.DeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.ServiceInstancePersistenceManager;
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

    private static final Logger log = Logger.getLogger(ServiceResource.class.getName());

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
        String workerClassPath = svcApiDelta.getWorkerClassPath();
        SystemDelta sysDelta = serviceCallHandler.compileAddDelta(svcInstanceUUID, workerClassPath, svcApiDelta.getUuid(), svcApiDelta.getModelAddition(), svcApiDelta.getModelReduction());
        if (sysDelta == null) {
            throw new EJBException("Failed to compile service delta");
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
        return apiSysDelta;
    }

    @POST
    @Consumes({"application/xml", "application/json"})
    @Produces("application/json")
    @Path("/{siUUID}")
    public ApiDeltaBase compileJson(@PathParam("siUUID") String svcInstanceUUID, ServiceApiDelta svcApiDelta) throws Exception {
        String workerClassPath = svcApiDelta.getWorkerClassPath();
        SystemDelta sysDelta = serviceCallHandler.compileAddDelta(svcInstanceUUID, workerClassPath, svcApiDelta.getUuid(), svcApiDelta.getModelAddition(), svcApiDelta.getModelReduction());
        if (sysDelta == null) {
            throw new EJBException("Failed to compile service delta");
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
        return apiSysDelta;
    }

    //PUT to push and sync deltas
    //Propagate:
    // propagate_through (default): use cached VG + no refresh
    // propagate_forward: use persisted/updated VG + no refresh
    // propagate_forced: refresh VG and apply delta to refreshed only
    //      forced only work after FAILED status
    //Commit:
    // Async - status update later
    //      forced: only work after FAILED status
    //Revert: 
    // revert forced: after FAILED status
    //Refresh: refresh VG to latest from drivers
    @PUT
    @Path("/{siUUID}/{action}")
    public String push(@PathParam("siUUID") String svcInstanceUUID, @PathParam("action") String action) {
        if (action.equalsIgnoreCase("propagate")
                || action.equalsIgnoreCase("propagate_through")) {
            try {
                return serviceCallHandler.propagateDeltas(svcInstanceUUID, true, false);
            } catch (EJBException ejbEx) {
                serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
                throw ejbEx;
            }
        } else if (action.equalsIgnoreCase("propagate_forward")) {
            try {
                return serviceCallHandler.propagateDeltas(svcInstanceUUID, false, false);
            } catch (EJBException ejbEx) {
                serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
                throw ejbEx;
            }
        } else if (action.equalsIgnoreCase("propagate_forced")) {
            try {
                return serviceCallHandler.propagateDeltas(svcInstanceUUID, false, true);
            } catch (EJBException ejbEx) {
                serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
                throw ejbEx;
            }
        } else if (action.equalsIgnoreCase("propagate_retry")) {
            try {
                return serviceCallHandler.propagateRetry(svcInstanceUUID, true, false);
            } catch (EJBException ejbEx) {
                serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
                throw ejbEx;
            }
        } else if (action.equalsIgnoreCase("propagate_forwardretry")) {
            try {
                return serviceCallHandler.propagateRetry(svcInstanceUUID, false, false);
            } catch (EJBException ejbEx) {
                serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
                throw ejbEx;
            }
        } else if (action.equalsIgnoreCase("propagate_forcedretry")) {
            try {
                return serviceCallHandler.propagateRetry(svcInstanceUUID, false, true);
            } catch (EJBException ejbEx) {
                serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
                throw ejbEx;
            }
        } else if (action.equalsIgnoreCase("commit")) {
            return serviceCallHandler.commitDeltas(svcInstanceUUID, false);
        } else if (action.equalsIgnoreCase("commit_forced")) {
            return serviceCallHandler.commitDeltas(svcInstanceUUID, true);
        } else if (action.equalsIgnoreCase("revert")) {
            try {
                return serviceCallHandler.revertDeltas(svcInstanceUUID, false);
            } catch (EJBException ejbEx) {
                serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
                throw ejbEx;
            }
        } else if (action.equalsIgnoreCase("revert_forced")) {
            try {
                return serviceCallHandler.revertDeltas(svcInstanceUUID, true);
            } catch (EJBException ejbEx) {
                serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
                throw ejbEx;
            }
        } else if (action.equalsIgnoreCase("refresh")) {
            serviceCallHandler.refreshVersionGroup(svcInstanceUUID);
            return "REFRESHED";
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

    @GET
    @Produces("application/json")
    @Path("/verify/{sdUUID}")
    public ApiDeltaVerification verifyJson(@PathParam("sdUUID") String svcDeltaUUID) throws Exception {
        ApiDeltaVerification apiDeltaVerification = new ApiDeltaVerification();
        java.util.Date now = new java.util.Date();
        apiDeltaVerification.setCreationTime(new java.sql.Date(now.getTime()).toString());
        apiDeltaVerification.setReferenceUUID(svcDeltaUUID);
        ModelUtil.DeltaVerification deltaVerification = new ModelUtil.DeltaVerification();
        serviceCallHandler.verifyDelta(svcDeltaUUID, deltaVerification, true);
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
        return apiDeltaVerification;
    }
    
    @GET
    @Produces("application/xml")
    @Path("/verify/{sdUUID}")
    public ApiDeltaVerification verify(@PathParam("sdUUID") String svcDeltaUUID) throws Exception {
        ApiDeltaVerification apiDeltaVerification = new ApiDeltaVerification();
        java.util.Date now = new java.util.Date();
        apiDeltaVerification.setCreationTime(new java.sql.Date(now.getTime()).toString());
        apiDeltaVerification.setReferenceUUID(svcDeltaUUID);
        ModelUtil.DeltaVerification deltaVerification = new ModelUtil.DeltaVerification();
        serviceCallHandler.verifyDelta(svcDeltaUUID, deltaVerification, false);
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
        return apiDeltaVerification;
    }
    
    @GET
    @Produces("application/json")
    @Path("/delta/{svcUUID}")
    public ApiDeltaRetrieval retrieveDeltaJson(@PathParam("svcUUID") String svcUUID) throws Exception {
        ApiDeltaRetrieval apiDeltaRetrieval = new ApiDeltaRetrieval();
        ModelUtil.DeltaRetrieval deltaRetrieval = new ModelUtil.DeltaRetrieval();
        serviceCallHandler.retrieveDelta(svcUUID, deltaRetrieval, true);
        apiDeltaRetrieval.setReferenceUUID(deltaRetrieval.getReferenceModelUUID());
        apiDeltaRetrieval.setServiceModelAddition(deltaRetrieval.getModelAdditionSvc());
        apiDeltaRetrieval.setServiceModelReduction(deltaRetrieval.getModelReductionSvc());
        apiDeltaRetrieval.setSystemModelAddition(deltaRetrieval.getModelAdditionSys());
        apiDeltaRetrieval.setSystemModelReduction(deltaRetrieval.getModelReductionSys());
        return apiDeltaRetrieval;
    }
    
    @GET
    @Produces("application/xml")
    @Path("/delta/{svcUUID}")
    public ApiDeltaRetrieval retrieveDelta(@PathParam("svcUUID") String svcUUID) throws Exception {
        ApiDeltaRetrieval apiDeltaRetrieval = new ApiDeltaRetrieval();
        ModelUtil.DeltaRetrieval deltaRetrieval = new ModelUtil.DeltaRetrieval();
        serviceCallHandler.retrieveDelta(svcUUID, deltaRetrieval, false);
        apiDeltaRetrieval.setReferenceUUID(deltaRetrieval.getReferenceModelUUID());
        apiDeltaRetrieval.setServiceModelAddition(deltaRetrieval.getModelAdditionSvc());
        apiDeltaRetrieval.setServiceModelReduction(deltaRetrieval.getModelReductionSvc());
        apiDeltaRetrieval.setSystemModelAddition(deltaRetrieval.getModelAdditionSys());
        apiDeltaRetrieval.setSystemModelReduction(deltaRetrieval.getModelReductionSys());
        return apiDeltaRetrieval;
    }
    
    @POST
    @Path("/manifest")
    @Consumes({"application/json","application/xml"})
    @Produces("application/json")
    public ServiceApiManifest resolveManifest(ServiceApiManifest manifest) {
        // if manifest.getJsonModel() == null, get serviceDelta.modelAddition into manifest.jsonTemplate
        String jsonModel = manifest.getJsonModel();
        if (jsonModel == null) {
            return resolveServiceManifest(manifest.getServiceUUID(), manifest);
        }
        JSONObject joManifest = ServiceManifest.resolveManifestJsonTemplate(manifest.getJsonTemplate(), jsonModel);
        manifest.setJsonTemplate(joManifest.toJSONString());
        manifest.setJsonModel(null);
        return manifest;
    }
    
    @POST
    @Path("/manifest/{svcUUID}")
    @Consumes({"application/json","application/xml"})
    @Produces("application/json")
    public ServiceApiManifest resolveServiceManifest(@PathParam("svcUUID") String svcUUID, ServiceApiManifest manifest) {
        // if manifest.getJsonModel() == null, get serviceDelta.modelAddition into manifest.jsonTemplate
        manifest.setServiceUUID(svcUUID);
        ModelUtil.DeltaVerification deltaVerification = new ModelUtil.DeltaVerification();
        serviceCallHandler.verifyDelta(manifest.getServiceUUID(), deltaVerification, true);
        String jsonModel = deltaVerification.getModelAdditionVerified();
        if (jsonModel == null) {
            throw new EJBException("resolveServiceManifest cannot get verified modelAddition for service UUID="+manifest.getServiceUUID());
        }
        JSONObject joManifest = ServiceManifest.resolveManifestJsonTemplate(manifest.getJsonTemplate(), jsonModel);
        manifest.setJsonTemplate(joManifest.toJSONString());
        manifest.setJsonModel(null);
        return manifest;
    }

}
