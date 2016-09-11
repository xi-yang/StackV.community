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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.EJBException;
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
import net.maxgigapop.mrs.driver.IHandleDriverSystemCall;

/**
 *
 * @author xin
 */
@Stateless
public class DTNDriver implements IHandleDriverSystemCall {

    Logger logger = Logger.getLogger(DTNDriver.class.getName());

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {
        aDelta = (DriverSystemDelta) DeltaPersistenceManager.findById(aDelta.getId());

        String user_account = driverInstance.getProperty("user_account");
        String access_key = driverInstance.getProperty("access_key");
        String address = driverInstance.getProperty("address");
        String topologyURI = driverInstance.getProperty("topologyUri");

        String model = driverInstance.getHeadVersionItem().getModelRef().getTtlModel();
        String modelAdd = aDelta.getModelAddition().getTtlModel();
        String modelReduc = aDelta.getModelReduction().getTtlModel();

        DTNPush push = new DTNPush(user_account, access_key, address, topologyURI);
        String requests = null;
        try {
            requests = push.pushPropagate(model, modelAdd, modelReduc);
        } catch (Exception ex) {
            Logger.getLogger(DTNDriver.class.getName()).log(Level.SEVERE, ex.getMessage());
        }

        String requestId = driverInstance.getId().toString() + aDelta.getId().toString();
        driverInstance.putProperty(requestId, requests);
        DriverInstancePersistenceManager.merge(driverInstance);
        logger.log(Level.INFO, "DTN driver delta models succesfully propagated");
    }

    @Asynchronous
    @Override
    public Future<String> commitDelta(DriverSystemDelta aDelta) {
        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(aDelta.getDriverInstance().getId());
        if (driverInstance == null) {
            throw new EJBException(String.format("commitDelta see null driverInance for %s", aDelta));
        }

        String user_account = driverInstance.getProperty("user_account");
        String access_key = driverInstance.getProperty("access_key");
        String address = driverInstance.getProperty("address");
        String topologyURI = driverInstance.getProperty("topologyUri");
        String requestId = driverInstance.getId().toString() + aDelta.getId().toString();
        String requests = driverInstance.getProperty(requestId);

        DTNPush push = new DTNPush(user_account, access_key, address, topologyURI);
        push.pushCommit(requests);

        driverInstance.getProperties().remove(requestId);
        DriverInstancePersistenceManager.merge(driverInstance);

        logger.log(Level.INFO, "DTN driver delta models succesfully commited");
        return new AsyncResult<>("SUCCESS");
    }

    @Override
    @Asynchronous
    public Future<String> pullModel(Long driverInstanceId) {
        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(driverInstanceId);
        if (driverInstance == null) {
            throw new EJBException(String.format("pullModel cannot find driverInstance(id=%d)", driverInstanceId));
        }

        try {
            String user_account = driverInstance.getProperty("user_account");
            String access_key = driverInstance.getProperty("access_key");
            String topologyURI = driverInstance.getProperty("topologyUri");
            String addresses = driverInstance.getProperty("addresses");
            String endpoint = driverInstance.getProperty("endpoint");
            
            OntModel ontModel = DTNModelBuilder.createOntology(user_account, access_key, addresses, topologyURI, endpoint);

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
        } catch (Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }

//        logger.log(Level.INFO, "DTN driver ontology model succesfully pulled");
        return new AsyncResult<>("SUCCESS");
    }

}
