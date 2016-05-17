/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
import net.maxgigapop.mrs.bean.persist.DeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.ServiceDeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.ServiceInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.SystemInstancePersistenceManager;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.Mrs;
import net.maxgigapop.mrs.core.SystemModelCoordinator;
import net.maxgigapop.mrs.service.orchestrate.WorkerBase;
import net.maxgigapop.mrs.system.HandleSystemCall;

/**
 *
 * @author xyang
 */
@Stateless
@LocalBean
public class HandleServiceCall {

    @EJB
    HandleSystemCall systemCallHandler;

    public ServiceInstance createInstance() {
        ServiceInstance serviceInstance = new ServiceInstance();
        serviceInstance.setReferenceUUID(UUID.randomUUID().toString());
        ServiceInstancePersistenceManager.save(serviceInstance);
        return serviceInstance;
    }

    public void terminateInstance(String refUUID) {
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(refUUID);
        serviceInstance = ServiceInstancePersistenceManager.findById(serviceInstance.getId());
        if (serviceInstance == null) {
            throw new EJBException(String.format("terminateInstance cannot find the ServiceInstance with referenceUUID=%s", refUUID));
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
    }

    public void setInstanceProperty(String refUUID, String property, String value) {
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(refUUID);
        if (serviceInstance == null) {
            throw new EJBException(String.format("setInstanceProperty cannot find the ServiceInstance with referenceUUID=%s", refUUID));
        }
        serviceInstance.getProperties().put(property, value);
        ServiceInstancePersistenceManager.merge(serviceInstance);
    }

    public String getInstanceProperty(String refUUID, String property) {
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(refUUID);
        if (serviceInstance == null) {
            throw new EJBException(String.format("getInstanceProperty cannot find the ServiceInstance with referenceUUID=%s", refUUID));
        }
        return serviceInstance.getProperty(property);
    }


    public Map listInstanceProperties(String refUUID) {
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(refUUID);
        if (serviceInstance == null) {
            throw new EJBException(String.format("listInstanceProperty cannot find the ServiceInstance with referenceUUID=%s", refUUID));
        }
        return serviceInstance.getProperties();
    }
    
    public SystemDelta compileAddDelta(String serviceInstanceUuid, String workerClassPath, ServiceDelta spaDelta) {
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceInstanceUuid);
        if (serviceInstance == null) {
            throw new EJBException(HandleServiceCall.class.getName() + ".compileDelta cannot find serviceInstance with uuid=" + serviceInstanceUuid);
        }
        if (ServiceDeltaPersistenceManager.findByReferenceUUID(spaDelta.getReferenceUUID()) != null) {
            throw new EJBException(HandleServiceCall.class.getName() + ".compileDelta has already received a spaDelta with same uuid=" + spaDelta.getReferenceUUID());
        }
        // run with chosen worker
        WorkerBase worker = null;
        try {
            worker = (WorkerBase) this.getClass().getClassLoader().loadClass(workerClassPath).newInstance();
        } catch (Exception ex) {
            Logger.getLogger(HandleServiceCall.class.getName()).log(Level.SEVERE, null, ex);
            throw new EJBException(ex);
        }
        worker.setAnnoatedModel(spaDelta);
        try {
            worker.run();
        } catch (EJBException ex) {
            serviceInstance.setStatus("FAILED");
            ServiceInstancePersistenceManager.merge(serviceInstance);
            throw ex;
        }
        // save serviceInstance, spaDelta and systemDelta
        SystemDelta resultDelta = worker.getResultModelDelta();
        resultDelta.setServiceDelta(spaDelta);
        //DeltaPersistenceManager.save(resultDelta);
        spaDelta.setSystemDelta(resultDelta);
        if (serviceInstance.getServiceDeltas() == null) {
            serviceInstance.setServiceDeltas(new ArrayList<ServiceDelta>());
        }
        serviceInstance.getServiceDeltas().add(spaDelta);
        spaDelta.setServiceInstance(serviceInstance);
        DeltaPersistenceManager.save(spaDelta);
        //serviceInstance = ServiceInstancePersistenceManager.findById(serviceInstance.getId());
        serviceInstance.setStatus("COMPILED");
        ServiceInstancePersistenceManager.merge(serviceInstance);
        return resultDelta;
    }

    public SystemDelta compileAddDelta(String serviceInstanceUuid, String workerClassPath, String uuid, String modelAdditionTtl, String modelReductionTtl) {
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

            DeltaModel dmReduction = null;
            if (modelReductionTtl != null) {
                OntModel modelReduction = ModelUtil.unmarshalOntModel(modelReductionTtl);
                dmReduction = new DeltaModel();
                dmReduction.setCommitted(false);
                dmReduction.setDelta(spaDelta);
                dmReduction.setIsAddition(false);
                dmReduction.setOntModel(modelReduction);
            }
            spaDelta.setModelAddition(dmAddition);
            spaDelta.setModelReduction(dmReduction);
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
        return compileAddDelta(serviceInstanceUuid, workerClassPath, spaDelta);
    }

    // handling multiple deltas:  propagate + commit + query = transactional propagate + parallel commits
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String propagateDeltas(String serviceInstanceUuid, boolean useUpdatedRefModel) {
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceInstanceUuid);
        if (serviceInstance == null) {
            throw new EJBException(HandleServiceCall.class.getName() + ".propogateDeltas cannot find serviceInstance with uuid=" + serviceInstanceUuid);
        }
        if (!serviceInstance.getStatus().equals("COMPILED") && !serviceInstance.getStatus().equals("PROPAGATED-PARTIAL") && !serviceInstance.getStatus().equals("COMMITTED-PARTIAL")) {
            throw new EJBException(HandleServiceCall.class.getName() + ".propogateDeltas needs  status='INIT or PROPAGATED-PARTIAL or COMMITTED-PARTIAL' by " + serviceInstance + ", the actual status=" + serviceInstance.getStatus());
        }
        Iterator<ServiceDelta> itSD = serviceInstance.getServiceDeltas().iterator();
        if (!itSD.hasNext()) {
            throw new EJBException(HandleServiceCall.class.getName() + ".propogateDeltas (by " + serviceInstance + ",  in status=" + serviceInstance.getStatus() + ") has none delta to propagate.");
        }
        // By default propagate a delta if it is the first with init status in queue or there is only commited ones before.
        // Commit only one delta at a time.
        boolean canMultiPropagate = false;
        String multiPropagate = serviceInstance.getProperty("multiPropagate");
        if (multiPropagate != null && multiPropagate.equalsIgnoreCase("true")) {
            canMultiPropagate = true;
        }
        while (itSD.hasNext()) {
            ServiceDelta serviceDelta = itSD.next();
            if (serviceDelta.getSystemDelta() == null) {
                continue; // ??
            } else if (serviceDelta.getStatus().equals("INIT")) {
                SystemInstance systemInstance = systemCallHandler.createInstance();
                try {
                    systemCallHandler.propagateDelta(systemInstance, serviceDelta.getSystemDelta(), useUpdatedRefModel);
                } catch (EJBException ejbEx) {
                    //serviceInstance.setStatus("FAILED");
                    //ServiceInstancePersistenceManager.merge(serviceInstance);
                    throw ejbEx;
                }
                serviceDelta.setStatus("PROPAGATED");
                DeltaPersistenceManager.merge(serviceDelta);
                if (!canMultiPropagate) {
                    break;
                }
            } else if (!canMultiPropagate && !serviceDelta.getStatus().equals("COMMITTED") && !serviceDelta.getStatus().equals("READY")) {
                throw new EJBException(HandleServiceCall.class.getName() + ".propogateDeltas (by " + serviceInstance + ") with 'multiPropagate=false' encounters " + serviceDelta + " in status=" + serviceDelta.getStatus());
            }
        }
        itSD = serviceInstance.getServiceDeltas().iterator();
        boolean hasInitiated = false;
        boolean hasPropagated = false;
        boolean hasCommited = false;
        while (itSD.hasNext()) {
            ServiceDelta serviceDelta = itSD.next();
            if (serviceDelta.getStatus().equalsIgnoreCase("INIT")) {
                hasInitiated = true;
            } else if (serviceDelta.getStatus().equalsIgnoreCase("PROPAGATED")) {
                hasPropagated = true;
            } else if (serviceDelta.getStatus().equalsIgnoreCase("COMMITTED")) {
                hasCommited = true;
            }
        }
        //serviceInstance.setStatus("COMPILED");
        if (hasInitiated && hasPropagated) {
            serviceInstance.setStatus("PROPAGATED-PARTIAL");
        } else if (hasPropagated && !hasCommited) {
            serviceInstance.setStatus("PROPAGATED");
        } else if (hasPropagated && hasCommited) {
            serviceInstance.setStatus("COMMITTED-PARTIAL");
        } else if (!hasPropagated && hasCommited) {
            serviceInstance.setStatus("COMMITTED");
        }
        ServiceInstancePersistenceManager.merge(serviceInstance);
        return serviceInstance.getStatus();
    }

    public String commitDeltas(String serviceInstanceUuid, boolean forced) {
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceInstanceUuid);
        if (serviceInstance == null) {
            throw new EJBException(HandleServiceCall.class.getName() + ".commitDeltas cannot find serviceInstance with uuid=" + serviceInstanceUuid);
        }
        if (!serviceInstance.getStatus().equals("PROPAGATED")
                && !serviceInstance.getStatus().equals("PROPAGATED-PARTIAL")
                && !serviceInstance.getStatus().equals("COMMITTED-PARTIAL")) {
            throw new EJBException(HandleServiceCall.class.getName() + ".commitDeltas needs  status='PROPAGATED or PROPAGATED-PARTIAL or COMMITTED-PARTIAL' by " + serviceInstance + ", the actual status=" + serviceInstance.getStatus());
        }
        Iterator<ServiceDelta> itSD = serviceInstance.getServiceDeltas().iterator();
        if (!itSD.hasNext()) {
            throw new EJBException(HandleServiceCall.class.getName() + ".commitDeltas (by " + serviceInstance + ",  in status=" + serviceInstance.getStatus() + ") has none delta to commit.");
        }
        // By default commit a delta if it is the first with propagated status in queue or there is only commited ones before.
        // Also commit only one delta at a time.
        boolean canMultiCommit = false;
        String multiCommit = serviceInstance.getProperty("multiCommit");
        if (multiCommit != null && multiCommit.equalsIgnoreCase("true")) {
            canMultiCommit = true;
        }
        while (itSD.hasNext()) {
            ServiceDelta serviceDelta = itSD.next();
            if (serviceDelta.getSystemDelta() == null) {
                continue; // ?? exception ??
            }
            if (serviceDelta.getStatus().equals("PROPAGATED")) {
                SystemInstance systemInstance = SystemInstancePersistenceManager.findBySystemDelta(serviceDelta.getSystemDelta());
                if (systemInstance.getSystemDelta().getDriverSystemDeltas() == null || systemInstance.getSystemDelta().getDriverSystemDeltas().isEmpty()) {
                    throw new EJBException(HandleServiceCall.class.getName() + ".commitDeltas (by " + serviceInstance + ") has nothing to change (empty driver delta list).");
                }
                systemInstance = SystemInstancePersistenceManager.findByReferenceUUID(systemInstance.getReferenceUUID());
                Future<String> asynResult = systemCallHandler.commitDelta(systemInstance);
                systemInstance.setCommitStatus(asynResult);
                //systemInstance.setCommitFlag(true);
                serviceDelta.setStatus("COMMITTED");
                DeltaPersistenceManager.merge(serviceDelta);
                if (!canMultiCommit) {
                    break;
                }
            } else if (!forced && !canMultiCommit && !serviceDelta.getStatus().equals("COMMITTED") && !serviceDelta.getStatus().equals("READY")) {
                throw new EJBException(HandleServiceCall.class.getName() + ".commitDeltas (by " + serviceInstance + ") encounters " + serviceDelta + " in status=" + serviceDelta.getStatus());
            }
        }
        itSD = serviceInstance.getServiceDeltas().iterator();
        boolean hasInitiated = false;
        boolean hasPropagated = false;
        boolean hasCommited = false;
        while (itSD.hasNext()) {
            ServiceDelta serviceDelta = itSD.next();
            if (serviceDelta.getStatus().equalsIgnoreCase("INIT")) {
                hasInitiated = true;
            } else if (serviceDelta.getStatus().equalsIgnoreCase("PROPAGATED")) {
                hasPropagated = true;
            } else if (serviceDelta.getStatus().equalsIgnoreCase("COMMITTED")) {
                hasCommited = true;
            }
        }
        //serviceInstance.setStatus("INIT");
        if (hasInitiated && hasPropagated) {
            serviceInstance.setStatus("PROPAGATED-PARTIAL");
        } else if (hasPropagated && !hasCommited) {
            serviceInstance.setStatus("PROPAGATED");
        } else if (hasPropagated && hasCommited) {
            serviceInstance.setStatus("COMMITTED-PARTIAL");
        } else if (!hasPropagated && hasCommited) {
            serviceInstance.setStatus("COMMITTED");
        }
        ServiceInstancePersistenceManager.merge(serviceInstance);
        return serviceInstance.getStatus();
    }

    public String revertDeltas(String serviceInstanceUuid, boolean forced) {
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceInstanceUuid);
        if (serviceInstance == null) {
            throw new EJBException(HandleServiceCall.class.getName() + ".revertDeltas cannot find serviceInstance with uuid=" + serviceInstanceUuid);
        }
        if (!forced && !serviceInstance.getStatus().startsWith("PROPAGATED")
                && !serviceInstance.getStatus().startsWith("COMMITTED")
                && !serviceInstance.getStatus().equals("READY")) {
            throw new EJBException(HandleServiceCall.class.getName() + ".revertDeltas needs  status='PROPAGATED' or 'COMMITTED' or 'READY' by " + serviceInstance + ", the actual status=" + serviceInstance.getStatus());
        }
        Iterator<ServiceDelta> itSD = serviceInstance.getServiceDeltas().iterator();
        List<ServiceDelta> reversableServiceDeltas = new ArrayList<>();
        while (itSD.hasNext()) {
            ServiceDelta svcDelta = itSD.next();
            if (svcDelta.getStatus().equalsIgnoreCase("READY") || svcDelta.getStatus().equalsIgnoreCase("COMMITTED")
                    || (forced && svcDelta.getStatus().equalsIgnoreCase("FAILED"))) {
                reversableServiceDeltas.add(svcDelta);
            }
        }
        itSD = reversableServiceDeltas.iterator();
        if (!itSD.hasNext()) {
            throw new EJBException(HandleServiceCall.class.getName() + ".revertDeltas (by " + serviceInstance + ",  in status=" + serviceInstance.getStatus() + ") has none delta to commit.");
        }
        ServiceDelta reverseSvcDelta = new ServiceDelta();
        reverseSvcDelta.setServiceInstance(serviceInstance);
        reverseSvcDelta.setReferenceUUID(UUID.randomUUID().toString());
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
                throw new EJBException(HandleServiceCall.class.getName() + ".revertDeltas encounters uncompiled (null systemDelta)  " + serviceDelta);
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
            excludeExtentials.add("#nextHop");
            excludeExtentials.add("#routeFrom");
            excludeExtentials.add("#routeTo");
            //@TODO: exclude essential Resource trees under routeTo / routeFrom / nextHop ==> excludeEssentials (include the statement but not go further)
            String sparql = "SELECT ?res WHERE {?s ?p ?res. "
                    + "FILTER(regex(str(?p), '#has|#provides'))"
                    + "}";
            OntModel refModel = this.fetchReferenceModel();
            if (refModel == null) {
                throw new EJBException(this.getClass().getName() + " systemModelCoordinator not ready or contending on access lock.");
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
        serviceInstance.getServiceDeltas().add(reverseSvcDelta);
        DeltaPersistenceManager.save(reverseSvcDelta);
        //serviceInstance = ServiceInstancePersistenceManager.findById(serviceInstance.getId());
        if (serviceInstance.getStatus().equals("PROPAGATED")) {
            serviceInstance.setStatus("PROPAGATED-PARTIAL");
        } else {
            serviceInstance.setStatus("COMMITTED-PARTIAL");
        }
        ServiceInstancePersistenceManager.merge(serviceInstance);
        return reverseSvcDelta.getReferenceUUID();
    }

    public String checkStatus(String serviceInstanceUuid) {
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceInstanceUuid);
        if (serviceInstance == null) {
            throw new EJBException(HandleServiceCall.class.getName() + ".checkStatus cannot find serviceInstance with uuid=" + serviceInstanceUuid);
        }
        if (!serviceInstance.getStatus().equals(ServiceInstancePersistenceManager.findById(serviceInstance.getId()).getStatus())) {
            ServiceInstancePersistenceManager.merge(serviceInstance);
        }
        if (!serviceInstance.getStatus().equals("COMMITTED")) {
            return serviceInstance.getStatus();
        }
        Iterator<ServiceDelta> itSD = serviceInstance.getServiceDeltas().iterator();
        while (itSD.hasNext()) {
            ServiceDelta serviceDelta = itSD.next();
            if (serviceDelta.getSystemDelta() == null) {
                throw new EJBException(HandleServiceCall.class.getName() + ".checkStatus (by " + serviceInstance + ") encounters " + serviceDelta + " without compiled systemDelta.");
            }
            if (serviceDelta.getStatus().equals("READY")) {
                continue;
            }
            if (!serviceDelta.getStatus().equals("COMMITTED")) {
                throw new EJBException(HandleServiceCall.class.getName() + ".checkStatus (by " + serviceInstance + ") encounters " + serviceDelta + " in status=" + serviceDelta.getStatus());
            }
            // for commited serviceDelta we check if the systemDelta has been commited
            SystemInstance systemInstance = SystemInstancePersistenceManager.findBySystemDelta(serviceDelta.getSystemDelta());
            if (systemInstance == null) {
                throw new EJBException(HandleServiceCall.class.getName() + ".checkStatus cannot find systemInstance based on " + serviceDelta.getSystemDelta());
            }
            // get cached systemInstance
            systemInstance = SystemInstancePersistenceManager.findByReferenceUUID(systemInstance.getReferenceUUID());

            Future<String> asyncStatus = systemInstance.getCommitStatus();
            serviceDelta.setStatus("FAILED");
            if (asyncStatus.isDone()) {
                try {
                    String commitStatus = asyncStatus.get();
                    if (commitStatus.equals("SUCCESS")) {
                        serviceDelta.setStatus("READY");
                    }
                } catch (Exception ex) {
                    //@TODO: add exception into ErrorReport
                    serviceDelta.setStatus("FAILED");
                }
            } else {
                serviceDelta.setStatus("COMMITTED");
            }
            DeltaPersistenceManager.merge(serviceDelta);
        }
        // collect status from systemDeltas (all_commited == ready)
        boolean failed = false;
        boolean ready = true;
        itSD = serviceInstance.getServiceDeltas().iterator();
        while (itSD.hasNext()) {
            ServiceDelta serviceDelta = itSD.next();
            if (serviceDelta.getStatus().equals("FAILED")) {
                failed = true;
                break;
            } else if (serviceDelta.getStatus().equals("COMMITTED")) {
                ready = false;
            }
        }
        if (failed) {
            serviceInstance.setStatus("FAILED");
        } else if (ready) {
            serviceInstance.setStatus("READY");
        } else {
            serviceInstance.setStatus("COMMITTED");
        }
        ServiceInstancePersistenceManager.merge(serviceInstance);
        return serviceInstance.getStatus();
    }

    public void updateStatus(String serviceInstanceUuid, String status) {
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceInstanceUuid);
        serviceInstance.setStatus(status);
        ServiceInstancePersistenceManager.merge(serviceInstance);
    }
    
    public String propagateRetry(String serviceInstanceUuid, boolean forced) {
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceInstanceUuid);
        if (serviceInstance == null) {
            throw new EJBException(HandleServiceCall.class.getName() + ".propagateRetry cannot find serviceInstance with uuid=" + serviceInstanceUuid);
        }
        if (!forced && !serviceInstance.getStatus().equals("FAILED")) {
            throw new EJBException(HandleServiceCall.class.getName() + ".propagateRetry (forced==false) cannot requires FAILED status for serviceInstance with uuid=" + serviceInstanceUuid
            +" instead of the acutal status: " + serviceInstance.getStatus());
        } 
        Iterator<ServiceDelta> itSD = serviceInstance.getServiceDeltas().iterator();
        if (!itSD.hasNext()) {
            throw new EJBException(HandleServiceCall.class.getName() + ".propagateRetry (by " + serviceInstance + ",  in status=" + serviceInstance.getStatus() + ") has none delta to retry.");
        }
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
                try {
                    systemCallHandler.propagateDelta(systemInstance, serviceDelta.getSystemDelta(), true);
                } catch (EJBException ejbEx) {
                    //serviceInstance.setStatus("FAILED");
                    //ServiceInstancePersistenceManager.merge(serviceInstance);
                    throw ejbEx;
                }
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
        boolean hasCommited = false;
        while (itSD.hasNext()) {
            ServiceDelta serviceDelta = itSD.next();
            if (serviceDelta.getStatus().equalsIgnoreCase("INIT")) {
                hasInitiated = true;
            } else if (serviceDelta.getStatus().equalsIgnoreCase("PROPAGATED")) {
                hasPropagated = true;
            } else if (serviceDelta.getStatus().equalsIgnoreCase("COMMITTED")) {
                hasCommited = true;
            }
        }
        //serviceInstance.setStatus("COMPILED");
        if (hasInitiated && hasPropagated) {
            serviceInstance.setStatus("PROPAGATED-PARTIAL");
        } else if (hasPropagated && !hasCommited) {
            serviceInstance.setStatus("PROPAGATED");
        } else if (hasPropagated && hasCommited) {
            serviceInstance.setStatus("COMMITTED-PARTIAL");
        } else if (!hasPropagated && hasCommited) {
            serviceInstance.setStatus("COMMITTED");
        }
        ServiceInstancePersistenceManager.merge(serviceInstance);
        return serviceInstance.getStatus();
    }
    
    public void verifyDelta(String serviceDeltaUuid, ModelUtil.DeltaVerification apiData, boolean marshallWithJson) {
        ServiceDelta serviceDelta = ServiceDeltaPersistenceManager.findByReferenceUUID(serviceDeltaUuid);
        if (serviceDelta == null) {
            //try serviceDeltaUuid as a serviceInstanceUuid and look for the latest serviceDeltaUuid in this instance
            ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceDeltaUuid);
            if (serviceInstance != null && !serviceInstance.getServiceDeltas().isEmpty()) {
                serviceDelta = serviceInstance.getServiceDeltas().get(serviceInstance.getServiceDeltas().size()-1);
            }
        }
        if (serviceDelta == null || serviceDelta.getSystemDelta() == null) {
            throw new EJBException(this.getClass().getName() + " verifyDelta does not know serviceDelta: " + serviceDeltaUuid);
        }
        OntModel refModel = this.fetchReferenceModel();
        if (refModel == null) {
            throw new EJBException(this.getClass().getName() + " verifyDelta cannot obtain reference model from SystemModelCoordinator.");
        }
        
        if (serviceDelta.getSystemDelta().getModelAddition() != null && serviceDelta.getSystemDelta().getModelAddition().getOntModel() != null) {
            Model modelAdditionVerified = ModelFactory.createDefaultModel();
            Model modelAdditionUnverified = ModelFactory.createDefaultModel();
            boolean additionVerified = this.verifyModelAddition(serviceDelta.getSystemDelta().getModelAddition().getOntModel().getBaseModel(), 
                    refModel.getBaseModel(), modelAdditionVerified, modelAdditionUnverified);
            apiData.setAdditionVerified(additionVerified);
            try {
                if (marshallWithJson) {
                    apiData.setModelAdditionVerified(ModelUtil.marshalModelJson(modelAdditionVerified));
                    apiData.setModelAdditionUnverified(ModelUtil.marshalModelJson(modelAdditionUnverified));
                } else {
                    apiData.setModelAdditionVerified(ModelUtil.marshalModel(modelAdditionVerified));
                    apiData.setModelAdditionUnverified(ModelUtil.marshalModel(modelAdditionUnverified));
                }
            } catch (Exception ex) {
                throw new EJBException(ex);
            }
        }
        if (serviceDelta.getSystemDelta().getModelReduction()!= null && serviceDelta.getSystemDelta().getModelReduction().getOntModel() != null) {
            Model modelReductionVerified = ModelFactory.createDefaultModel();
            Model modelReductionUnverified = ModelFactory.createDefaultModel();
            boolean reductionVerified = this.verifyModelReduction(serviceDelta.getSystemDelta().getModelReduction().getOntModel().getBaseModel(), 
                    refModel.getBaseModel(), modelReductionVerified, modelReductionUnverified);
            apiData.setReductionVerified(reductionVerified);
            try {
                if (marshallWithJson) {
                    apiData.setModelReductionVerified(ModelUtil.marshalModelJson(modelReductionVerified));
                    apiData.setModelReductionUnverified(ModelUtil.marshalModelJson(modelReductionUnverified));
                } else {
                    apiData.setModelReductionVerified(ModelUtil.marshalModel(modelReductionVerified));
                    apiData.setModelReductionUnverified(ModelUtil.marshalModel(modelReductionUnverified));
                }
            } catch (Exception ex) {
                throw new EJBException(ex);
            }
        }
    }
    
    public boolean verifyModelAddition(Model deltaModel, Model refModel, Model verifiedModel, Model unverifiedModel) {
        if (refModel == null) {
            refModel = this.fetchReferenceModel();
        }
        if (refModel == null) {
            throw new EJBException(this.getClass().getName() + " verifyModelAddition cannot obtain reference model from SystemModelCoordinator.");
        }
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
        if (refModel == null) {
            refModel = this.fetchReferenceModel();
        }
        if (refModel == null) {
            throw new EJBException(this.getClass().getName() + " verifyModelAddition cannot obtain reference model from SystemModelCoordinator.");
        }
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
            allEssentialVerified = false;
            unverifiedModel.add(residualModel);
            break;
        }
        return allEssentialVerified;
    }
    
    public boolean hasSystemBootStrapped() {
        SystemModelCoordinator systemModelCoordinator = null;
        try {
            Context ejbCxt = new InitialContext();
            systemModelCoordinator = (SystemModelCoordinator) ejbCxt.lookup("java:module/SystemModelCoordinator");
        } catch (NamingException ex) {
            throw new EJBException(this.getClass().getName() + " failed to inject systemModelCoordinator", ex);
        }
        return systemModelCoordinator.isBootStrapped();
    }
    
    public void resetSystemBootStrapped() {
        SystemModelCoordinator systemModelCoordinator = null;
        try {
            Context ejbCxt = new InitialContext();
            systemModelCoordinator = (SystemModelCoordinator) ejbCxt.lookup("java:module/SystemModelCoordinator");
        } catch (NamingException ex) {
            throw new EJBException(this.getClass().getName() + " failed to inject systemModelCoordinator", ex);
        }
        systemModelCoordinator.setBootStrapped(false);
    }
    
    private OntModel fetchReferenceModel() {
        SystemModelCoordinator systemModelCoordinator = null;
        try {
            Context ejbCxt = new InitialContext();
            systemModelCoordinator = (SystemModelCoordinator) ejbCxt.lookup("java:module/SystemModelCoordinator");
        } catch (NamingException ex) {
            throw new EJBException(this.getClass().getName() + " failed to inject systemModelCoordinator", ex);
        }
        OntModel refModel = null;
        for (int retry = 0; retry < 10; retry++) {
            try {
                refModel = systemModelCoordinator.getLatestOntModel();
                break;
            } catch (EJBException ex) {
                if (ex.getMessage().contains("concurrent access timeout ")) {
                    try {
                        sleep(10000L);
                    } catch (InterruptedException ex1) {
                        ;
                    }
                } else {
                    throw ex;
                }
            }
        }
        return refModel;
    }
}
