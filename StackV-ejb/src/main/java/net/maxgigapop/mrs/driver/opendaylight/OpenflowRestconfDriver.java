/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Xi Yang 2016

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


package net.maxgigapop.mrs.driver.opendaylight;

import com.hp.hpl.jena.ontology.OntModel;
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


@Stateless
public class OpenflowRestconfDriver implements IHandleDriverSystemCall{
    
    public static final StackLogger logger = new StackLogger(OpenflowRestconfDriver.class.getName(), "OpenflowRestconfDriver");

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)

    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {
        String method = "propagateDelta";
        if (aDelta.getSystemDelta() != null && aDelta.getSystemDelta().getServiceDelta() != null && aDelta.getSystemDelta().getServiceDelta().getServiceInstance() != null) {
            logger.refuuid(aDelta.getSystemDelta().getServiceDelta().getServiceInstance().getReferenceUUID());
        }
        logger.targetid(aDelta.getReferenceUUID());
        logger.start(method);
        driverInstance = DriverInstancePersistenceManager.findById(driverInstance.getId());
        if (driverInstance == null) {
            throw logger.error_throwing(method, "DriverInstance == null");
        }
        OntModel modelRef = driverInstance.getHeadVersionItem().getModelRef().getOntModel();
        OntModel modelAdd = aDelta.getModelAddition().getOntModel();
        OntModel modelReduc = aDelta.getModelReduction().getOntModel();
        OpenflowPush push = new OpenflowPush();
        String requests = null;
        requests = push.propagate(modelRef, modelAdd, modelReduc);
        aDelta.putCommand("requests", requests); // DO NOT merge/save as the parent transaction may double up
        logger.end(method);
    }

    @Override
    @Asynchronous
    public Future<String> commitDelta(DriverSystemDelta aDelta) {
        logger.cleanup();
        String method = "commitDelta";
        if (aDelta.getSystemDelta() != null && aDelta.getSystemDelta().getServiceDelta() != null && aDelta.getSystemDelta().getServiceDelta().getServiceInstance() != null) {
            logger.refuuid(aDelta.getSystemDelta().getServiceDelta().getServiceInstance().getReferenceUUID());
        }
        logger.targetid(aDelta.getReferenceUUID());
        logger.start(method);
        DriverInstance driverInstance = aDelta.getDriverInstance();
        if (driverInstance == null) {
            throw logger.error_throwing(method, "DriverInstance == null");
        }
        String loginUser = driverInstance.getProperty("loginUser");
        String loginPass = driverInstance.getProperty("loginPass");
        String topologyURI = driverInstance.getProperty("topologyUri");
        
        //Searches for subsystemBaseUrl in Driver Instance
        String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");
        if (subsystemBaseUrl == null || loginUser == null || loginPass ==null || topologyURI == null) {
            throw logger.error_throwing(method, String.format("%s misses one of the property keys {subsystemBaseUrlm, loginUser, loginPass, topologyUri}", driverInstance));
        }
        String requests = aDelta.getCommand("requests");
        if (requests == null) {
            throw logger.error_throwing(method, "requests == null - something wrong with requests from propagate.");
        }
        if (requests.isEmpty()) {
            logger.warning(method, "requests is empty --  nothing has been propagated (no change needed).");
            return new AsyncResult<String>("SUCCESS");
        }        
        OpenflowPush push = new OpenflowPush();
        try {
            push.commit(loginUser, loginPass, requests, subsystemBaseUrl);
        } catch (Exception ex) {
            throw logger.throwing(method, ex);
        }
        logger.end(method);
        return new AsyncResult<String>("SUCCESS");
    }
    
    @Override
    @Asynchronous
    @SuppressWarnings("empty-statement")
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
            String loginUser = driverInstance.getProperty("loginUser");
            String loginPass = driverInstance.getProperty("loginPass");
            String topologyURI = driverInstance.getProperty("topologyUri");
            String subsystemBaseUrl = driverInstance.getProperty("subsystemBaseUrl");

            if (subsystemBaseUrl == null) {
                throw logger.error_throwing(method, String.format("%s has no property key=subsystemBaseUrl", driverInstance));
            }

            String modelExtTtl = driverInstance.getProperty("modelExt");
            OntModel modelExt = null;
            if (modelExtTtl != null && !modelExtTtl.isEmpty()) {
                modelExt = ModelUtil.unmarshalOntModel(modelExtTtl);
            }
            // Creates an Ontology Model from ODL controller RESTConf
            OntModel ontModel = OpenflowModelBuilder.createOntology(topologyURI, subsystemBaseUrl, loginUser, loginPass, modelExt);
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
            throw logger.throwing(method, driverInstance + " failed pull model", ex);
        }
        logger.trace_end(method);
        return new AsyncResult<>("SUCCESS");
    }

}

