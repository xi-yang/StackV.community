/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.maxgigapop.mrs.driver;

import com.hp.hpl.jena.ontology.OntModel;
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
import net.maxgigapop.mrs.bean.persist.ModelPersistenceManager;
import net.maxgigapop.mrs.bean.persist.PersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionItemPersistenceManager;
import net.maxgigapop.mrs.common.ModelUtil;
import net.maxgigapop.mrs.system.IHandleDriverSystemCall;

/**
 *
 * @author xyang
 */

//use properties: stubModelTtl

@Stateless
public class StubSystemDriver implements IHandleDriverSystemCall {
    private static Long stubModelVersionId = 1L;
    
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {
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
        } catch (Exception e) {
            throw new EJBException(String.format("propagateDelta for %s with %s raised exception(%s)", driverInstance, aDelta, e.getMessage()));
        }
    }

    @Override
    @Asynchronous
    public Future<String> commitDelta(DriverInstance driverInstance, VersionItem targetVI) {
        String status = "SUCCESS";
        return new AsyncResult<String>(status);
    }

    @Override
    @Asynchronous
    public Future<String> pullModel(DriverInstance driverInstance) {
        String status = "INIT";
        String ttlModel = driverInstance.getProperty("stubModelTtl");
        if (ttlModel == null) {
            status = "FAILED";
            return new AsyncResult<String>(status);
        }
        // create VI using stubModel in properties
        try {
            OntModel ontModel = ModelUtil.unmarshalOntModel(ttlModel);
            DriverModel dm = new DriverModel();
            dm.setCommitted(true);
            dm.setOntModel(ontModel);
            ModelPersistenceManager.save(dm);
            VersionItem vi = new VersionItem();
            vi.setModelRef(dm);
            vi.setReferenceId(stubModelVersionId++);
            VersionItemPersistenceManager.save(vi);
            driverInstance.setHeadVersionItem(vi);
        } catch (Exception e) {
            status = "FAILED";
            return new AsyncResult<String>(status);
        }
        status = "SUCCESS";
        return new AsyncResult<String>(status);
    }
}
