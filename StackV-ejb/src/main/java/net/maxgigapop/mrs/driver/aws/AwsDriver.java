/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Miguel Uzcategui 2015
 * Modified by: Xi Yang 2015-2016

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
package net.maxgigapop.mrs.driver.aws;

import net.maxgigapop.mrs.driver.aws.AwsModelBuilder;
import net.maxgigapop.mrs.driver.aws.AwsPush;
import net.maxgigapop.mrs.driver.aws.AwsDriver;
import com.amazonaws.regions.Regions;
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
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.driver.IHandleDriverSystemCall;

/**
 *
 * @author muzcategui
 */
//TODO make the stirng returned by the push.progate function to be in JSON format
// and adapt it to the driver
//TODO make request not to be in the database as a driver property, as they do not
//truly get deleted.
@Stateless
public class AwsDriver implements IHandleDriverSystemCall {

    public static final StackLogger logger = new StackLogger(AwsDriver.class.getName(), "AwsDriver");

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {
        aDelta = (DriverSystemDelta) DeltaPersistenceManager.findById(aDelta.getId()); // refresh
        String method = "propagateDelta";
        if (aDelta.getSystemDelta() != null && aDelta.getSystemDelta().getServiceDelta() != null && aDelta.getSystemDelta().getServiceDelta().getServiceInstance() != null) {
            logger.refuuid(aDelta.getSystemDelta().getServiceDelta().getServiceInstance().getReferenceUUID());
        }
        logger.targetid(aDelta.getId());
        logger.start(method);
        String access_key_id = driverInstance.getProperty("aws_access_key_id");
        String secret_access_key = driverInstance.getProperty("aws_secret_access_key");
        String r = driverInstance.getProperty("region");
        Regions region = Regions.fromName(r);
        String topologyURI = driverInstance.getProperty("topologyUri");
        String defaultImage = driverInstance.getProperty("defaultImage");
        String defaultInstanceType = driverInstance.getProperty("defaultInstanceType");
        String defaultKeyPair = driverInstance.getProperty("defaultKeyPair");
        String defaultSecGroup = driverInstance.getProperty("defaultSecGroup");

        String model = driverInstance.getHeadVersionItem().getModelRef().getTtlModel();
        String modelAdd = aDelta.getModelAddition().getTtlModel();
        String modelReduc = aDelta.getModelReduction().getTtlModel();

        AwsPush push = new AwsPush(access_key_id, secret_access_key, region, topologyURI, defaultImage, defaultInstanceType, defaultKeyPair, defaultSecGroup);
        String requests = null;
        requests = push.pushPropagate(model, modelAdd, modelReduc);
        String requestId = driverInstance.getId().toString() + aDelta.getId().toString();
        driverInstance.putProperty(requestId, requests);
        DriverInstancePersistenceManager.merge(driverInstance);
        logger.end(method);
    }

    // Use ID to avoid passing entity bean between threads, which breaks persistence session
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
        DriverInstance driverInstance = aDelta.getDriverInstance();
        if (driverInstance == null) {
            throw logger.error_throwing(method, "DriverInstance == null");
        }
        driverInstance = DriverInstancePersistenceManager.findById(driverInstance.getId());
        String requestId = driverInstance.getId().toString() + aDelta.getId().toString();
        String requests = driverInstance.getProperty(requestId);
        if (requests == null) {
            throw logger.error_throwing(method, "requests == null - trying to commit after propagate failed, requestId="+requestId);
        }
        if (requests.isEmpty()) {
            driverInstance.getProperties().remove(requestId);
            DriverInstancePersistenceManager.merge(driverInstance);
            throw logger.error_throwing(method, "requests.isEmpty - no change to commit, requestId="+requestId);
        }        
        String access_key_id = driverInstance.getProperty("aws_access_key_id");
        String secret_access_key = driverInstance.getProperty("aws_secret_access_key");
        String r = driverInstance.getProperty("region");
        Regions region = Regions.fromName(r);
        String topologyURI = driverInstance.getProperty("topologyUri");
        String defaultImage = driverInstance.getProperty("defaultImage");
        String defaultInstanceType = driverInstance.getProperty("defaultInstanceType");
        String defaultKeyPair = driverInstance.getProperty("defaultKeyPair");
        String defaultSecGroup = driverInstance.getProperty("defaultSecGroup");
        driverInstance.getProperties().remove(requestId);
        DriverInstancePersistenceManager.merge(driverInstance);
        AwsPush push = new AwsPush(access_key_id, secret_access_key, region, topologyURI, defaultImage, defaultInstanceType, defaultKeyPair, defaultSecGroup);
        try {
            push.pushCommit(requests);
        } catch (com.amazonaws.AmazonServiceException ex) {
            throw logger.throwing(method, ex);
        }
        logger.end(method);
        return new AsyncResult<String>("SUCCESS");
    }

    // Use ID to avoid passing entity bean between threads, which breaks persistence session
    @Asynchronous
    @Override
    public Future<String> pullModel(Long driverInstanceId) {
        logger.cleanup();
        String method = "pullModel";
        logger.targetid(driverInstanceId.toString());
        logger.trace_start(method);
        DriverInstance driverInstance = DriverInstancePersistenceManager.findById(driverInstanceId);
        if (driverInstance == null) {
            throw logger.error_throwing(method, "DriverInstance == null");
        }

        try {
            String access_key_id = driverInstance.getProperty("aws_access_key_id");
            String secret_access_key = driverInstance.getProperty("aws_secret_access_key");
            String r = driverInstance.getProperty("region");
            String topologyURI = driverInstance.getProperty("topologyUri");
            Regions region = Regions.fromName(r);
            OntModel ontModel = AwsModelBuilder.createOntology(access_key_id, secret_access_key, region, topologyURI);

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
            

        } catch (IOException e) {
            throw logger.throwing(method, driverInstance + " failed AwsModelBuilder.createOntology", e);
        } catch (Exception ex) {
            throw logger.throwing(method, driverInstance + " failed pull model", ex);
        }
        logger.trace_end(method);
        return new AsyncResult<>("SUCCESS");
    }

}
