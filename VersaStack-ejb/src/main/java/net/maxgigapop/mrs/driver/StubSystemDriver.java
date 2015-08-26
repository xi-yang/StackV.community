/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.driver;

import com.hp.hpl.jena.ontology.OntModel;
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
import net.maxgigapop.mrs.bean.persist.PersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionItemPersistenceManager;
import net.maxgigapop.mrs.common.ModelUtil;

/**
 *
 * @author xyang
 */

//use properties: stubModelTtl

@Stateless
public class StubSystemDriver implements IHandleDriverSystemCall {   
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {
        //driverInstance = DriverInstancePersistenceManager.findById(driverInstance.getId());
        aDelta = (DriverSystemDelta)DeltaPersistenceManager.findById(aDelta.getId());
        String ttlModel = driverInstance.getProperty("stubModelTtl");
        if (ttlModel == null) {
            throw new EJBException(String.format("%s has no property key=stubModelTtl", driverInstance));
        }
        try {
            OntModel ontModel = ModelUtil.unmarshalOntModel(ttlModel);
            DriverModel dm = new DriverModel();
            dm.setOntModel(ontModel);
            ontModel = dm.applyDelta(aDelta);
            ttlModel = ModelUtil.marshalOntModel(ontModel);
            driverInstance.putProperty("stubModelTtl", ttlModel);
            driverInstance.putProperty("stubModelTtl2", ttlModel);
            DriverInstancePersistenceManager.merge(driverInstance);
        } catch (Exception e) {
            throw new EJBException(String.format("propagateDelta for %s with %s raised exception(%s)", driverInstance, aDelta, e.getMessage()));
        }
    }
/*
    @Override
    @Asynchronous
    public Future<String> commitDelta(DriverSystemDelta aDelta) {
        DriverInstance driverInstance = aDelta.getDriverInstance();
        if (driverInstance == null) {
            throw new EJBException(String.format("commitDelta see null driverInance for %s", aDelta));
        }
        return new AsyncResult<String>("SUCCESS");
    }*/

    @Override
    @Asynchronous
    public Future<String> pullModel(Long driverInstanceId) {
        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(driverInstanceId);
        if (driverInstance == null) {
            throw new EJBException(String.format("pullModel cannot find driverInance(id=%d)", driverInstanceId));
        }
        String ttlModel = driverInstance.getProperty("stubModelTtl");
        if (ttlModel == null) {
            throw new EJBException(String.format("%s has no stubModelTtl property configured", driverInstance));
        }
        // create VI using stubModel in properties
        try {
            //@TODO: compare to previous version model
            OntModel ontModel = ModelUtil.unmarshalOntModel(ttlModel);
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
        } catch (Exception e) {
            throw new EJBException(String.format("pullModel on %s raised exception[%s]", driverInstance, e.getMessage()));
        }
        return new AsyncResult<String>("SUCCESS");
    }

}
