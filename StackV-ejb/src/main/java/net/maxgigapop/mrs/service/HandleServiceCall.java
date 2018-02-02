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

package net.maxgigapop.mrs.service;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import net.maxgigapop.mrs.bean.DeltaModel;
import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.bean.DriverSystemDelta;
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.bean.ServiceInstance;
import net.maxgigapop.mrs.bean.SystemDelta;
import net.maxgigapop.mrs.bean.SystemInstance;
import net.maxgigapop.mrs.bean.VersionGroup;
import net.maxgigapop.mrs.bean.persist.DeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.ServiceDeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.ServiceInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.SystemInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionGroupPersistenceManager;
import net.maxgigapop.mrs.common.EJBExceptionNegotiable;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.core.DataConcurrencyPoster;
import net.maxgigapop.mrs.core.SystemModelCoordinator;
import net.maxgigapop.mrs.service.orchestrate.WorkerBase;
import net.maxgigapop.mrs.system.HandleSystemCall;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author xyang
 */
@Stateless
@LocalBean
public class HandleServiceCall {
    @EJB
    HandleSystemCall systemCallHandler;

    private static final StackLogger logger = new StackLogger(HandleServiceCall.class.getName(), "HandleServiceCall");
    
    public ServiceInstance createInstance() {
        logger.cleanup();
        logger.start("createInstance");
        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setReferenceUUID(UUID.randomUUID().toString());
        logger.refuuid(serviceInstance.getReferenceUUID());
        ServiceInstancePersistenceManager.save(serviceInstance);
        logger.end("createInstance");
        return serviceInstance;
    }

    public void terminateInstance(String refUUID) {
        logger.cleanup();
        logger.refuuid(refUUID);
        logger.start("terminateInstance");
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(refUUID);
        if (serviceInstance != null) {
            serviceInstance = ServiceInstancePersistenceManager.findById(serviceInstance.getId());
        }
        if (serviceInstance == null) {
            throw logger.error_throwing("terminateInstance", "Cannot find the ServiceInstance by refUUID");
        }
        if (serviceInstance.getServiceDeltas() != null) {
            // clean up serviceDeltas
            for (Iterator<ServiceDelta> svcDeltaIt = serviceInstance.getServiceDeltas().iterator(); svcDeltaIt.hasNext();) {
                ServiceDelta svcDelta = svcDeltaIt.next();
                if (svcDelta.getSystemDelta() != null) {
                    if (svcDelta.getSystemDelta().getDriverSystemDeltas() != null) {
                        for (Iterator<DriverSystemDelta> dsdIt = svcDelta.getSystemDelta().getDriverSystemDeltas().iterator(); dsdIt.hasNext();) {
                            DriverSystemDelta dsd = dsdIt.next();
                            //DriverInstance driverInstance = DriverInstancePersistenceManager.findByTopologyUri(dsd.getDriverInstance().getTopologyUri());
                            DriverInstance driverInstance = dsd.getDriverInstance();
                            driverInstance.getDriverSystemDeltas().remove(dsd);
                            DeltaPersistenceManager.delete(dsd);
                        }
                    }
                    SystemInstance systemInstance = SystemInstancePersistenceManager.findBySystemDelta(svcDelta.getSystemDelta());
                    if (systemInstance != null) {
                        SystemInstancePersistenceManager.delete(systemInstance);
                    }
                    DeltaPersistenceManager.delete(svcDelta.getSystemDelta());
                }
                svcDeltaIt.remove();
                DeltaPersistenceManager.delete(svcDelta);
            }
        }
        ServiceInstancePersistenceManager.delete(serviceInstance);
        logger.end("terminateInstance");
    }

    public void setInstanceProperty(String refUUID, String property, String value) {
        logger.refuuid(refUUID);
        logger.trace("setInstanceProperty", String.format("set property %s=%s", property, value));
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(refUUID);
        if (serviceInstance == null) {
            throw logger.error_throwing("getInstanceProperty", "cannot find the ref:ServiceInstance");
        }
        serviceInstance.getProperties().put(property, value);
        ServiceInstancePersistenceManager.merge(serviceInstance);
    }

    public String getInstanceProperty(String refUUID, String property) {
        logger.refuuid(refUUID);
        logger.trace("setInstanceProperty", String.format("set property %s", property));
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(refUUID);
        if (serviceInstance == null) {
            throw logger.error_throwing("getInstanceProperty", "cannot find the ref:ServiceInstance");
        }
        return serviceInstance.getProperty(property);
    }


    public Map listInstanceProperties(String refUUID) {
        logger.refuuid(refUUID);
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(refUUID);
        if (serviceInstance == null) {
            throw logger.error_throwing("listInstanceProperty", "cannot find the ref:ServiceInstance");
        }
        return serviceInstance.getProperties();
    }
    
    public SystemDelta compileAddDelta(String serviceInstanceUuid, String workerClassPath, ServiceDelta spaDelta) {
        logger.refuuid(serviceInstanceUuid);
        logger.targetid(spaDelta.getReferenceUUID());
        logger.start("compileAddDelta", "COMPILING");
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceInstanceUuid);
        if (serviceInstance == null) {
            throw logger.error_throwing("compileAddDelta", "cannot find the ref:ServiceInstance");
        }
        if (ServiceDeltaPersistenceManager.findByReferenceUUID(spaDelta.getReferenceUUID()) != null) {
            throw logger.error_throwing("compileAddDelta", "has already received the same traget:ServiceDelta");
        }
        // run with chosen worker
        WorkerBase worker = null;
        try {
            worker = (WorkerBase) this.getClass().getClassLoader().loadClass(workerClassPath).newInstance();
        } catch (Exception ex) {
            throw logger.throwing("compileAddDelta", ex);
        }
        spaDelta.setReferenceUUID(serviceInstanceUuid);
        worker.setAnnoatedModel(spaDelta);
        try {
            worker.run();
        } catch (EJBException ex) {
            serviceInstance.setStatus("FAILED");
            ServiceInstancePersistenceManager.merge(serviceInstance);
            logger.status("compileAddDelta", "FAILED");
            throw logger.throwing("compileAddDelta", ex);
        }
        // save serviceInstance, spaDelta and systemDelta
        SystemDelta resultDelta = worker.getResultModelDelta();
        resultDelta.setServiceDelta(spaDelta);
        //DeltaPersistenceManager.save(resultDelta);
        spaDelta.setSystemDelta(resultDelta);
        if (serviceInstance.getServiceDeltas() == null) {
            serviceInstance.setServiceDeltas(new ArrayList<ServiceDelta>());
        }
        serviceInstance.addServiceDeltaWithoutSave(spaDelta);
        spaDelta.setServiceInstance(serviceInstance);
        DeltaPersistenceManager.save(spaDelta);
        //serviceInstance = ServiceInstancePersistenceManager.findById(serviceInstance.getId());
        serviceInstance.setStatus("COMPILED");
        ServiceInstancePersistenceManager.merge(serviceInstance);
        logger.end("compileAddDelta", "COMPILED");
        return resultDelta;
    }

    public SystemDelta compileAddDelta(String serviceInstanceUuid, String workerClassPath, String uuid, String modelAdditionTtl, String modelReductionTtl) {
        logger.cleanup();
        logger.refuuid(serviceInstanceUuid);
        logger.targetid(uuid);
        logger.start("compileAddDelta");
        ServiceDelta spaDelta = new ServiceDelta();
        spaDelta.setReferenceUUID(uuid);
        try {
            DeltaModel dmAddition = null;
            if (modelAdditionTtl != null) {
                OntModel modelAddition = ModelUtil.unmarshalOntModel(modelAdditionTtl);
                dmAddition = new DeltaModel();
                dmAddition.setCommitted(false);
                dmAddition.setDelta(spaDelta);
                dmAddition.setIsAddition(true);
                dmAddition.setOntModel(modelAddition);
            }
            spaDelta.setModelAddition(dmAddition);
        } catch (Exception ex) {
            throw logger.error_throwing("compileAddDelta", "unmarshalOntModel(modelAdditionTtl) -exception- " + ex);
        }
        try {
            DeltaModel dmReduction = null;
            if (modelReductionTtl != null) {
                OntModel modelReduction = ModelUtil.unmarshalOntModel(modelReductionTtl);
                dmReduction = new DeltaModel();
                dmReduction.setCommitted(false);
                dmReduction.setDelta(spaDelta);
                dmReduction.setIsAddition(false);
                dmReduction.setOntModel(modelReduction);
            }
            spaDelta.setModelReduction(dmReduction);
        } catch (Exception ex) {
            throw logger.error_throwing("compileAddDelta", "unmarshalOntModel(modelReductionTtl) -exception- " + ex);
        }
        return compileAddDelta(serviceInstanceUuid, workerClassPath, spaDelta);
    }

    public SystemDelta recompileDeltas(String serviceInstanceUuid, String workerClassPath) {
        String method = "recompileDelta";
        logger.refuuid(serviceInstanceUuid);
        logger.start(method, "COMPILING");
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceInstanceUuid);
        if (serviceInstance == null) {
            throw logger.error_throwing(method, "cannot find the ref:ServiceInstance");
        }
        logger.status(method, serviceInstance.getStatus());
        if (!serviceInstance.getStatus().equals("NEGOTIATING")) {
            throw logger.error_throwing(method, "ref:ServiceInstance must have status=NEGOTIATING while the actual status=" + serviceInstance.getStatus());
        }
        Iterator<ServiceDelta> itSD = serviceInstance.getServiceDeltas().iterator();
        if (!itSD.hasNext()) {
            throw logger.error_throwing(method, "ref:ServiceInstance has none delta to propagate.");
        }
        ServiceDelta spaDelta = null;
        while (itSD.hasNext()) {
            ServiceDelta serviceDelta = itSD.next();
            if (serviceDelta.getSystemDelta() == null) {
                logger.targetid(serviceDelta.getId());
                logger.error(method, "target:ServiceDelta getSystemDelta() == null -but- continue");
                continue;
            } else if (serviceDelta.getStatus().equals("NEGOTIATING")) {
                spaDelta = serviceDelta;
            } else if (serviceDelta.getStatus().equals("FAILED")) {
                if (spaDelta != null) {
                    logger.warning(method, "A FAILED delta (" + serviceDelta + ") comes after NEGOTIATING delta (" + spaDelta +") - ignore the previous NEGOTIATING delta.");
                    spaDelta = null; 
                }
            }
        }
        if (spaDelta == null) {
            throw logger.error_throwing(method, "ref:ServiceInstance has none active delta with status=NEGOTIATING");
        }
        logger.targetid(spaDelta.getReferenceUUID());
        WorkerBase worker = null;
        try {
            worker = (WorkerBase) this.getClass().getClassLoader().loadClass(workerClassPath).newInstance();
        } catch (Exception ex) {
            throw logger.throwing(method, ex);
        }
        spaDelta.setReferenceUUID(serviceInstanceUuid);
        worker.setAnnoatedModel(spaDelta);
        try {
            worker.run();
        } catch (EJBException ex) {
            serviceInstance.setStatus("FAILED");
            ServiceInstancePersistenceManager.merge(serviceInstance);
            logger.status(method, "FAILED");
            throw logger.throwing(method, ex);
        }
        // save serviceInstance, spaDelta and systemDelta
        SystemDelta resultDelta = worker.getResultModelDelta();
        resultDelta.setServiceDelta(spaDelta);
        spaDelta.setSystemDelta(resultDelta);
        spaDelta.setStatus("COMPILED");
        DeltaPersistenceManager.merge(spaDelta);
        serviceInstance.setStatus("COMPILED");
        ServiceInstancePersistenceManager.merge(serviceInstance);
        logger.end(method, "COMPILED");
        return resultDelta;
    }
    
    // handling multiple deltas:  propagate + commit + query = transactional propagate + parallel commits
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String propagateDeltas(String serviceInstanceUuid, boolean useCachedVG, boolean refreshForced) {
        logger.cleanup();
        String method = "propagateDeltas";
        logger.refuuid(serviceInstanceUuid);
        logger.start(method);
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceInstanceUuid);
        if (serviceInstance == null) {
            throw logger.error_throwing(method, "cannot find ref:ServiceInstance");
        }
        logger.status(method, serviceInstance.getStatus());
        if (!serviceInstance.getStatus().equals("COMPILED") && !serviceInstance.getStatus().equals("PROPAGATED-PARTIAL") && !serviceInstance.getStatus().equals("COMMITTING-PARTIAL")) {
            throw logger.error_throwing(method, "ref:ServiceInstance must have status=INIT or PROPAGATED-PARTIAL or COMMITTED-PARTIAL while the actual status=" + serviceInstance.getStatus());
        }
        Iterator<ServiceDelta> itSD = serviceInstance.getServiceDeltas().iterator();
        if (!itSD.hasNext()) {
            throw logger.error_throwing(method, "ref:ServiceInstance has none delta to propagate.");
        }
        // By default propagate a delta if it is the first with init status in queue or there is only committing ones before.
        // Commit only one delta at a time.
        boolean canMultiPropagate = false;
        String multiPropagate = serviceInstance.getProperty("multiPropagate");
        if (multiPropagate != null && multiPropagate.equalsIgnoreCase("true")) {
            canMultiPropagate = true;
        }
        while (itSD.hasNext()) {
            ServiceDelta serviceDelta = itSD.next();
            if (serviceDelta.getSystemDelta() == null) {
                logger.targetid(serviceDelta.getId());
                logger.error(method, "target:ServiceDelta getSystemDelta() == null -but- continue");
                continue;
            } else if (serviceDelta.getStatus().equals("INIT")) {
                SystemInstance systemInstance = systemCallHandler.createInstance();
                try {
                    systemCallHandler.propagateDelta(systemInstance, serviceDelta.getSystemDelta(), useCachedVG, refreshForced);
                } catch (EJBExceptionNegotiable ex) {
                    JSONParser parser = new JSONParser();
                    JSONObject jsonObj;
                    try {
                        jsonObj = (JSONObject) parser.parse(ex.getMessage());
                    } catch (ParseException ex1) {
                        throw logger.error_throwing(method, "received EJBExceptionNegotiable (" + ex  + "), but failed to parse JSON: " + ex1);
                    }
                    if (!jsonObj.containsKey("conflict") && !jsonObj.containsKey("markup")) {
                        throw logger.error_throwing(method, "received EJBExceptionNegotiable (" + ex  + "), but without providing 'conflict' or 'markup' data");
                    }
                    serviceDelta.setNegotiationMarkup(ex.getMessage());
                    serviceDelta.setStatus("NEGOTIATING");
                    DeltaPersistenceManager.merge(serviceDelta);
                } catch (EJBException ex) {
                    logger.throwing(method, ex);
                }
                serviceDelta.setStatus("PROPAGATED");
                DeltaPersistenceManager.merge(serviceDelta);
                if (!canMultiPropagate) {
                    break;
                }
            } else if (!canMultiPropagate && !serviceDelta.getStatus().equals("COMMITTING") && !serviceDelta.getStatus().equals("COMMITTED")) {
                throw logger.error_throwing(method, "ref:ServiceInstance with 'multiPropagate=false' encounters target:ServiceDelta with status="+serviceDelta.getStatus());
            }
        }
        itSD = serviceInstance.getServiceDeltas().iterator();
        boolean hasInitiated = false;
        boolean hasPropagated = false;
        boolean isCommitting = false;
        boolean isNegotiating = false;
        while (itSD.hasNext()) {
            ServiceDelta serviceDelta = itSD.next();
            if (serviceDelta.getStatus().equalsIgnoreCase("INIT")) {
                hasInitiated = true;
            } else if (serviceDelta.getStatus().equalsIgnoreCase("PROPAGATED")) {
                hasPropagated = true;
            } else if (serviceDelta.getStatus().equalsIgnoreCase("COMMITTING")) {
                isCommitting = true;
            } else if (serviceDelta.getStatus().equalsIgnoreCase("NEGOTIATING")) {
                isNegotiating = true;
            }
        }
        //serviceInstance.setStatus("COMPILED");
        if (isNegotiating) {
            serviceInstance.setStatus("NEGOTIATING");
        } else if (hasInitiated && hasPropagated) {
            serviceInstance.setStatus("PROPAGATED-PARTIAL");
        } else if (hasPropagated && !isCommitting) {
            serviceInstance.setStatus("PROPAGATED");
        } else if (hasPropagated && isCommitting) {
            serviceInstance.setStatus("COMMITTING-PARTIAL");
        } else if (!hasPropagated && isCommitting) {
            serviceInstance.setStatus("COMMITTING");
        }
        ServiceInstancePersistenceManager.merge(serviceInstance);
        logger.end(method, serviceInstance.getStatus());
        return serviceInstance.getStatus();
    }

    public String commitDeltas(String serviceInstanceUuid, boolean forced) {
        logger.cleanup();
        String method = "commitDeltas";
        logger.refuuid(serviceInstanceUuid);
        logger.start(method);
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceInstanceUuid);
        if (serviceInstance == null) {
            throw logger.error_throwing(method, "cannot find ref:ServiceInstance.");
        }
        if (!serviceInstance.getStatus().equals("PROPAGATED")
                && !serviceInstance.getStatus().equals("PROPAGATED-PARTIAL")
                && !serviceInstance.getStatus().equals("COMMITTING-PARTIAL")) {
            throw logger.error_throwing(method, "ref:ServiceInstance must have status=PROPAGATED or PROPAGATED-PARTIAL or COMMITTED-PARTIAL while the actual status=" + serviceInstance.getStatus());
        }
        if (serviceInstance.getServiceDeltas() == null || serviceInstance.getServiceDeltas().isEmpty()) {
            throw logger.error_throwing(method, "ref:ServiceInstance has none delta to commit.");
        }
        Iterator<ServiceDelta> itSD = serviceInstance.getServiceDeltas().iterator();
        // By default commit a delta if it is the first with propagated status in queue or there is only committing ones before.
        // Also commit only one delta at a time.
        boolean canMultiCommit = false;
        String multiCommit = serviceInstance.getProperty("multiCommit");
        if (multiCommit != null && multiCommit.equalsIgnoreCase("true")) {
            canMultiCommit = true;
        }
        while (itSD.hasNext()) {
            ServiceDelta serviceDelta = itSD.next();
            if (serviceDelta.getSystemDelta() == null) {
                logger.warning(method, "target:SreviceDelta is null -but- skip and continue");
                continue; // ?? exception ??
            }
            if (serviceDelta.getStatus().equals("PROPAGATED")) {
                SystemInstance systemInstance = SystemInstancePersistenceManager.findBySystemDelta(serviceDelta.getSystemDelta());
                if (systemInstance.getSystemDelta().getDriverSystemDeltas() == null || systemInstance.getSystemDelta().getDriverSystemDeltas().isEmpty()) {
                    throw logger.error_throwing(method, "ref:ServiceInstance has no change to commit (empty driver delta list).");
                }
                systemInstance = SystemInstancePersistenceManager.findByReferenceUUID(systemInstance.getReferenceUUID());
                Future<String> asynResult = systemCallHandler.commitDelta(systemInstance);
                systemInstance.setCommitStatus(asynResult);
                //systemInstance.setCommitFlag(true);
                serviceDelta.setStatus("COMMITTING");
                DeltaPersistenceManager.merge(serviceDelta);
                if (!canMultiCommit) {
                    break;
                }
            } else if (!forced && !canMultiCommit && !serviceDelta.getStatus().equals("COMMITTING") && !serviceDelta.getStatus().equals("COMMITTED")) {
                throw logger.error_throwing(method, "ref:ServiceInstance with forced==false and canMultiCommit==false encounters target:ServiceDelta in unexpected status=" + serviceDelta.getStatus());
            }
        }
        itSD = serviceInstance.getServiceDeltas().iterator();
        boolean hasInitiated = false;
        boolean hasPropagated = false;
        boolean isCommitting = false;
        while (itSD.hasNext()) {
            ServiceDelta serviceDelta = itSD.next();
            if (serviceDelta.getStatus().equalsIgnoreCase("INIT")) {
                hasInitiated = true;
            } else if (serviceDelta.getStatus().equalsIgnoreCase("PROPAGATED")) {
                hasPropagated = true;
            } else if (serviceDelta.getStatus().equalsIgnoreCase("COMMITTING")) {
                isCommitting = true;
            }
        }
        //serviceInstance.setStatus("INIT");
        if (hasInitiated && hasPropagated) {
            serviceInstance.setStatus("PROPAGATED-PARTIAL");
        } else if (hasPropagated && !isCommitting) {
            serviceInstance.setStatus("PROPAGATED");
        } else if (hasPropagated && isCommitting) {
            serviceInstance.setStatus("COMMITTING-PARTIAL");
        } else if (!hasPropagated && isCommitting) {
            serviceInstance.setStatus("COMMITTING");
        }
        ServiceInstancePersistenceManager.merge(serviceInstance);
        logger.end(method, serviceInstance.getStatus());
        return serviceInstance.getStatus();
    }

    public void refreshVersionGroup(String serviceInstanceUuid) {
        logger.cleanup();
        String method = "refreshVersionGroup";
        logger.refuuid(serviceInstanceUuid);
        logger.start(method);
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceInstanceUuid);
        if (serviceInstance == null) {
            throw logger.error_throwing(method, "cannot find ref:ServiceInstance.");
        }
        VersionGroup vg = null;
        Iterator<ServiceDelta> itSD = serviceInstance.getServiceDeltas().iterator();
        while (itSD.hasNext()) {
            ServiceDelta svcDelta = itSD.next();
            if (svcDelta.getSystemDelta() != null && svcDelta.getSystemDelta().getReferenceVersionGroup() != null) {
                vg = svcDelta.getSystemDelta().getReferenceVersionGroup();
            }
        }
        if (vg != null) {
            VersionGroupPersistenceManager.refreshToHead(vg, true);
            logger.targetid(vg.getRefUuid());
            logger.message(method, "target:VersionGroup refreshed");
        }
        logger.end(method);
    }

    public String revertDeltas(String serviceInstanceUuid, boolean forced) {
        logger.cleanup();
        String method = "revertDeltas";
        logger.refuuid(serviceInstanceUuid);
        logger.start(method);
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceInstanceUuid);
        if (serviceInstance == null) {
            throw logger.error_throwing(method, "cannot find ref:ServiceInstance.");
        }
        logger.status(method, serviceInstance.getStatus());
        if (!forced && !serviceInstance.getStatus().startsWith("PROPAGATED")
                && !serviceInstance.getStatus().startsWith("COMMITTING")
                && !serviceInstance.getStatus().equals("COMMITTED")
                && !serviceInstance.getStatus().equals("READY")) {
            throw logger.error_throwing(method, "ref:ServiceInstance with forced==false must have status=PROPAGATED or COMMITTING or COMMITTED or READY while the actual status=" + serviceInstance.getStatus());
        }
        Iterator<ServiceDelta> itSD = serviceInstance.getServiceDeltas().iterator();
        List<ServiceDelta> reversableServiceDeltas = new ArrayList<>();
        while (itSD.hasNext()) {
            ServiceDelta svcDelta = itSD.next();
            if (svcDelta.getStatus().equalsIgnoreCase("COMMITTED") || svcDelta.getStatus().equalsIgnoreCase("COMMITTING")
                    || (forced && svcDelta.getStatus().equalsIgnoreCase("FAILED"))) {
                reversableServiceDeltas.add(svcDelta);
            }
        }
        itSD = reversableServiceDeltas.iterator();
        if (!itSD.hasNext()) {
            throw logger.error_throwing(method, "ref:ServiceInstance has none delta to commit.");
        }
        ServiceDelta reverseSvcDelta = new ServiceDelta();
        reverseSvcDelta.setServiceInstance(serviceInstance);
        //reverseSvcDelta.setReferenceUUID(UUID.randomUUID().toString());
        reverseSvcDelta.setReferenceUUID(serviceInstanceUuid);
        reverseSvcDelta.setStatus("INIT");
        DeltaModel dmAddition = new DeltaModel();
        dmAddition.setCommitted(false);
        dmAddition.setDelta(reverseSvcDelta);
        dmAddition.setIsAddition(true);
        dmAddition.setOntModel(ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF));
        DeltaModel dmReduction = new DeltaModel();
        dmReduction.setCommitted(false);
        dmReduction.setDelta(reverseSvcDelta);
        dmReduction.setIsAddition(true);
        dmReduction.setOntModel(ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF));
        reverseSvcDelta.setModelAddition(dmAddition);
        reverseSvcDelta.setModelReduction(dmReduction);
        SystemDelta reverseSysDelta = new SystemDelta();
        reverseSysDelta.setServiceDelta(reverseSvcDelta);
        reverseSvcDelta.setSystemDelta(reverseSysDelta);
        DeltaModel dmSysAddition = new DeltaModel();
        dmSysAddition.setCommitted(false);
        dmSysAddition.setDelta(reverseSvcDelta);
        dmSysAddition.setIsAddition(true);
        dmSysAddition.setOntModel(ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF));
        DeltaModel dmSysReductiom = new DeltaModel();
        dmSysReductiom.setCommitted(false);
        dmSysReductiom.setDelta(reverseSvcDelta);
        dmSysReductiom.setIsAddition(true);
        dmSysReductiom.setOntModel(ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MICRO_RULE_INF));
        reverseSysDelta.setModelAddition(dmSysAddition);
        reverseSysDelta.setModelReduction(dmSysReductiom);
        boolean canRevertAll = false;
        String revertAll = serviceInstance.getProperty("revertAll");
        if (revertAll != null && revertAll.equalsIgnoreCase("true")) {
            canRevertAll = true;
        }
        while (itSD.hasNext()) {
            ServiceDelta serviceDelta = itSD.next();
            if (!canRevertAll && itSD.hasNext()) {
                continue;
            }
            SystemDelta systemDelta = serviceDelta.getSystemDelta();
            if (systemDelta == null) {
                throw logger.error_throwing(method, "ref:ServiceInstance encounters uncompiled ref:ServiceDelta (having null SystemDelta).");
            }
            if (systemDelta.getReferenceVersionGroup() != null) {
                //@TODO: make sure versionGroup has been updated (mandate verify method?)
                reverseSysDelta.setReferenceVersionGroup(systemDelta.getReferenceVersionGroup());
            }
            // swap and add addition and reduction to reverse models
            if (serviceDelta.getModelReduction() != null && serviceDelta.getModelReduction().getOntModel() != null) {
                reverseSvcDelta.getModelAddition().getOntModel().add(serviceDelta.getModelReduction().getOntModel().getBaseModel());
            }
            if (serviceDelta.getModelAddition() != null && serviceDelta.getModelAddition().getOntModel() != null) {
                reverseSvcDelta.getModelReduction().getOntModel().add(serviceDelta.getModelAddition().getOntModel().getBaseModel());
            }
            List<String> includeMatches = new ArrayList<String>();
            List<String> excludeMatches = new ArrayList<String>();
            List<String> excludeExtentials = new ArrayList<String>();
            excludeMatches.add("#isAlias");
            excludeMatches.add("#providedBy");
            excludeMatches.add("#belongsTo");
            excludeExtentials.add("#nextHop");
            excludeExtentials.add("#routeFrom");
            excludeExtentials.add("#routeTo");
            //@TODO: exclude essential Resource trees under routeTo / routeFrom / nextHop ==> excludeEssentials (include the statement but not go further)
            String sparql = "SELECT ?res WHERE {?s ?p ?res. "
                    + "FILTER(regex(str(?p), '#has|#provides'))"
                    + "}";
            OntModel refModel = this.fetchReferenceModel();
            if (refModel == null) {
                throw logger.error_throwing(method, "fetchReferenceModel() returned null - systemModelCoordinator not ready or contending on access lock");
            }
            if (systemDelta.getModelReduction() != null && systemDelta.getModelReduction().getOntModel() != null) {
                reverseSysDelta.getModelAddition().getOntModel().add(systemDelta.getModelReduction().getOntModel().getBaseModel());
                ResultSet rs = ModelUtil.sparqlQuery(systemDelta.getModelReduction().getOntModel().getBaseModel(), sparql);
                List resList = new ArrayList<Resource>();
                while (rs.hasNext()) {
                    QuerySolution querySolution = rs.next();
                    Resource res = querySolution.getResource("res");
                    resList.add(res);
                }
                if (!resList.isEmpty()) {
                    Model sysModelReductionExt = ModelUtil.getModelSubTree(refModel, resList, includeMatches, excludeMatches, excludeExtentials);
                    reverseSysDelta.getModelAddition().getOntModel().add(sysModelReductionExt);
                    reverseSysDelta.getModelReduction().getOntModel().remove(sysModelReductionExt);
                }
            }
            if (systemDelta.getModelAddition() != null && systemDelta.getModelAddition().getOntModel() != null) {
                reverseSysDelta.getModelReduction().getOntModel().add(systemDelta.getModelAddition().getOntModel().getBaseModel());
                ResultSet rs = ModelUtil.sparqlQuery(systemDelta.getModelAddition().getOntModel().getBaseModel(), sparql);
                List resList = new ArrayList<Resource>();
                while (rs.hasNext()) {
                    QuerySolution querySolution = rs.next();
                    Resource res = querySolution.getResource("res");
                    resList.add(res);
                }
                if (!resList.isEmpty()) {
                    Model sysModelAdditionExt = ModelUtil.getModelSubTree(refModel, resList, includeMatches, excludeMatches, excludeExtentials);
                    reverseSysDelta.getModelReduction().getOntModel().add(sysModelAdditionExt);
                    reverseSysDelta.getModelAddition().getOntModel().remove(sysModelAdditionExt);
                }
            }
        }
        serviceInstance.addServiceDeltaWithoutSave(reverseSvcDelta);
        DeltaPersistenceManager.save(reverseSvcDelta);
        //serviceInstance = ServiceInstancePersistenceManager.findById(serviceInstance.getId());
        if (serviceInstance.getStatus().equals("PROPAGATED")) {
            serviceInstance.setStatus("PROPAGATED-PARTIAL");
        } else {
            serviceInstance.setStatus("COMMITTING-PARTIAL");
        }
        ServiceInstancePersistenceManager.merge(serviceInstance);
        logger.end(method, serviceInstance.getStatus());
        return reverseSvcDelta.getId();
    }

    //By default, this only checks the last service delta. If multiPropagate==true, check all deltas.
    public String checkStatus(String serviceInstanceUuid) {
        logger.cleanup();
        String method = "checkStatus";
        logger.refuuid(serviceInstanceUuid);
        logger.trace_start(method);
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceInstanceUuid);
        if (serviceInstance == null) {
            throw logger.error_throwing(method, "cannot find ref:ServiceInstance");
        }
        if (!serviceInstance.getStatus().equals(ServiceInstancePersistenceManager.findById(serviceInstance.getId()).getStatus())) {
            ServiceInstancePersistenceManager.merge(serviceInstance);
        }
        if (!serviceInstance.getStatus().equals("COMMITTING")) {
            logger.trace_end(method, serviceInstance.getStatus());
            return serviceInstance.getStatus();
        }
        Iterator<ServiceDelta> itSD = serviceInstance.getServiceDeltas().iterator();
        //@TODO? change (multiPropagate==true) into using another property?
        String multiPropagate = serviceInstance.getProperty("multiPropagate");
        boolean checkAllDeltas = false;
        if (multiPropagate != null && multiPropagate.equalsIgnoreCase("true")) {
            checkAllDeltas = true;
        }
        while (itSD.hasNext()) {
            ServiceDelta serviceDelta = itSD.next();
            if (!checkAllDeltas && itSD.hasNext()) {
                continue;
            }
            if (serviceDelta.getSystemDelta() == null) {
                throw logger.error_throwing(method, "ref:ServiceInstance encounters uncompiled ref:ServiceDelta (having null SystemDelta).");
            }
            if (serviceDelta.getStatus().equals("COMMITTED")) {
                continue;
            }
            if (serviceDelta.getStatus().equals("FAILED")) {
                continue;
            }
            if (!serviceDelta.getStatus().equals("COMMITTING")) {
                throw logger.error_throwing(method, "ref:ServiceInstance encounters uncompiled ref:ServiceDelta in unexpected status=" + serviceDelta.getStatus());
            }
            // for committing serviceDelta we check if the systemDelta has been committing
            SystemInstance systemInstance = SystemInstancePersistenceManager.findBySystemDelta(serviceDelta.getSystemDelta());
            if (systemInstance == null) {
                throw logger.error_throwing(method, "cannot find SystemInstance based on ref:ServiceDelta.");
            }
            // get cached systemInstance
            systemInstance = SystemInstancePersistenceManager.findByReferenceUUID(systemInstance.getReferenceUUID());
            if (systemInstance == null || systemInstance.getCommitStatus() == null) {
                throw logger.error_throwing(method, "ref:ServiceInstance encounters null " + (systemInstance == null ? " SystemInstance.": " asyncStatus cache." ) );
            }
            Future<String> asyncStatus = systemInstance.getCommitStatus();
            serviceDelta.setStatus("FAILED");
            if (asyncStatus.isDone()) {
                try {
                    String commitStatus = asyncStatus.get();
                    if (commitStatus.equals("SUCCESS")) {
                        serviceDelta.setStatus("COMMITTED");
                    }
                } catch (Exception ex) {
                    logger.error(method, "ref:ServiceInstance->SystemInstance commit done and asyncStatus.get() -exception- "+ex);
                    serviceDelta.setStatus("FAILED");
                }
            } else {
                serviceDelta.setStatus("COMMITTING");
            }
            DeltaPersistenceManager.merge(serviceDelta);
        }
        // collect status from systemDeltas (all_commited == ready)
        boolean failed = false;
        boolean ready = true;
        itSD = serviceInstance.getServiceDeltas().iterator();
        while (itSD.hasNext()) {
            ServiceDelta serviceDelta = itSD.next();
            if (!checkAllDeltas && itSD.hasNext()) {
                continue;
            }
            if (serviceDelta.getStatus().equals("FAILED")) {
                failed = true;
                break;
            } else if (serviceDelta.getStatus().equals("COMMITTING")) {
                ready = false;
            }
        }
        if (failed) {
            serviceInstance.setStatus("FAILED");
        } else if (ready) {
            serviceInstance.setStatus("COMMITTED");
        } else {
            serviceInstance.setStatus("COMMITTING");
        }
        ServiceInstancePersistenceManager.merge(serviceInstance);
        logger.trace_end(method, serviceInstance.getStatus());
        return serviceInstance.getStatus();
    }

    public void updateStatus(String serviceInstanceUuid, String status) {
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceInstanceUuid);
        serviceInstance.setStatus(status);
        ServiceInstancePersistenceManager.merge(serviceInstance);
    }
    
    // retry always companies FAILED status | forced => VG refresh requested
    public String propagateRetry(String serviceInstanceUuid, boolean useCachedVG, boolean refreshForced) {
        logger.cleanup();
        String method = "propagateRetry";
        logger.refuuid(serviceInstanceUuid);
        logger.start(method);
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceInstanceUuid);
        if (serviceInstance == null) {
            throw logger.error_throwing(method, "cannot find ref:ServiceInstance");
        }
        /* We allow 'retry' on any status ? For example, Create/READY with verification failure should be still good case for retry.
        if (!serviceInstance.getStatus().equals("FAILED")) {
            throw new EJBException(HandleServiceCall.class.getName() + ".propagateRetry requires FAILED status for serviceInstance with uuid=" + serviceInstanceUuid
            +" -- the acutal status: " + serviceInstance.getStatus());
        } 
        */
        logger.status(method, serviceInstance.getStatus());
        if (serviceInstance.getServiceDeltas() == null || serviceInstance.getServiceDeltas().isEmpty()) {
            throw logger.error_throwing(method, "ref:ServiceInstance has none delta to retry.");
        }
        Iterator<ServiceDelta> itSD = serviceInstance.getServiceDeltas().iterator();
        boolean canMultiPropagate = false;
        String multiPropagate = serviceInstance.getProperty("multiPropagate");
        if (multiPropagate != null && multiPropagate.equalsIgnoreCase("true")) {
            canMultiPropagate = true;
        }
        while (itSD.hasNext()) {
            ServiceDelta serviceDelta = itSD.next();
            if (serviceDelta.getSystemDelta() == null) {
                continue; // ??
            } else if (canMultiPropagate || !itSD.hasNext()) {
                SystemInstance systemInstance = SystemInstancePersistenceManager.findBySystemDelta(serviceDelta.getSystemDelta());
                if (systemInstance == null) {
                    systemInstance = systemCallHandler.createInstance();
                } else {
                    systemInstance = SystemInstancePersistenceManager.findById(systemInstance.getId());
                }
                if (systemInstance.getSystemDelta() != null
                        && systemInstance.getSystemDelta().getDriverSystemDeltas() != null
                        && !systemInstance.getSystemDelta().getDriverSystemDeltas().isEmpty()) {
                    //systemInstance.setSystemDelta((SystemDelta)DeltaPersistenceManager.merge(systemInstance.getSystemDelta()));
                    for (Iterator<DriverSystemDelta> dsdIt = systemInstance.getSystemDelta().getDriverSystemDeltas().iterator(); dsdIt.hasNext();) {
                        DriverSystemDelta dsd = dsdIt.next();
                        DriverInstance driverInstance = DriverInstancePersistenceManager.findByTopologyUri(dsd.getDriverInstance().getTopologyUri());
                        driverInstance = DriverInstancePersistenceManager.findById(driverInstance.getId());
                        driverInstance.getDriverSystemDeltas().remove(dsd);
                        if (serviceDelta.getSystemDelta() != null && serviceDelta.getSystemDelta().getDriverSystemDeltas() != null
                                && serviceDelta.getSystemDelta().getDriverSystemDeltas().contains(dsd)) {
                            driverInstance.getDriverSystemDeltas().remove(dsd);
                        }
                        // a hack. we really want to delete this DSD completely but have not found a way to make it go away.
                        dsd.setStatus("DELETED");
                        DeltaPersistenceManager.save(dsd);
                    }
                    systemInstance.getSystemDelta().getDriverSystemDeltas().clear();
                    DeltaPersistenceManager.save(systemInstance.getSystemDelta());
                }
                systemCallHandler.propagateDelta(systemInstance, serviceDelta.getSystemDelta(), useCachedVG, refreshForced);
                serviceDelta.setStatus("PROPAGATED");
                DeltaPersistenceManager.merge(serviceDelta);
                if (!canMultiPropagate) {
                    break;
                }
            } 
        }
        itSD = serviceInstance.getServiceDeltas().iterator();
        boolean hasInitiated = false;
        boolean hasPropagated = false;
        boolean isCommitting = false;
        while (itSD.hasNext()) {
            ServiceDelta serviceDelta = itSD.next();
            if (serviceDelta.getStatus().equalsIgnoreCase("INIT")) {
                hasInitiated = true;
            } else if (serviceDelta.getStatus().equalsIgnoreCase("PROPAGATED")) {
                hasPropagated = true;
            } else if (serviceDelta.getStatus().equalsIgnoreCase("COMMITTING")) {
                isCommitting = true;
            }
        }
        //serviceInstance.setStatus("COMPILED");
        if (hasInitiated && hasPropagated) {
            serviceInstance.setStatus("PROPAGATED-PARTIAL");
        } else if (hasPropagated && !isCommitting) {
            serviceInstance.setStatus("PROPAGATED");
        } else if (hasPropagated && isCommitting) {
            serviceInstance.setStatus("COMMITTING-PARTIAL");
        } else if (!hasPropagated && isCommitting) {
            serviceInstance.setStatus("COMMITTING");
        }
        ServiceInstancePersistenceManager.merge(serviceInstance);
        logger.end(method, serviceInstance.getStatus());
        return serviceInstance.getStatus();
    }
    
    public void verifyDelta(String svcUUID, ModelUtil.DeltaVerification apiData, boolean marshallWithJson) {
        logger.cleanup();
        String method = "verifyDelta";
        logger.refuuid(svcUUID);
        ServiceDelta serviceDelta;
        //try serviceDeltaUuid as a serviceInstanceUuid and look for the latest serviceDeltaUuid in this instance
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(svcUUID);
        if (serviceInstance != null && serviceInstance.getServiceDeltas() != null && !serviceInstance.getServiceDeltas().isEmpty()) {
            serviceDelta = serviceInstance.getServiceDeltas().get(serviceInstance.getServiceDeltas().size() - 1);
        } else {
            throw logger.error_throwing(method, "ref:ServiceInstance has no ServiceDelta to verify");
        }
        logger.targetid(serviceDelta.getId());
        logger.trace_start(method);
        if (serviceDelta.getSystemDelta() == null) {
            throw logger.error_throwing(method, "there is no SystemDelta associated with target:ServiceDelta");
        }
        OntModel refModel = this.fetchReferenceModel();
        if (refModel == null) {
            throw logger.error_throwing(method, "fetchReferenceModel() returned null - systemModelCoordinator not ready or contending on access lock");
        }
        Boolean ready = null;
        if (serviceDelta.getSystemDelta().getModelAddition() != null && serviceDelta.getSystemDelta().getModelAddition().getOntModel() != null) {
            Model modelAdditionVerified = ModelFactory.createDefaultModel();
            Model modelAdditionUnverified = ModelFactory.createDefaultModel();
            boolean additionVerified = this.verifyModelAddition(serviceDelta.getSystemDelta().getModelAddition().getOntModel().getBaseModel(), 
                    refModel.getBaseModel(), modelAdditionVerified, modelAdditionUnverified);
            apiData.setAdditionVerified(additionVerified);
            ready = additionVerified;
            try {
                if (marshallWithJson) {
                    apiData.setModelAdditionVerified(ModelUtil.marshalModelJson(modelAdditionVerified));
                    apiData.setModelAdditionUnverified(ModelUtil.marshalModelJson(modelAdditionUnverified));
                } else {
                    apiData.setModelAdditionVerified(ModelUtil.marshalModel(modelAdditionVerified));
                    apiData.setModelAdditionUnverified(ModelUtil.marshalModel(modelAdditionUnverified));
                }
            } catch (Exception ex) {
                throw logger.throwing(method, ex);
            }
        }
        if (serviceDelta.getSystemDelta().getModelReduction()!= null && serviceDelta.getSystemDelta().getModelReduction().getOntModel() != null) {
            Model modelReductionVerified = ModelFactory.createDefaultModel();
            Model modelReductionUnverified = ModelFactory.createDefaultModel();
            boolean reductionVerified = this.verifyModelReduction(serviceDelta.getSystemDelta().getModelReduction().getOntModel().getBaseModel(), 
                    refModel.getBaseModel(), modelReductionVerified, modelReductionUnverified);
            apiData.setReductionVerified(reductionVerified);
            if (ready == null) {
                ready = reductionVerified;
            } else {
                ready = (ready && reductionVerified);
            }
            try {
                if (marshallWithJson) {
                    apiData.setModelReductionVerified(ModelUtil.marshalModelJson(modelReductionVerified));
                    apiData.setModelReductionUnverified(ModelUtil.marshalModelJson(modelReductionUnverified));
                } else {
                    apiData.setModelReductionVerified(ModelUtil.marshalModel(modelReductionVerified));
                    apiData.setModelReductionUnverified(ModelUtil.marshalModel(modelReductionUnverified));
                }
            } catch (Exception ex) {
                throw logger.throwing(method, ex);
            }
        }
        if (ready) {
            serviceInstance.setStatus("READY");
            ServiceInstancePersistenceManager.merge(serviceInstance);
        }
        logger.trace_end(method);
    }
    
    public boolean verifyModelAddition(Model deltaModel, Model refModel, Model verifiedModel, Model unverifiedModel) {
        // residual (deltaModel - refModel)
        boolean allEssentialVerified = true;
        Model residualModel = ModelFactory.createDefaultModel();
        residualModel.add(deltaModel);
        residualModel.remove(refModel);
        // check essential statemtns in residual
        String sparql = "SELECT ?res WHERE {?s ?p ?res. "
                + "FILTER ( regex(str(?p), '#has|#provides') "
                + "     && (not exists {?s mrs:hasNetworkAddress ?res.}) "
                + "     && (not exists {?res mrs:type \"unverifiable\".}) "
                + ") }";
        ResultSet rs = ModelUtil.sparqlQuery(residualModel, sparql);
        if (rs.hasNext()) {
            allEssentialVerified = false;
            unverifiedModel.add(residualModel);
            while (rs.hasNext()) {
                String uri = rs.next().get("res").toString();
                logger.trace("verifyModelAddition", "cannot verify: " + uri);
            }
        }
        // add verified statements to verifiedModel
        verifiedModel.add(deltaModel);
        verifiedModel.remove(residualModel);
        // explore essential subtrees
        rs = ModelUtil.sparqlQuery(verifiedModel, sparql);
        List resList = new ArrayList<Resource>();
        while (rs.hasNext()) {
            QuerySolution querySolution = rs.next();
            Resource res = querySolution.getResource("res");
            resList.add(res);
        }
        if (!resList.isEmpty()) {
            List<String> includeMatches = new ArrayList<String>();
            List<String> excludeMatches = new ArrayList<String>();
            List<String> excludeExtentials = new ArrayList<String>();
            excludeMatches.add("#isAlias");
            excludeMatches.add("#belongsTo");
            excludeMatches.add("#providedBy");
            excludeExtentials.add("#nextHop");
            excludeExtentials.add("#routeFrom");
            excludeExtentials.add("#routeTo");
            Model extModel = ModelUtil.getModelSubTree(refModel, resList, includeMatches, excludeMatches, excludeExtentials);
            verifiedModel.add(extModel);
        }
        return allEssentialVerified;
    }
    
    public boolean verifyModelReduction(Model deltaModel, Model refModel, Model verifiedModel, Model unverifiedModel) {
        boolean allEssentialVerified = true;
        // manifest = (deltaModel - refModel), meaning statements no longer showing up
        verifiedModel.add(deltaModel);
        verifiedModel.remove(refModel);
        Model residualModel = ModelFactory.createDefaultModel();
        residualModel.add(deltaModel);
        residualModel.remove(verifiedModel);
        // first create a list of unverifiable resources 
        String sparql = "SELECT ?res WHERE {?s ?p ?res. "
                + "?res mrs:type \"unverifiable\".}";
        ResultSet rs = ModelUtil.sparqlQuery(deltaModel, sparql);
        List<Resource> unverifiableList = new ArrayList<>();
        while (rs.hasNext()) {
            unverifiableList.add(rs.next().getResource("res"));
        }
        // check if the essential satements in manifestModel are same as in deltaModel
        sparql = "SELECT ?res WHERE {?s ?p ?res. "
                + "FILTER( regex(str(?p), '#has|#provides')"
                + "     && (not exists {?s mrs:hasNetworkAddress ?res.}) "
                + "     && (not exists {?res mrs:type \"unverifiable\".}) "
                + ") }";
        rs = ModelUtil.sparqlQuery(residualModel, sparql);
        while (rs.hasNext()) {
            Resource res = rs.next().getResource("res");
            if (unverifiableList.contains(res)) {
                continue;
            }
            logger.trace("verifyModelReduction", "cannot verify: " + res.toString());
            if (allEssentialVerified) {
                unverifiedModel.add(residualModel);
                allEssentialVerified = false;
            }
        }
        return allEssentialVerified;
    }
    
    public void retrieveDelta(String svcUUID, ModelUtil.DeltaRetrieval apiData, boolean marshallWithJson) {
        logger.cleanup();
        String method = "retrieveDelta";
        logger.refuuid(svcUUID);
        ServiceDelta serviceDelta;
        //try serviceDeltaUuid as a serviceInstanceUuid and look for the latest serviceDeltaUuid in this instance
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(svcUUID);
        if (serviceInstance != null && serviceInstance.getServiceDeltas() != null && !serviceInstance.getServiceDeltas().isEmpty()) {
            serviceDelta = serviceInstance.getServiceDeltas().get(serviceInstance.getServiceDeltas().size() - 1);
        } else {
            throw logger.error_throwing(method, "ref:ServiceInstance has no ServiceDelta to retrieve");
        }
        logger.targetid(serviceDelta.getId());
        logger.trace_start(method);
        try {
            if (marshallWithJson) {
                if (serviceDelta.getModelAddition() != null && serviceDelta.getModelAddition().getOntModel() != null)
                   apiData.setModelAdditionSvc(ModelUtil.marshalModelJson(serviceDelta.getModelAddition().getOntModel()));
                if (serviceDelta.getModelReduction() != null && serviceDelta.getModelReduction().getOntModel() != null)
                    apiData.setModelReductionSvc(ModelUtil.marshalModelJson(serviceDelta.getModelReduction().getOntModel()));
            } else {
                if (serviceDelta.getModelAddition() != null && serviceDelta.getModelAddition().getOntModel() != null)
                    apiData.setModelAdditionSvc(ModelUtil.marshalModel(serviceDelta.getModelAddition().getOntModel()));
                if (serviceDelta.getModelReduction() != null && serviceDelta.getModelReduction().getOntModel() != null)
                    apiData.setModelReductionSvc(ModelUtil.marshalModel(serviceDelta.getModelReduction().getOntModel()));
            }
        } catch (Exception ex) {
            throw logger.throwing(method, ex);
        }
        if (serviceDelta.getSystemDelta() == null) {
            return;
        }
        try {
            if (marshallWithJson) {
                if (serviceDelta.getSystemDelta().getModelAddition() != null && serviceDelta.getSystemDelta().getModelAddition().getOntModel() != null)
                    apiData.setModelAdditionSys(ModelUtil.marshalModelJson(serviceDelta.getSystemDelta().getModelAddition().getOntModel()));
                if (serviceDelta.getSystemDelta().getModelReduction() != null && serviceDelta.getSystemDelta().getModelReduction().getOntModel() != null)
                    apiData.setModelReductionSys(ModelUtil.marshalModelJson(serviceDelta.getSystemDelta().getModelReduction().getOntModel()));
            } else {
                if (serviceDelta.getSystemDelta().getModelAddition() != null && serviceDelta.getSystemDelta().getModelAddition().getOntModel() != null)
                    apiData.setModelAdditionSys(ModelUtil.marshalModel(serviceDelta.getSystemDelta().getModelAddition().getOntModel()));
                if (serviceDelta.getSystemDelta().getModelReduction() != null && serviceDelta.getSystemDelta().getModelReduction().getOntModel() != null)
                    apiData.setModelReductionSys(ModelUtil.marshalModel(serviceDelta.getSystemDelta().getModelReduction().getOntModel()));
            }
            apiData.setReferenceModelUUID(serviceDelta.getSystemDelta().getReferenceVersionGroup().getRefUuid());
        } catch (Exception ex) {
            throw logger.throwing(method, ex);
        }
        logger.trace_end(method);
    }
    
    public boolean hasSystemBootStrapped() {
        DataConcurrencyPoster dataConcurrencyPoster;
        try {
            Context ejbCxt = new InitialContext();
            dataConcurrencyPoster = (DataConcurrencyPoster) ejbCxt.lookup("java:module/DataConcurrencyPoster");
        } catch (NamingException e) {
            throw logger.error_throwing("hasSystemBootStrapped", "failed to lookup DataConcurrencyPoster --" + e);
        }
        return dataConcurrencyPoster.isSystemModelCoordinator_bootStrapped();
    }

    public void resetSystemBootStrapped() {
        SystemModelCoordinator systemModelCoordinator = null;
        try {
            Context ejbCxt = new InitialContext();
            systemModelCoordinator = (SystemModelCoordinator) ejbCxt.lookup("java:module/SystemModelCoordinator");
        } catch (NamingException ex) {
            throw logger.error_throwing("resetSystemBootStrapped", " failed to inject systemModelCoordinator -exception- " + ex);
        }
        logger.message("resetSystemBootStrapped", "systemBootstraped=false");
        systemModelCoordinator.setBootStrapped(false);
    }
    
    private OntModel fetchReferenceModel() {
        String method = "fetchReferenceModel";
        SystemModelCoordinator systemModelCoordinator = null;
        try {
            Context ejbCxt = new InitialContext();
            systemModelCoordinator = (SystemModelCoordinator) ejbCxt.lookup("java:module/SystemModelCoordinator");
        } catch (NamingException ex) {
            throw logger.error_throwing(method, " failed to inject systemModelCoordinator -exception- " + ex);
        }
        OntModel refModel = null;
        try {
            refModel = systemModelCoordinator.getLatestOntModel();
        } catch (EJBException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("concurrent access timeout ")) {
                DataConcurrencyPoster dataConcurrencyPoster;
                try {
                    Context ejbCxt = new InitialContext();
                    dataConcurrencyPoster = (DataConcurrencyPoster) ejbCxt.lookup("java:module/DataConcurrencyPoster");
                } catch (NamingException e) {
                    throw logger.error_throwing(method, "failed to lookup DataConcurrencyPoster --" + e);
                }
                refModel = dataConcurrencyPoster.getSystemModelCoordinator_cachedOntModel();
            }
        }
        return refModel;
    }
}