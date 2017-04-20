/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.driver.dtn;

import com.hp.hpl.jena.ontology.OntModel;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Future;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.bean.DriverModel;
import net.maxgigapop.mrs.bean.DriverSystemDelta;
import net.maxgigapop.mrs.bean.VersionItem;
import net.maxgigapop.mrs.bean.persist.DeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.ModelPersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionItemPersistenceManager;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.driver.IHandleDriverSystemCall;

/**
 *
 * @author xin
 */
@Stateless
public class DTNDriver implements IHandleDriverSystemCall {

    public static final StackLogger logger = new StackLogger(DTNDriver.class.getName(), "DTNDriver");
    
    String transferMap = "";
    String perfMap = "";    
    
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {
        String method = "propagateDelta";
        aDelta = (DriverSystemDelta) DeltaPersistenceManager.findById(aDelta.getId());
        if (aDelta.getSystemDelta() != null && aDelta.getSystemDelta().getServiceDelta() != null && aDelta.getSystemDelta().getServiceDelta().getServiceInstance() != null) {
            logger.refuuid(aDelta.getSystemDelta().getServiceDelta().getServiceInstance().getReferenceUUID());
        }
        logger.targetid(aDelta.getId());
        logger.start(method);
        driverInstance = DriverInstancePersistenceManager.findById(driverInstance.getId());
        String user_account = driverInstance.getProperty("user_account");
        String access_key = driverInstance.getProperty("access_key");
        String address = driverInstance.getProperty("address");
        String topologyURI = driverInstance.getProperty("topologyUri");
        String map = driverInstance.getProperty("mappingId");

        String model = driverInstance.getHeadVersionItem().getModelRef().getTtlModel();
        String modelAdd = aDelta.getModelAddition().getTtlModel();
        String modelReduc = aDelta.getModelReduction().getTtlModel();

        DTNPush push = new DTNPush(user_account, access_key, address, topologyURI, map);
        String requests = null;
        try {
            requests = push.pushPropagate(model, modelAdd, modelReduc);
        } catch (Exception ex) {
            throw logger.throwing(method, ex);
        }

        String requestId = driverInstance.getId().toString() + aDelta.getId().toString();
        driverInstance.putProperty(requestId, requests);
        DriverInstancePersistenceManager.merge(driverInstance);
        logger.end(method);
    }

    @Asynchronous
    @Override
    public Future<String> commitDelta(DriverSystemDelta aDelta) {
        logger.cleanup();
        String method = "commitDelta";
        if (aDelta.getSystemDelta() != null && aDelta.getSystemDelta().getServiceDelta() != null && aDelta.getSystemDelta().getServiceDelta().getServiceInstance() != null) {
            logger.refuuid(aDelta.getSystemDelta().getServiceDelta().getServiceInstance().getReferenceUUID());
        }
        logger.targetid(aDelta.getId());
        logger.start(method);
        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(aDelta.getDriverInstance().getId());
        if (driverInstance == null) {
            throw logger.error_throwing(method, "DriverInstance == null");
        }

        String user_account = driverInstance.getProperty("user_account");
        String access_key = driverInstance.getProperty("access_key");
        String address = driverInstance.getProperty("address");
        String topologyURI = driverInstance.getProperty("topologyUri");
        String map = driverInstance.getProperty("mappingId");
        
        String requestId = driverInstance.getId().toString() + aDelta.getId().toString();
        String requests = driverInstance.getProperty(requestId);

        DTNPush push = new DTNPush(user_account, access_key, address, topologyURI, map);
        try {
            push.pushCommit(requests);
        } catch (Exception ex) {
            throw logger.throwing(method, ex);
        }
        
        driverInstance.getProperties().remove(requestId);        
        //get transfer information
        this.transferMap = push.getTransferMap();
        driverInstance.putProperty("mappingId", this.transferMap);    
        DriverInstancePersistenceManager.merge(driverInstance);
        logger.end(method);
        return new AsyncResult<>("SUCCESS");
    }

    @Override
    @Asynchronous
    public Future<String> pullModel(Long driverInstanceId) {
        logger.cleanup();
        String method = "pullModel";
        logger.targetid(driverInstanceId.toString());
        logger.trace_start(method);
        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(driverInstanceId);
        if (driverInstance == null) {
            throw logger.error_throwing(method, String.format("cannot find driverInance(id=%d)", driverInstanceId));
        }

        try {
            String user_account = driverInstance.getProperty("user_account");
            String access_key = driverInstance.getProperty("access_key");
            String proxy_server = driverInstance.getProperty("proxy_server");
            String topologyURI = driverInstance.getProperty("topologyUri");
            String addresses = driverInstance.getProperty("addresses");
            String endpoint = driverInstance.getProperty("endpoint");
            String mappingId = driverInstance.getProperty("mappingId");
            String perf = driverInstance.getProperty("performance");
            
            DTNModelBuilder pull = new DTNModelBuilder(user_account, mappingId, perf);
            OntModel ontModel = pull.createOntology(user_account, access_key, proxy_server, addresses, topologyURI, endpoint);
            this.transferMap=pull.getTransferMap();
            this.perfMap = pull.getPerformanceMap();
          
            if (driverInstance.getHeadVersionItem() == null || !driverInstance.getHeadVersionItem().getModelRef().getOntModel().isIsomorphicWith(ontModel)) {
                DriverModel dm = new DriverModel();
                dm.setCommitted(true);
                dm.setOntModel(ontModel);
                ModelPersistenceManager.save(dm);

                VersionItem vi = new VersionItem();
                vi.setModelRef(dm);
                vi.setReferenceUUID(UUID.randomUUID().toString());
                vi.setDriverInstance(driverInstance);
                VersionItemPersistenceManager.save(vi);
                driverInstance.setHeadVersionItem(vi);                
            }
            driverInstance.putProperty("mappingId", this.transferMap);
            driverInstance.putProperty("performance", this.perfMap);
            DriverInstancePersistenceManager.merge(driverInstance);            
        } catch (Exception ex) {
            throw logger.throwing(method, driverInstance + " failed pull model", ex);
        }
        logger.trace_end(method);
        return new AsyncResult<>("SUCCESS");
    }

}
