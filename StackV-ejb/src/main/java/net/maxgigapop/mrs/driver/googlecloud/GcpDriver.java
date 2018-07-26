/*
 * Copyright (c) 2013-2016 University of Maryland
 * Created by: Adam Smith 2017

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
package net.maxgigapop.mrs.driver.googlecloud;

//import net.maxgigapop.mrs.driver.aws.AwsModelBuilder;
//import net.maxgigapop.mrs.driver.aws.AwsPush;
//import net.maxgigapop.mrs.driver.aws.AwsDriver;

//import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.hpl.jena.ontology.OntModel;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import javax.ejb.AsyncResult;//returned by commit and pull
import javax.ejb.Asynchronous;//commit and pull are asynchronous
import javax.ejb.Stateless;//entire class is stateless
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import net.maxgigapop.mrs.bean.DriverInstance;
import net.maxgigapop.mrs.bean.DriverModel;
import net.maxgigapop.mrs.bean.DriverSystemDelta;
import net.maxgigapop.mrs.bean.VersionItem;
import net.maxgigapop.mrs.bean.persist.DeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.DriverInstancePersistenceManager;
import net.maxgigapop.mrs.bean.persist.DriverSystemDeltaPersistenceManager;
import net.maxgigapop.mrs.bean.persist.ModelPersistenceManager;
import net.maxgigapop.mrs.bean.persist.VersionItemPersistenceManager;
import net.maxgigapop.mrs.common.StackLogger;
import net.maxgigapop.mrs.driver.IHandleDriverSystemCall;
//import static net.maxgigapop.mrs.driver.openstack.OpenStackDriver.logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@Stateless
public class GcpDriver implements IHandleDriverSystemCall {
    
    public static final StackLogger logger = new StackLogger(GcpDriver.class.getName(), "GoogleCloudDriver");

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    @Override
    public void propagateDelta(DriverInstance driverInstance, DriverSystemDelta aDelta) {
        //System.out.println("DRIVER PROPAGATE START");
        //aDelta = (DriverSystemDelta) DeltaPersistenceManager.findById(aDelta.getId()); // refresh
        String method = "propagateDelta";
        if (aDelta.getSystemDelta() != null && aDelta.getSystemDelta().getServiceDelta() != null && aDelta.getSystemDelta().getServiceDelta().getServiceInstance() != null) {
            logger.refuuid(aDelta.getSystemDelta().getServiceDelta().getServiceInstance().getReferenceUUID());
        }
        logger.targetid(aDelta.getId());
        logger.start(method);
        
        OntModel model = driverInstance.getHeadVersionItem().getModelRef().getOntModel();
        OntModel modelAdd = aDelta.getModelAddition().getOntModel();
        OntModel modelReduc = aDelta.getModelReduction().getOntModel();
        
        GcpPush push = new GcpPush(driverInstance.getProperties());
        
        DriverInstancePersistenceManager.merge(driverInstance);
        //System.out.println("DRIVER PROPAGATE QUERY");
        JSONArray requests = push.propagate(model, modelAdd, modelReduc);
        aDelta.putCommand("requests", requests.toString()); // DO NOT merge/save as the parent transaction may double up
        logger.end(method);
        //System.out.println("DRIVER REQUESTS PROPAGATED: "+requests);
    }

    // Use ID to avoid passing entity bean between threads, which breaks persistence session
    @Asynchronous
    @Override
    public Future<String> commitDelta(DriverSystemDelta aDelta) {
        //System.out.println("DRIVER COMMIT START");
        logger.cleanup();
        String method = "commitDelta";
        aDelta = DriverSystemDeltaPersistenceManager.findById(aDelta.getId());
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
        String requests = aDelta.getCommand("requests");
        if (requests == null) {
            throw logger.error_throwing(method, "requests == null - something wrong with requests from propagate.");
        }
        if (requests.isEmpty()) {
            logger.warning(method, "requests is empty --  nothing has been propagated (no change needed).");
            return new AsyncResult<String>("SUCCESS");
        }
        
        GcpPush push = new GcpPush(driverInstance.getProperties());
        
        JSONParser parser = new JSONParser();
        
        try {
            JSONArray requestArray = (JSONArray) parser.parse(requests);
            //System.out.println("DRIVER COMMIT PUSH");
            push.commit(requestArray);
        } catch (ParseException | InterruptedException e) {
            throw logger.throwing(method, e);
        }
        logger.end(method);
        return new AsyncResult<>("SUCCESS");
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
            String jsonAuth = driverInstance.getProperty("gcp_access_json");
            String projectID = driverInstance.getProperty("projectID");
            String topologyURI = driverInstance.getProperty("topologyUri");
            
            OntModel ontModel = GcpModelBuilder.createOntology(driverInstance.getProperties());
            
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
        } catch (Exception e) {
            throw logger.throwing(method, driverInstance + " failed pull model due to "+e.getMessage(), e);
        }
        logger.trace_end(method);
        return new AsyncResult<>("SUCCESS");
    }
}
