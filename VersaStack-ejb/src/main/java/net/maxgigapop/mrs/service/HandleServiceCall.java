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

    public SystemDelta compileAddDelta(String serviceInstanceUuid, String workerClassPath, ServiceDelta spaDelta) {
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
        serviceInstance.setStatus("INIT");
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
    public String propagateDeltas (String serviceInstanceUuid) {
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceInstanceUuid);
        if (serviceInstance == null) {
            throw new EJBException(HandleServiceCall.class.getName()+".propogateDeltas cannot find serviceInstance with uuid=" + serviceInstanceUuid);
        }
        if (!serviceInstance.getStatus().equals("INIT")) {
            throw new EJBException(HandleServiceCall.class.getName()+".propogateDeltas needs  status='INIT' by " + serviceInstance + ", the actual status=" + serviceInstance.getStatus());
        }
        Iterator<ServiceDelta> itSD = serviceInstance.getServiceDeltas().iterator();
        if (itSD.hasNext()) { // assume failed before proven success
            serviceInstance.setStatus("FAILED"); 
        }
        while (itSD.hasNext()) {
            ServiceDelta serviceDelta = itSD.next();
            if (serviceDelta.getSystemDelta() == null) {
                continue;
            } else if (serviceDelta.getStatus().equals("INIT")) {
                SystemInstance systemInstance = systemCallHandler.createInstance();
                systemCallHandler.propagateDelta(systemInstance, serviceDelta.getSystemDelta());
                serviceDelta.setStatus("PROPOGATED");
                DeltaPersistenceManager.merge(serviceDelta);
            } else if (!serviceDelta.getStatus().equals("PROPOGATED")) {
                throw new EJBException(HandleServiceCall.class.getName()+".propogateDeltas (by " + serviceInstance + ") encounters "+serviceDelta +" in status=" + serviceDelta.getStatus());
            }
        }
        if (serviceInstance.getStatus().equals("FAILED")) {
            serviceInstance.setStatus("PROPOGATED");
        }
        ServiceInstancePersistenceManager.merge(serviceInstance);     
        return serviceInstance.getStatus();
    }
    
    public String commitDeltas (String serviceInstanceUuid) {
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceInstanceUuid);
        if (serviceInstance == null) {
            throw new EJBException(HandleServiceCall.class.getName()+".commitDeltas cannot find serviceInstance with uuid=" + serviceInstanceUuid);
        }
        if (!serviceInstance.getStatus().equals("PROPOGATED")) {
            throw new EJBException(HandleServiceCall.class.getName()+".commitDeltas needs  status='PROPOGATED' by " + serviceInstance + ", the actual status=" + serviceInstance.getStatus());
        }
        Iterator<ServiceDelta> itSD = serviceInstance.getServiceDeltas().iterator();
        if (itSD.hasNext()) { // assume failed before proven success
            serviceInstance.setStatus("FAILED"); 
        }
        while (itSD.hasNext()) {
            ServiceDelta serviceDelta = itSD.next();
            if (serviceDelta.getSystemDelta() == null)
                continue;
            if (serviceDelta.getStatus().equals("PROPOGATED")) {
                SystemInstance systemInstance = SystemInstancePersistenceManager.findBySystemDelta(serviceDelta.getSystemDelta());
                systemInstance = SystemInstancePersistenceManager.findByReferenceUUID(systemInstance.getReferenceUUID());
                Future<String> asynResult = systemCallHandler.commitDelta(systemInstance);
                systemInstance.setCommitStatus(asynResult);
                systemInstance.setCommitFlag(true);
                serviceDelta.setStatus("COMMITTED");
                DeltaPersistenceManager.merge(serviceDelta);
            } else {
                throw new EJBException(HandleServiceCall.class.getName()+".commitDeltas (by " + serviceInstance + ") encounters "+serviceDelta +" in status=" + serviceDelta.getStatus());
            }
        }

        if (serviceInstance.getStatus().equals("FAILED")) {
            serviceInstance.setStatus("COMMITTED");
        }        
        ServiceInstancePersistenceManager.merge(serviceInstance);     
        return serviceInstance.getStatus();
    }
    
    public String checkStatus(String serviceInstanceUuid) {
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceInstanceUuid);
        if (serviceInstance == null) {
            throw new EJBException(HandleServiceCall.class.getName()+".checkStatus cannot find serviceInstance with uuid=" + serviceInstanceUuid);
        }
        if (!serviceInstance.getStatus().equals("COMMITTED")) {
            return serviceInstance.getStatus();
        }
        Iterator<ServiceDelta> itSD = serviceInstance.getServiceDeltas().iterator();
        while (itSD.hasNext()) {
            ServiceDelta serviceDelta = itSD.next();
            if (serviceDelta.getSystemDelta() == null) {
                continue;
            }
            if (!serviceDelta.getStatus().equals("COMMITTED")) {
                throw new EJBException(HandleServiceCall.class.getName() + ".checkStatus (by " + serviceInstance + ") encounters " + serviceDelta + " in status=" + serviceDelta.getStatus());
            }

            SystemInstance systemInstance = SystemInstancePersistenceManager.findBySystemDelta(serviceDelta.getSystemDelta());
            if (systemInstance == null) {
                throw new EJBException(HandleServiceCall.class.getName() + ".checkStatus cannot find systemInstance based on " + serviceDelta.getSystemDelta());
            }
            // get cached systemInstance
            systemInstance = SystemInstancePersistenceManager.findByReferenceUUID(systemInstance.getReferenceUUID());
            if (!systemInstance.getCommitFlag()) {
                throw new EJBException(HandleServiceCall.class.getName() + ".checkStatus encounters un-commited systemInstance based on " + serviceDelta.getSystemDelta());
            }
            Future<String> asyncStatus = systemInstance.getCommitStatus();
            serviceDelta.setStatus("FAILED");
            if (asyncStatus.isDone()) {
                try {
                    String commitStatus = asyncStatus.get();
                    if (commitStatus.equals("SUCCESS")) {
                        serviceDelta.setStatus("READY");
                    }
                } catch (Exception ex) {
                    serviceDelta.setStatus("FAILED");
                }
            } else {
                serviceDelta.setStatus("COMMITTED");
            }
            DeltaPersistenceManager.merge(serviceDelta);
        }
        // collect status:
        boolean failed = false;
        boolean ready = true;
        itSD = serviceInstance.getServiceDeltas().iterator();
        while (itSD.hasNext()) {
            ServiceDelta serviceDelta = itSD.next();
            if (serviceDelta.getStatus().equals("FAILED")) {
                failed = true;
                break;
            } else if (serviceDelta.getStatus().equals("COMMITTED"))  {
                ready = false;
            }
        }
        if (failed) {
            serviceInstance.setStatus("FALIED");
        } else if (ready) {
            serviceInstance.setStatus("READY");
        } else {
            serviceInstance.setStatus("COMMITTED");
        }
        ServiceInstancePersistenceManager.merge(serviceInstance);
        return serviceInstance.getStatus();
    }
}
