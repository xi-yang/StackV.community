/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import net.maxgigapop.mrs.rest.api.model.ApiDeltaVerification;
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
            throw new ProcessingException("Failed to compile service delta");
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
            throw new ProcessingException("Failed to compile service delta");
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
    @PUT
    @Path("/{siUUID}/{action}")
    public String push(@PathParam("siUUID") String svcInstanceUUID, @PathParam("action") String action) {
        long retryDelay = 1000L; // 1 sec
        long delayMax = 16000L; // 16 secs 
        if (action.equalsIgnoreCase("propagate")) {
            while (true) {
                retryDelay *= 2; // retry up to 4 times at 2, 4, 8, 16 secs
                try {
                    if (retryDelay == 2000L) {
                        return serviceCallHandler.propagateDeltas(svcInstanceUUID, true);
                    } else {
                        return serviceCallHandler.propagateRetry(svcInstanceUUID, true);
                    }   
                } catch (EJBException ejbEx) {
                    String errMsg = ejbEx.getMessage();
                    log.warning("Caught+Retry: " + errMsg);
                    if (errMsg.contains("could not execute statement") && retryDelay <= delayMax) {
                        try {
                            sleep(retryDelay);
                        } catch (InterruptedException ex) {
                            ;
                        }
                    } else {
                        serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
                        throw ejbEx;
                    }
                }
            }
        } else if (action.equalsIgnoreCase("propagate_through")) {
            while (true) {
                retryDelay *= 2; // retry up to 4 times at 2, 4, 8, 16 secs
                try {
                    if (retryDelay == 2000L) {
                        return serviceCallHandler.propagateDeltas(svcInstanceUUID, false);
                    } else {
                        return serviceCallHandler.propagateRetry(svcInstanceUUID, false);
                    }   
                } catch (EJBException ejbEx) {
                    String errMsg = ejbEx.getMessage();
                    log.warning("Caught+Retry: " + errMsg);
                    if (errMsg.contains("could not execute statement") && retryDelay <= delayMax) {
                        try {
                            sleep(retryDelay);
                        } catch (InterruptedException ex) {
                            ;
                        }
                    } else {
                        serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
                        throw ejbEx;
                    }
                }
            }
        } else if (action.equalsIgnoreCase("propagate_retry")) {
            while (true) {
                retryDelay *= 2; // retry up to 4 times at 2, 4, 8, 16 secs
                try {
                    return serviceCallHandler.propagateRetry(svcInstanceUUID, false);
                } catch (EJBException ejbEx) {
                    String errMsg = ejbEx.getMessage();
                    log.warning("Caught+Retry: " + errMsg);
                    if (errMsg.contains("could not execute statement") && retryDelay <= delayMax) {
                        try {
                            sleep(retryDelay);
                        } catch (InterruptedException ex) {
                            ;
                        }
                    } else {
                        serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
                        throw ejbEx;
                    }
                }
            }
        } else if (action.equalsIgnoreCase("propagate_forcedretry")) {
            while (true) {
                retryDelay *= 2; // retry up to 4 times at 2, 4, 8, 16 secs
                try {
                    return serviceCallHandler.propagateRetry(svcInstanceUUID, true);
                } catch (EJBException ejbEx) {
                    String errMsg = ejbEx.getMessage();
                    log.warning("Caught+Retry: " + errMsg);
                    if (errMsg.contains("could not execute statement") && retryDelay <= delayMax) {
                        try {
                            sleep(retryDelay);
                        } catch (InterruptedException ex) {
                            ;
                        }
                    } else {
                        serviceCallHandler.updateStatus(svcInstanceUUID, "FAILED");
                        throw ejbEx;
                    }
                }
            }
        } else if (action.equalsIgnoreCase("commit")) {
            return serviceCallHandler.commitDeltas(svcInstanceUUID, false);
        } else if (action.equalsIgnoreCase("commit_forced")) {
            return serviceCallHandler.commitDeltas(svcInstanceUUID, true);
        } else if (action.equalsIgnoreCase("revert")) {
            return serviceCallHandler.revertDeltas(svcInstanceUUID, false);
        } else if (action.equalsIgnoreCase("revert_forced")) {
            return serviceCallHandler.revertDeltas(svcInstanceUUID, true);
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
            log.info("ModelAdditionVerified=\n"+deltaVerification.getModelAdditionVerified());
        }
        if (deltaVerification.getModelAdditionUnverified() != null) {
            apiDeltaVerification.setUnverifiedModelAddition(deltaVerification.getModelAdditionUnverified());
            log.info("ModelAdditionUnverified=\n"+deltaVerification.getModelAdditionUnverified());
        }
        if (deltaVerification.getModelReductionVerified() != null) {
            apiDeltaVerification.setVerifiedModelReduction(deltaVerification.getModelReductionVerified());
            log.info("ModelReductionVerified=\n"+deltaVerification.getModelReductionVerified());
        }
        if (deltaVerification.getModelReductionUnverified() != null) {
            apiDeltaVerification.setUnverifiedModelReduction(deltaVerification.getModelReductionUnverified());
            log.info("ModelReductionUnverified=\n"+deltaVerification.getModelReductionUnverified());
        }
        if (deltaVerification.getAdditionVerified() != null) {
           apiDeltaVerification.setAdditionVerified(deltaVerification.getAdditionVerified() ? "true" : "false");
        }
        if (deltaVerification.getReductionVerified() != null) {
            apiDeltaVerification.setReductionVerified(deltaVerification.getReductionVerified() ? "true" : "false");
        }
        return apiDeltaVerification;
    }
    
}
