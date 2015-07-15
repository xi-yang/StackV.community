/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service;

import com.hp.hpl.jena.ontology.OntModel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.DeltaModel;
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.bean.ServiceInstance;
import net.maxgigapop.mrs.bean.SystemDelta;
import net.maxgigapop.mrs.bean.SystemInstance;
import net.maxgigapop.mrs.bean.persist.DeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.PersistenceManager;
import net.maxgigapop.mrs.bean.persist.ServiceDeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.ServiceInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.SystemInstancePersistenceManager;
import net.maxgigapop.mrs.common.ModelUtil;
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
        if (serviceInstance == null) {
            throw new EJBException(String.format("terminateInstance cannot find the ServiceInstance with referenceUUID=%s", refUUID));
        }
        if (serviceInstance.getServiceDeltas()!= null) {
            // clean up serviceDeltas ?
        }
        ServiceInstancePersistenceManager.delete(serviceInstance);
    }

    public SystemDelta compileDelta(String serviceInstanceUuid, String workerClassPath, ServiceDelta spaDelta) {
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceInstanceUuid);
        if (serviceInstance == null) {
            throw new EJBException(HandleServiceCall.class.getName()+".compileDelta cannot find serviceInstance with uuid=" + serviceInstanceUuid);
        }
        if (ServiceDeltaPersistenceManager.findByReferenceUUID(spaDelta.getReferenceUUID()) != null) {
            throw new EJBException(HandleServiceCall.class.getName()+".compileDelta has already received a spaDelta with same uuid=" + spaDelta.getReferenceUUID());            
        }
        // run with chosen worker
        WorkerBase worker = null;
        try {
            worker = (WorkerBase)this.getClass().getClassLoader().loadClass(workerClassPath).newInstance();
        } catch (Exception ex) {
            Logger.getLogger(HandleServiceCall.class.getName()).log(Level.SEVERE, null, ex);
            throw new EJBException(ex);
        } 
        worker.setAnnoatedModel(spaDelta);
        worker.run();
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
        serviceInstance = ServiceInstancePersistenceManager.findById(serviceInstance.getId());
        ServiceInstancePersistenceManager.save(serviceInstance);
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
        return compileDelta(serviceInstanceUuid, workerClassPath, spaDelta);
    }
    
    // handling multiple deltas:  propagate + commit + query = transactional propagate + parallel commits
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String pushSyncDeltas (String serviceInstanceUuid) {
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceInstanceUuid);
        if (serviceInstance == null) {
            throw new EJBException(HandleServiceCall.class.getName()+".propogateDeltas cannot find serviceInstance with uuid=" + serviceInstanceUuid);
        }
        boolean allReady = true;
        Iterator<ServiceDelta> itSD = serviceInstance.getServiceDeltas().iterator();
        while (itSD.hasNext()) {
            ServiceDelta serviceDelta = itSD.next();
            if (serviceDelta.getSystemDelta() == null)
                continue;
            else if (serviceDelta.getStatus().equals("INIT")) {
                allReady = false;
                SystemInstance systemInstance = systemCallHandler.createInstance();
                serviceInstance.setStatus("FAILED");
                systemCallHandler.propagateDelta(systemInstance, serviceDelta.getSystemDelta());
                serviceDelta.setStatus("PROPOGATED");
                serviceInstance.setStatus("PROCESSING");
                DeltaPersistenceManager.save(serviceDelta);
            } else if (serviceDelta.getStatus().equals("PROPOGATED")) {
                allReady = false;
                SystemInstance systemInstance = SystemInstancePersistenceManager.findBySystemDelta(serviceDelta.getSystemDelta());
                systemCallHandler.commitDelta(systemInstance);
                serviceDelta.setStatus("COMMITTED");
                serviceInstance.setStatus("PROCESSING");
                DeltaPersistenceManager.save(serviceDelta);
            } else if (serviceDelta.getStatus().equals("COMMITTED")) {
                SystemInstance systemInstance = SystemInstancePersistenceManager.findBySystemDelta(serviceDelta.getSystemDelta());
                // get cached systemInstance
                systemInstance = SystemInstancePersistenceManager.findByReferenceUUID(systemInstance.getReferenceUUID());
                Future<String> asyncStatus = systemInstance.getCommitStatus();
                serviceDelta.setStatus("FAILED");
                serviceInstance.setStatus("FAILED");
                if (asyncStatus.isDone()) {
                    try {
                        String commitStatus = asyncStatus.get();
                    } catch (InterruptedException ex) {
                        throw new EJBException(ex);
                    } catch (ExecutionException ex) {
                        throw new EJBException(ex);
                    }
                }
                serviceDelta.setStatus("READY");
                serviceInstance.setStatus("PROCESSING");
                DeltaPersistenceManager.save(serviceDelta);
            }
        }
        if (allReady == true) {
            serviceInstance.setStatus("READY");
        }
        ServiceInstancePersistenceManager.save(serviceInstance);     
        return serviceInstance.getStatus();
    }
}
