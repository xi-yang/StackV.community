/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AsyncResult;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import net.maxgigapop.mrs.bean.DeltaBase;
import net.maxgigapop.mrs.bean.ServiceDelta;
import net.maxgigapop.mrs.bean.ServiceInstance;
import net.maxgigapop.mrs.bean.SystemDelta;
import net.maxgigapop.mrs.bean.persist.ServiceInstancePersistenceManager;
import net.maxgigapop.mrs.service.orchestrate.WorkerBase;

/**
 *
 * @author xyang
 */
@Stateless
@LocalBean
public class HandleServiceCall {  
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
        WorkerBase worker = null;
        try {
            worker = (WorkerBase)this.getClass().getClassLoader().loadClass(workerClassPath).newInstance();
        } catch (Exception ex) {
            Logger.getLogger(HandleServiceCall.class.getName()).log(Level.SEVERE, null, ex);
            throw new EJBException(ex);
        } 
        ServiceInstance serviceInstance = ServiceInstancePersistenceManager.findByReferenceUUID(serviceInstanceUuid);
        if (serviceInstance.getServiceDeltas() == null) {
            serviceInstance.setServiceDeltas(new ArrayList<ServiceDelta>());
            serviceInstance.getServiceDeltas().add(spaDelta);
        }
        //@TODO: save spaDelta and serviceInstance
        worker.setAnnoatedModel(spaDelta);
        worker.run();
        SystemDelta resultDelta = worker.getResultModelDelta();
        spaDelta.setSystemDelta(resultDelta);
        //@TODO: save spaDelta
        return resultDelta;
    }
    
    //@TODO:
    //pulic Future<String> pushDelta
    //? move serviceInstance.getServiceDeltas().add(spaDelta) here ? --> No
    //? handling multiple deltas: split into propagate + commit = transactional propagate -> parallel commits
    //? a wrapper method to hide intermediate SystemDelta etc. from user + tracking status

}
